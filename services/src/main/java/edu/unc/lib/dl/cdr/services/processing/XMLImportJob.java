/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */package edu.unc.lib.dl.cdr.services.processing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.samskivert.mustache.Template;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.update.UpdateException;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.validation.MetadataValidationException;

public class XMLImportJob implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(XMLImportJob.class);

    private AccessControlService aclService;
    private AgentPrincipals agent;
    private UpdateDescriptionService updateService;
    private RepositoryObjectLoader repoObjLoader;
    private JavaMailSender mailSender;
    private Template completeTemplate;
    private Template failedTemplate;
    private String fromAddress;

    private enum DocumentState {
        ROOT, IN_BULK, IN_OBJECT, IN_CONTENT;
    }

    private final static String BULK_MD_TAG = "bulkMetadata";
    private final static String OBJECT_TAG = "object";
    private final static String UPDATE_TAG = "update";
    private final static String MODS_TYPE = "MODS";
    private final static QName pidAttribute = new QName("pid");
    private final static QName lastModifiedAttribute = new QName("lastModified");
    private final static QName typeAttribute = new QName("type");

    private PID currentPid;
    private int updateCount = 0;
    private int objectCount = 0;
    private final File importFile;
    private List<String> updated;
    private Map<String, String> failed;

    private XMLEventReader xmlReader;
    private final XMLOutputFactory xmlOutput = XMLOutputFactory.newInstance();
    private DocumentState state = DocumentState.ROOT;

    private final int BUFFER_SIZE = 2048;
    private final Charset utf8 = Charset.forName("UTF-8");
    private final String separator = System.getProperty("line.separator");
    private final byte[] separatorBytes = System.getProperty("line.separator").getBytes();
    private final byte[] importHeaderBytes = ("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + separator
            + "<bulkMetadata>" + separator).getBytes(utf8);

    private final String username;
    private final String userEmail;

    public XMLImportJob(String username, String userEmail, AgentPrincipals agent,
            File importFile) {
        this.username = username;
        this.userEmail = userEmail;
        this.agent = agent;
        this.importFile = importFile;

        this.updated = new ArrayList<>();
        this.failed = new HashMap<>();
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();

        try {
            initializeXMLReader();
        } catch (UpdateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            getNextUpdate();
            log.info("Finished metadata import for {} objects in {}ms for user {}",
                    new Object[] {objectCount, System.currentTimeMillis() - startTime, username});
        } catch (JDOMException | XMLStreamException | UpdateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
        } finally {
            cleanup(importFile);
        }

        sendCompletedEmail(userEmail, updated, failed);
    }

    public AccessControlService getAclService() {
        return aclService;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public RepositoryObjectLoader getRepoObjLoader() {
        return repoObjLoader;
    }

    public void setRepoObjLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    private void initializeXMLReader() throws UpdateException {
        XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
        try {
            xmlReader = xmlFactory.createXMLEventReader(new FileInputStream(importFile));
        } catch (FileNotFoundException | XMLStreamException e) {
            throw new UpdateException("Failed to read metadata update package for " + username, e);
        }
    }

    private void getNextUpdate()
            throws UpdateException, JDOMException, XMLStreamException {
        seekNextUpdate(null, null);
    }

    private void seekNextUpdate(PID resumePid, String resumeDs)
            throws UpdateException, JDOMException, XMLStreamException {
        QName contentOpening = null;
        long countOpenings = 0;
        XMLEventWriter xmlWriter = null;
        StringWriter contentWriter = null;

        String currentDs = null;
        String lastModified = null;

        boolean resumeMode = (resumePid != null);
        boolean foundResumptionPoint = false;

        try {
            while (xmlReader.hasNext()) {
                XMLEvent e = xmlReader.nextEvent();

                switch (state) {
                    case ROOT:
                        if (e.isStartElement()) {
                            StartElement element = e.asStartElement();
                            // Make sure that this document begins with bulk md tag
                            if (element.getName().getLocalPart().equals(BULK_MD_TAG)) {
                                state = DocumentState.IN_BULK;
                            }
                        }
                        break;
                    case IN_BULK:
                        if (e.isStartElement()) {
                            StartElement element = e.asStartElement();
                            // Found an opening object tag, capture its PID
                            if (element.getName().getLocalPart().equals(OBJECT_TAG)) {
                                Attribute pid = element.getAttributeByName(pidAttribute);

                                if (pid != null) {
                                    currentPid = PIDs.get(pid.getValue());
                                    objectCount++;
                                }
                                state = DocumentState.IN_OBJECT;
                            }
                        }
                        break;
                    case IN_OBJECT:
                        if (e.isStartElement()) {
                            StartElement element = e.asStartElement();
                            // Found start of update, extract the datastream
                            if (element.getName().getLocalPart().equals(UPDATE_TAG)) {
                                // Get last modified date if available
                                Attribute lastModifiedAttr = element.getAttributeByName(lastModifiedAttribute);
                                lastModified = lastModifiedAttr == null ? null : lastModifiedAttr.getValue();

                                Attribute typeAttr = element.getAttributeByName(typeAttribute);
                                if (typeAttr == null) {
                                    throw new UpdateException("Invalid import data, missing type attribute"
                                            + " on update of " + currentPid);
                                }
                                if (MODS_TYPE.equals(typeAttr.getValue())) {
                                    currentDs = Datastream.MD_DESCRIPTIVE.toString();
                                } else {
                                    throw new UpdateException("Invalid import data, unsupport type in update tag "
                                            + currentPid);
                                }

                                foundResumptionPoint = resumeMode
                                        && currentPid.equals(resumePid) && currentDs.equals(resumeDs);

                                state = DocumentState.IN_CONTENT;
                                if (!resumeMode) {
                                    contentWriter = new StringWriter();
                                    xmlWriter = xmlOutput.createXMLEventWriter(contentWriter);
                                }
                            }
                        } else if (e.isEndElement()) {
                            // Closing object tag
                            state = DocumentState.IN_BULK;
                        }
                        break;
                    case IN_CONTENT:
                        // Record the name of the content opening tag so we can tell when it closes
                        if (e.isStartElement()) {
                            if (contentOpening == null) {
                                contentOpening = e.asStartElement().getName();
                                countOpenings = 1;
                            } else if (contentOpening.equals(e.asStartElement().getName())) {
                                // Count the number of openings just in case there are nested records
                                countOpenings++;
                            }
                        }
                        // Subtract from count of opening tags for root element
                        if (e.isEndElement() && contentOpening.equals(e.asEndElement().getName())) {
                            countOpenings--;
                        }

                        // Finished with opening tags and the update tag is ending, done with content.
                        if (countOpenings == 0 && e.isEndElement() && UPDATE_TAG.equals(
                                e.asEndElement().getName().getLocalPart())) {
                            state = DocumentState.IN_OBJECT;
                            // Increment the number of updates retrieved
                            updateCount++;
                            if (!resumeMode) {
                                xmlWriter.close();
                                xmlWriter = null;
                                InputStream modsStream = new ByteArrayInputStream(contentWriter.toString().getBytes());
                                try {
                                    updateService.updateDescription(agent, currentPid, modsStream);
                                } catch (AccessRestrictionException ex) {
                                    failed.put(currentPid.getRepositoryPath(),
                                            "User doesn't have permission to update this object: " + ex.getMessage());
                                } catch (MetadataValidationException ex) {
                                    failed.put(currentPid.getRepositoryPath(),
                                            "MODS is not valid: " + ex.getMessage());
                                } catch (IOException ex) {
                                    failed.put(currentPid.getRepositoryPath(),
                                            "Error reading or converting MODS stream: " + ex.getMessage());
                                } catch (FedoraException ex) {
                                    failed.put(currentPid.getRepositoryPath(),
                                            "Error retrieving object from Fedora: " + ex.getMessage());
                                }
                            } else if (foundResumptionPoint) {
                                return;
                            }
                        } else {
                            if (!resumeMode) {
                                // Store all of the content from the incoming document
                                xmlWriter.add(e);
                            }
                        }

                        break;
                }
            }
        } catch (Exception e) {
            throw new UpdateException("Could not parse content for " + currentPid, e);
        } finally {
            if (xmlWriter != null) {
                xmlWriter.close();
            }
            reset();
        }

    }

    private void close() {
        try {
            xmlReader.close();
        } catch (XMLStreamException e) {
            log.error("Failed to close XML Reader during CDR metadata update", e);
        }
    }

    private void reset() throws UpdateException {
        close();
        initializeXMLReader();

        updateCount = 0;
        objectCount = 0;
    }

    private void cleanup(File mdImportFile) {
        mdImportFile.delete();
    }

    private void sendCompletedEmail(String toAddress, List<String> updated, Map<String, String> failed) {
        MimeMessage mimeMsg = mailSender.createMimeMessage();
        try {
            MimeMessageHelper msg = new MimeMessageHelper(mimeMsg, MimeMessageHelper.MULTIPART_MODE_MIXED);

            msg.setFrom(fromAddress);
            log.error("Sending email to '{}'", toAddress);
            if (toAddress == null || toAddress.trim().length() == 0) {
                // No email provided, send to admins instead
                msg.addTo(fromAddress);
            } else {
                msg.addTo(toAddress);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("fileName", importFile.getAbsolutePath());

            data.put("updated", updated);

            int updatedCount = updated.size();
            data.put("updatedCount", updatedCount);

            if (failed.size() > 0) {
                data.put("failedCount", failed.size());
                data.put("failed", failed.entrySet());
            }

            if (failed.size() > 0) {
                data.put("issues", true);
                msg.setSubject("CDR Metadata update completed with issues:" + importFile.getAbsolutePath());
                msg.addTo(fromAddress);
            } else {
                msg.setSubject("CDR Metadata update completed:" + importFile.getAbsolutePath());
            }

            String html = completeTemplate.execute(data);
            msg.setText(html, true);

            mailSender.send(mimeMsg);
        } catch (MessagingException e) {
            log.error("Failed to send email to " + toAddress
                    + " for update " + importFile.getAbsolutePath(), e);
        }
    }

    private void sendValidationFailureEmail(String toAddress, Map<String, String> problems) {
        MimeMessage mimeMsg = mailSender.createMimeMessage();
        try {
            MimeMessageHelper msg = new MimeMessageHelper(mimeMsg, MimeMessageHelper.MULTIPART_MODE_MIXED);

            msg.setFrom(fromAddress);
            if (toAddress == null || toAddress.trim().length() == 0) {
                // No email provided, send to admins instead
                msg.addTo(fromAddress);
            } else {
                msg.addTo(toAddress);
            }

            msg.setSubject("CDR Metadata update failed");

            Map<String, Object> data = new HashMap<>();
            data.put("fileName", importFile.getAbsolutePath());
            data.put("problems", problems.entrySet());
            data.put("problemCount", problems.size());

            String html = failedTemplate.execute(data);
            msg.setText(html, true);

            mailSender.send(mimeMsg);
        } catch (MessagingException e) {
            log.error("Failed to send email to " + toAddress
                    + " for update " + importFile.getAbsolutePath(), e);
        }
    }

}
