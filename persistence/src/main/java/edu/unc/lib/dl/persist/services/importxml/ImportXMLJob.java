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
package edu.unc.lib.dl.persist.services.importxml;

import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createXMLInputFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
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

import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.persist.api.indexing.IndexingPriority;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.persist.api.transfer.MultiDestinationTransferSession;
import edu.unc.lib.dl.persist.services.edit.UpdateDescriptionService;
import edu.unc.lib.dl.persist.services.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.dl.validation.MetadataValidationException;
import io.dropwizard.metrics5.Timer;

/**
 * A job for stepping through the bulk metadata update doc and making updates to individual objects
 *
 * @author harring
 *
 */
public class ImportXMLJob implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ImportXMLJob.class);

    private UpdateDescriptionService updateService;
    private JavaMailSender mailSender;
    private Template completeTemplate;
    private Template failedTemplate;
    private String fromAddress;
    private String adminAddress;
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

    private BinaryTransferService transferService;
    private StorageLocationManager locationManager;

    private static final Timer timer = TimerFactory.createTimerForClass(ImportXMLJob.class);

    public ImportXMLJob(ImportXMLRequest request) {
        this.userEmail = request.getUserEmail();
        this.agent = request.getAgent();
        this.importFile = request.getImportFile();

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
            throw new RepositoryException("Failed to initialize email templates", e);
        }

        try (
                Timer.Context context = timer.time();
                InputStream importStream = new FileInputStream(importFile);
                MultiDestinationTransferSession session = transferService.getSession();
        ) {
            initializeXMLReader(importStream);
            processUpdates(session, null, null);
            log.info("Finished metadata import for {} objects in {}ms for user {}",
                    objectCount, System.currentTimeMillis() - startTime, username);
            sendCompletedEmail(updated, failed);
        } catch (XMLStreamException e) {
            log.info("Errors reading XML during update " + username, e);
            failed.put(importFile.getAbsolutePath(), "The import file contains XML errors");
            sendValidationFailureEmail(failed);
        } catch (ServiceException e) {
            log.error(e.getMessage());
            failed.put(importFile.getAbsolutePath(), "File is not a bulk-metadata-update doc");
            sendValidationFailureEmail(failed);
        } catch (IOException e) {
            log.error("Failed to read import file {}", importFile, e);
            failed.put(importFile.getAbsolutePath(), "Failed to read import file: " + e.getMessage());
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

    public void setAdminAddress(String adminAddress) {
        this.adminAddress = adminAddress;
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

    /**
     * @param transferService the transferService to set
     */
    public void setTransferService(BinaryTransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * @param locationManager the locationManager to set
     */
    public void setLocationManager(StorageLocationManager locationManager) {
        this.locationManager = locationManager;
    }

    private void initializeXMLReader(InputStream importStream) throws XMLStreamException {
        xmlReader = createXMLInputFactory().createXMLEventReader(importStream);
    }

    private void processUpdates(MultiDestinationTransferSession session, PID resumePid, String resumeDs)
            throws XMLStreamException, ServiceException {
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
                                log.debug("Starting bulk metadata import document");
                                state = DocumentState.IN_BULK;
                            } else {
                                throw new ServiceException("Submitted document is not a bulk-metadata-update doc");
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
                                    try {
                                        currentPid = PIDs.get(pid.getValue());
                                        log.debug("Starting element for object {}", currentPid.getQualifiedId());
                                        objectCount++;
                                        state = DocumentState.IN_OBJECT;
                                    } catch (Exception ex) {
                                        failed.put(pid.getValue(), "Invalid PID attribute: " + ex.getMessage());
                                    }
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
                                    failed.put(currentPid.getQualifiedId(),
                                            "Invalid import data, missing type attribute on update");
                                }
                                if (MODS_TYPE.equals(typeAttr.getValue())) {
                                    currentDs = MODS_TYPE;
                                    log.debug("Starting MODS element for object {}", currentPid.getQualifiedId());
                                } else {
                                    failed.put(currentPid.getQualifiedId(),
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
                                    log.debug("Ending MODS tag for {}, preparing to update description",
                                            currentPid.getQualifiedId());
                                    BinaryTransferSession transferSession =
                                            session.forDestination(locationManager.getStorageLocation(currentPid));
                                    updateService.updateDescription(
                                            new UpdateDescriptionRequest(agent, currentPid, modsStream)
                                                .withTransferSession(transferSession)
                                                .withPriority(IndexingPriority.low));
                                    updated.add(currentPid.getQualifiedId());
                                    log.debug("Finished updating object {} with id {}", objectCount, currentPid);
                                } catch (AccessRestrictionException ex) {
                                    failed.put(currentPid.getQualifiedId(),
                                            "User doesn't have permission to update this object: " + ex.getMessage());
                                } catch (MetadataValidationException ex) {
                                    log.debug("Validation failed for {}", currentPid, ex);
                                    failed.put(currentPid.getQualifiedId(),
                                            "MODS is not valid: " + ex.getDetailedMessage());
                                } catch (IOException ex) {
                                    failed.put(currentPid.getQualifiedId(),
                                            "Error reading or converting MODS stream: " + ex.getMessage());
                                } catch (NotFoundException ex) {
                                    failed.put(currentPid.getQualifiedId(), "Object not found");
                                } catch (FedoraException ex) {
                                    failed.put(currentPid.getQualifiedId(),
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
                log.error("Failed to close XML Reader during DCR metadata update", e);
            }
        }
    }

    private void cleanup() {
        if (importFile != null) {
            try {
                Files.delete(importFile.toPath());
            } catch (IOException e) {
                log.error("Failed to cleanup import file {}", importFile.getAbsolutePath(), e);
            }
        }
    }

    private void sendCompletedEmail(List<String> updated, Map<String, String> failed) {
        try {
            msg.setFrom(fromAddress);
            log.info("Sending email to '{}'", userEmail);
            if (userEmail == null || userEmail.trim().length() == 0) {
                // No email provided, send to admins instead
                msg.addTo(adminAddress);
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
                msg.setSubject("DCR Metadata update completed with issues:" + importFile.getAbsolutePath());
                msg.addTo(adminAddress);
            } else {
                msg.setSubject("DCR Metadata update completed: " + importFile.getPath());
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
                msg.addTo(adminAddress);
            } else {
                msg.addTo(userEmail);
            }

            msg.setSubject("DCR Metadata update failed");

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
