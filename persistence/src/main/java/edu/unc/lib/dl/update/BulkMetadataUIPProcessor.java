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
package edu.unc.lib.dl.update;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.xml.stream.XMLStreamException;

import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.samskivert.mustache.Template;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.RedisWorkerConstants;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Update processor which performs bulk metadata imports from a CDR metadata package
 *
 * @author bbpennel
 * @date Jul 21, 2015
 */
public class BulkMetadataUIPProcessor implements UIPProcessor {

    private static Logger log = LoggerFactory.getLogger(BulkMetadataUIPProcessor.class);

//    private DigitalObjectManager digitalObjectManager;
//    private ManagementClient managementClient;
    private UIPUpdatePipeline validationPipeline;
    private UIPUpdatePipeline transformPipeline;
    private JedisPool jedisPool;
    private JavaMailSender mailSender;
    private Template completeTemplate;
    private Template failedTemplate;
    private String fromAddress;

    @Override
    public void process(UpdateInformationPackage uip) throws UpdateException, UIPException {
        if (!(uip instanceof BulkMetadataUIP)) {
            throw new UIPException("Incorrect UIP class, found " + uip.getClass().getName() + ", expected "
                    + BulkMetadataUIP.class.getName());
        }

        BulkMetadataUIP bulkUIP = (BulkMetadataUIP) uip;
        long start = System.currentTimeMillis();

        try {
            if (bulkUIP.isExistingUpdate()) {
                resume(bulkUIP);
                log.info("Resuming metadata update {} by {}", bulkUIP.getOriginalFilename(), bulkUIP.getUser());
            } else {
                // Store data related to this update in case it is interrupted
                storeUpdateInformation(bulkUIP);
                log.info("Starting metadata update {} by {}", bulkUIP.getOriginalFilename(), bulkUIP.getUser());
            }

            Map<String, String> failed = new HashMap<>();

            BulkMetadataDatastreamUIP singleUIP;
            do {
                try {
                    singleUIP = bulkUIP.getNextUpdate();
                    if (singleUIP == null) {
                        break;
                    }

                    validationPipeline.processUIP(singleUIP);
                } catch (XMLStreamException | JDOMException e) {
                    failed.put(bulkUIP.getCurrentPid().getPid(),
                            "Could not parse XML:   " + e.getMessage());
                    break;
                } catch (UIPException e) {
                    failed.put(bulkUIP.getCurrentPid().getPid(),
                            "Invalid update:  " + e.getMessage());
                }
            } while (true);

            // If there were any validation problems, inform the user and end the update
            if (failed.size() > 0) {
                log.info("Metadata update {} by user {} failed due to {} problems",
                        new Object[] { bulkUIP.getOriginalFilename(), bulkUIP.getUser(), failed.size() });
                sendValidationFailureEmail(bulkUIP, failed);
                cleanup(bulkUIP);
                return;
            }

            // Wait for the repository to become available before proceeding with updates
//            while (!managementClient.isRepositoryAvailable()) {
//                try {
//                    Thread.sleep(10000L);
//                } catch (InterruptedException e) {
//                    return;
//                }
//            }

            // Reset the state of the package so as to start updating from the beginning
            bulkUIP.reset();

            List<String> skipped = new ArrayList<>();
            List<String> updated = new ArrayList<>();
            List<String> outdated = new ArrayList<>();

//            while ((singleUIP = bulkUIP.getNextUpdate()) != null) {
//
//                for (java.util.Map.Entry<String, Element> entry : singleUIP.getIncomingData().entrySet()) {
//                    try {
//                        // Check to see if the checksum of the new datastream matches the existing
//                        edu.unc.lib.dl.fedora.types.Datastream datastream
//                                = managementClient.getDatastream(singleUIP.getPID(), entry.getKey());
//
//                        transformPipeline.processUIP(singleUIP);
//
//                        // New datastream, create it
//                        if (datastream == null) {
//                            File contentFile = File.createTempFile("content", null);
//                            try {
//                                XMLOutputter xmlOutput = new XMLOutputter(Format.getRawFormat());
//                                try (FileOutputStream outStream = new FileOutputStream(contentFile)) {
//                                    xmlOutput.output(entry.getValue(), outStream);
//                                }
//
//                                digitalObjectManager.addOrReplaceDatastream(singleUIP.getPID(),
//                                        Datastream.getDatastream(entry.getKey()), contentFile, "text/xml",
//                                        uip.getUser(), uip.getMessage());
//                                updated.add(singleUIP.getPID().getPid());
//                            } finally {
//                                contentFile.delete();
//                            }
//                        } else {
//                            // Updating existing, so check if the update is necessary
//                            Format formatForChecksum = Format.getCompactFormat();
//                            formatForChecksum.setOmitDeclaration(false);
//                            XMLOutputter checksumOutputter = new XMLOutputter(formatForChecksum);
//
//                            String incomingChecksum = DigestUtils.md5Hex(
//                                    checksumOutputter.outputString(entry.getValue().getDocument())
//                                    .trim().replaceAll("\r\n", ""));
//                            if (!incomingChecksum.equals(datastream.getChecksum())) {
//                                XMLOutputter rawOutputter = new XMLOutputter(Format.getRawFormat());
//                                // or the checksums don't match, so update
//                                try {
//                                    managementClient.modifyDatastream(singleUIP.getPID(), entry.getKey(), null,
//                                            singleUIP.getLastModified(),
//                                            rawOutputter.outputString(entry.getValue()).getBytes("UTF-8"));
//                                    updated.add(singleUIP.getPID().getPid());
//                                } catch (OptimisticLockException e) {
//                                    // Datastream on the server more recent than submitted copy, reject it
//                                    outdated.add(singleUIP.getPID().getPid());
//                                }
//                                log.info("Updated {} for object {} during bulk update",
//                                        new Object[] { entry.getKey(), singleUIP.getPID().getPid()});
//                            } else {
//                                log.debug("Skipping update of {} because the content has not changed.");
//                                skipped.add(singleUIP.getPID().getPid());
//                            }
//                        }
//                    } catch (UIPException | FedoraException e) {
//                        log.error("Failed to perform update to {} as part of bulk update", singleUIP.getPID(), e);
//                        failed.put(singleUIP.getPID().getPid(), e.getMessage());
//                    }
//
//                    // Store information about the last update completed so we can resume if interrupted
//                    updateResumptionPoint(bulkUIP.getPID(), singleUIP);
//                }
//            }

            sendCompletedEmail(bulkUIP, updated, skipped, outdated, failed);
            // Finalize the update and clean up the trash
            cleanup(bulkUIP);

            log.info("Completed metadata update {} by {} containing {} updates after {}ms", new Object[] {
                    bulkUIP.getOriginalFilename(), bulkUIP.getUser(), bulkUIP.getUpdateCount(),
                    (System.currentTimeMillis() - start) });
//        } catch (XMLStreamException | IOException | JDOMException e) {
//            throw new UpdateException("Failed to perform metadata update for user " + uip.getUser(), e);
        } finally {
            bulkUIP.close();
        }
    }

