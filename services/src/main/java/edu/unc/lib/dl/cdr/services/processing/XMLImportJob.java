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
 */
package edu.unc.lib.dl.cdr.services.processing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.samskivert.mustache.Template;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.update.UpdateException;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.validation.MetadataValidationException;
import io.dropwizard.metrics5.Timer;

/**
 * A job for stepping through the bulk metadata update doc and making updates to individual objects
 *
 * @author harring
 *
 */
public class XMLImportJob implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(XMLImportJob.class);

    private UpdateDescriptionService updateService;
    private JavaMailSender mailSender;
    private Template completeTemplate;
    private Template failedTemplate;
    private String fromAddress;
    private MimeMessage mimeMsg;
    private MimeMessageHelper msg;

    private enum DocumentState {
        ROOT, IN_BULK, IN_OBJECT, IN_CONTENT;
    }

    private final static String BULK_MD_TAG = "bulkMetadata";
    private final static String OBJECT_TAG = "object";
    private final static String UPDATE_TAG = "update";
    private final static String MODS_TYPE = "MODS";
    private final static QName pidAttribute = new QName("pid");
    private final static QName typeAttribute = new QName("type");

    private PID currentPid;
    private int objectCount = 0;

    private XMLEventReader xmlReader;
    private final XMLOutputFactory xmlOutput = XMLOutputFactory.newInstance();
    private DocumentState state = DocumentState.ROOT;

    private final String userEmail;
    private AgentPrincipals agent;
    private final File importFile;

    private String username;

    private List<String> updated;
    private Map<String, String> failed;

    private static final Timer timer = TimerFactory.createTimerForClass(XMLImportJob.class);

    public XMLImportJob(String userEmail, AgentPrincipals agent, File importFile) {
        this.userEmail = userEmail;
        this.agent = agent;
        this.importFile = importFile;

        this.username = agent.getUsername();

        this.updated = new ArrayList<>();
        this.failed = new HashMap<>();
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();

        try {
            mimeMsg = mailSender.createMimeMessage();
            msg = new MimeMessageHelper(mimeMsg, MimeMessageHelper.MULTIPART_MODE_MIXED);
        } catch (MessagingException e) {
            log.error("Failed to send email to " + userEmail
                    + " for update " + importFile.getAbsolutePath(), e);
        }

        try (Timer.Context context = timer.time()) {
            initializeXMLReader();
            processUpdates();
            log.info("Finished metadata import for {} objects in {}ms for user {}",
                    new Object[] {objectCount, System.currentTimeMillis() - startTime, username});
            sendCompletedEmail(updated, failed);
        } catch (XMLStreamException e) {
            log.info("Errors reading XML during update " + username, e);
            failed.put(importFile.getAbsolutePath(), "The import file contains XML errors");
            sendValidationFailureEmail(failed);
        } catch (UpdateException e) {
            log.error("Submitted document is not a bulk-metadata-update doc");
            failed.put(importFile.getAbsolutePath(), "File is not a bulk-metadata-update doc");
            sendValidationFailureEmail(failed);
        } catch (FileNotFoundException e) {
            log.error("The import file could not be found on the server");
            failed.put(importFile.getAbsolutePath(), "Import file could not be found on the server");
            sendValidationFailureEmail(failed);
        } finally {
            close();
            cleanup();
        }

    }

    public UpdateDescriptionService getUpdateService() {
        return updateService;
    }

    public void setUpdateService(UpdateDescriptionService updateService) {
        this.updateService = updateService;
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }

    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public Template getCompleteTemplate() {
        return completeTemplate;
    }

    public void setCompleteTemplate(Template completeTemplate) {
        this.completeTemplate = completeTemplate;
    }

    public Template getFailedTemplate() {
        return failedTemplate;
    }

    public void setFailedTemplate(Template failedTemplate) {
        this.failedTemplate = failedTemplate;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public MimeMessage getMimeMessage() {
        return mimeMsg;
    }

    public void setMimeMessage(MimeMessage mimeMsg) {
        this.mimeMsg = mimeMsg;
    }

    public MimeMessageHelper getMessageHelper() {
        return msg;
    }

    public void setMessageHelper(MimeMessageHelper msg) {
        this.msg = msg;
    }

    public List<String> getUpdated() {
        return updated;
    }

    public Map<String, String> getFailed() {
        return failed;
    }

    private void initializeXMLReader() throws FileNotFoundException, XMLStreamException {
        XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
        xmlReader = xmlFactory.createXMLEventReader(new FileInputStream(importFile));

    }

    private void processUpdates() throws XMLStreamException, UpdateException {
        processUpdates(null, null);
    }

    private void processUpdates(PID resumePid, String resumeDs) throws XMLStreamException, UpdateException {
        QName contentOpening = null;
        long countOpenings = 0;
        XMLEventWriter xmlWriter = null;
        StringWriter contentWriter = null;

        String currentDs = null;

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
                            } else {
                                throw new UpdateException("Submitted document is not a bulk-metadata-update doc");
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
                                    state = DocumentState.IN_OBJECT;
                                } else {
                                    failed.put(importFile.getAbsolutePath(), "PID attribute was missing");
                                }
                            }
                        }
                        break;
                    case IN_OBJECT:
                        if (e.isStartElement()) {
                            StartElement element = e.asStartElement();
                            // Found start of update, extract the datastream
                            if (element.getName().getLocalPart().equals(UPDATE_TAG)) {

                                Attribute typeAttr = element.getAttributeByName(typeAttribute);
                                if (typeAttr == null) {
                                    failed.put(currentPid.getRepositoryPath(),
                                            "Invalid import data, missing type attribute on update");
                                }
                                if (MODS_TYPE.equals(typeAttr.getValue())) {
                                    currentDs = Datastream.MD_DESCRIPTIVE.toString();
                                } else {
                                    failed.put(currentPid.getRepositoryPath(),
                                            "Invalid import data, unsupported type in update tag");
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
        } finally {
            if (xmlWriter != null) {
                xmlWriter.close();
            }
        }

    }

    private void close() {
        if (xmlReader != null) {
            try {
                xmlReader.close();
            } catch (XMLStreamException e) {
                log.error("Failed to close XML Reader during CDR metadata update", e);
            }
        }
    }

    private void cleanup() {
        if (importFile != null) {
            importFile.delete();
        }
    }

    private void sendCompletedEmail(List<String> updated, Map<String, String> failed) {
        try {
            msg.setFrom(fromAddress);
            log.error("Sending email to '{}'", userEmail);
            if (userEmail == null || userEmail.trim().length() == 0) {
                // No email provided, send to admins instead
                msg.addTo(fromAddress);
            } else {
                msg.addTo(userEmail);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("fileName", importFile.getPath());

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
                msg.setSubject("CDR Metadata update completed: " + importFile.getPath());
            }

            String html = completeTemplate.execute(data);
            msg.setText(html, true);

            mailSender.send(mimeMsg);
        } catch (MessagingException e) {
            log.error("Failed to send email to " + userEmail
                    + " for update " + importFile.getAbsolutePath(), e);
        }
    }

    private void sendValidationFailureEmail(Map<String, String> problems) {
        try {
            msg.setFrom(fromAddress);
            if (userEmail == null || userEmail.trim().length() == 0) {
                // No email provided, send to admins instead
                msg.addTo(fromAddress);
            } else {
                msg.addTo(userEmail);
            }

            msg.setSubject("CDR Metadata update failed");

            Map<String, Object> data = new HashMap<>();
            data.put("fileName", importFile.getPath());
            data.put("problems", problems.entrySet());
            data.put("problemCount", problems.size());

            String html = failedTemplate.execute(data);
            try {
                msg.setText(html, true);
            } catch (IllegalArgumentException e) {
                log.error("Text of failure email was null, probably due to bad filepath of submission");
                msg.setText("Please check the filepath of your submission and try again", true);
            }

            mailSender.send(mimeMsg);
        } catch (MessagingException e) {
            log.error("Failed to send email to " + userEmail
                    + " for update " + importFile.getAbsolutePath(), e);
        }
    }

}