    private void updateResumptionPoint(PID uipPID, BulkMetadataDatastreamUIP singleUIP) {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> values = new HashMap<>();
            values.put("lastPid", singleUIP.getPID().getPid());
            values.put("lastDatastream", singleUIP.getDatastream());
            jedis.hmset(RedisWorkerConstants.BULK_RESUME_PREFIX + uipPID.getPid(), values);
        }
    }

    private void storeUpdateInformation(BulkMetadataUIP uip) {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> values = new HashMap<>();
            values.put("email", uip.getEmailAddress());
            values.put("user", uip.getUser());
            values.put("groups", uip.getGroups().joinAccessGroups(" ", null, false));
            values.put("filePath", uip.getImportFile().getAbsolutePath());
            values.put("originalFilename", uip.getOriginalFilename());
            jedis.hmset(RedisWorkerConstants.BULK_UPDATE_PREFIX + uip.getPID().getPid(), values);
        }
    }

    /**
     * Resumes an interrupted update using the last resumption point stored,
     * moving the update cursor up to the point where the next getNextUpdate
     * call will return the information for the next datastream after where the
     * previous run left off.
     *
     * @param uip
     * @throws UpdateException
     */
    private void resume(BulkMetadataUIP uip) throws UpdateException {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> resumeValues = jedis.hgetAll(RedisWorkerConstants.BULK_RESUME_PREFIX
                    + uip.getPID().getPid());
            if (resumeValues == null) {
                // No resumption info, so store update info just in case
                storeUpdateInformation(uip);
                return;
            }

            // If the update file doesn't exist anymore, clear this update out so it doesn't stick around forever
            if (!uip.getImportFile().exists()) {
                cleanup(uip);
                throw new UpdateException("Unable to resume update " + uip.getPID() + ", could not find update file");
            }

            // Move the update cursor past the last updated object
            try {
                uip.seekNextUpdate(new PID(resumeValues.get("lastPid")), resumeValues.get("lastDatastream"));
            } catch (Exception e) {
                cleanup(uip);
                throw new UpdateException("Failed to parse update package while resuming", e);
        }
        }
    }

    /**
     * Cleans up resumption information and files related to the update
     * @param uip
     */
    private void cleanup(BulkMetadataUIP uip) {
        String pid = uip.getPID().getPid();

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(RedisWorkerConstants.BULK_UPDATE_PREFIX + pid);
            jedis.del(RedisWorkerConstants.BULK_RESUME_PREFIX + pid);
        }

        uip.getImportFile().delete();
    }

    public void sendCompletedEmail(BulkMetadataUIP uip, List<String> updated, List<String> skipped,
            List<String> outdated, Map<String, String> failed) {
        MimeMessage mimeMsg = mailSender.createMimeMessage();
        try {
            MimeMessageHelper msg = new MimeMessageHelper(mimeMsg, MimeMessageHelper.MULTIPART_MODE_MIXED);

            msg.setFrom(fromAddress);
            String toEmail = uip.getEmailAddress();
            log.error("Sending email to '{}'", toEmail);
            if (toEmail == null || toEmail.trim().length() == 0) {
                // No email provided, send to admins instead
                msg.addTo(fromAddress);
            } else {
                msg.addTo(toEmail);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("fileName", uip.getOriginalFilename());

            data.put("updated", updated);

            int updatedCount = updated.size();
            if (skipped.size() > 0) {
                data.put("skippedCount", skipped.size());
                data.put("skipped", skipped);
                updatedCount += skipped.size();
            }
            data.put("updatedCount", updatedCount);

            if (outdated.size() > 0) {
                data.put("outdatedCount", outdated.size());
                data.put("outdated", outdated);
            }

            if (failed.size() > 0) {
                data.put("failedCount", failed.size());
                data.put("failed", failed.entrySet());
            }

            if (outdated.size() > 0 || failed.size() > 0) {
                data.put("issues", true);
                msg.setSubject("CDR Metadata update completed with issues:" + uip.getOriginalFilename());
                msg.addTo(fromAddress);
            } else {
                msg.setSubject("CDR Metadata update completed:" + uip.getOriginalFilename());
            }

            String html = completeTemplate.execute(data);
            msg.setText(html, true);

            mailSender.send(mimeMsg);
        } catch (MessagingException e) {
            log.error("Failed to send email to " + uip.getEmailAddress()
                    + " for update " + uip.getOriginalFilename(), e);
        }
    }

    public void sendValidationFailureEmail(BulkMetadataUIP uip, Map<String, String> problems) {
        MimeMessage mimeMsg = mailSender.createMimeMessage();
        try {
            MimeMessageHelper msg = new MimeMessageHelper(mimeMsg, MimeMessageHelper.MULTIPART_MODE_MIXED);

            msg.setFrom(fromAddress);
            String toEmail = uip.getEmailAddress();
            if (toEmail == null || toEmail.trim().length() == 0) {
                // No email provided, send to admins instead
                msg.addTo(fromAddress);
            } else {
                msg.addTo(toEmail);
            }

            msg.setSubject("CDR Metadata update failed");

            Map<String, Object> data = new HashMap<>();
            data.put("fileName", uip.getOriginalFilename());
            data.put("problems", problems.entrySet());
            data.put("problemCount", problems.size());

            String html = failedTemplate.execute(data);
            msg.setText(html, true);

            mailSender.send(mimeMsg);
        } catch (MessagingException e) {
            log.error("Failed to send email to " + uip.getEmailAddress()
                    + " for update " + uip.getOriginalFilename(), e);
        }
    }

    public void setValidationPipeline(UIPUpdatePipeline validationPipeline) {
        this.validationPipeline = validationPipeline;
    }

    public void setTransformPipeline(UIPUpdatePipeline transformPipeline) {
        this.transformPipeline = transformPipeline;
    }

    public void setCompleteTemplate(Template completeTemplate) {
        this.completeTemplate = completeTemplate;
    }

    public void setFailedTemplate(Template failedTemplate) {
        this.failedTemplate = failedTemplate;
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }
}
