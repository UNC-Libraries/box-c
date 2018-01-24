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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.cdr.services.rest.modify.ExportXMLController.XMLExportRequest;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;


    /**
     * Job that performs the work of retrieving metadata documents and compiling them into the export document.
     *
     * @author bbpennel
     * @author harring
     */
public class XMLExportJob implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(XMLExportJob.class);

    private JavaMailSender mailSender;
    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;

    private final int BUFFER_SIZE = 2048;
    private final Charset utf8 = Charset.forName("UTF-8");
    private final String separator = System.getProperty("line.separator");
    private final byte[] separatorBytes = System.getProperty("line.separator").getBytes();
    private final byte[] exportHeaderBytes = ("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + separator
            + "<bulkMetadata>" + separator).getBytes(utf8);

    private final String user;
    private final AccessGroupSet groups;
    private final XMLExportRequest request;

    public XMLExportJob(String user, AccessGroupSet groups, XMLExportRequest request) {
        this.user = user;
        this.groups = groups;
        this.request = request;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();

        try {
            GroupsThreadStore.storeGroups(groups);
            GroupsThreadStore.storeUsername(user);

            File mdExportFile = File.createTempFile("xml_export", ".xml");

            try (FileOutputStream xfop = new FileOutputStream(mdExportFile)) {
                xfop.write(exportHeaderBytes);

                XMLOutputter xmlOutput = new XMLOutputter(Format.getRawFormat());
                for (String pidString : request.getPids()) {
                    PID pid = PIDs.get(pidString);
                    if (!aclService.hasAccess(pid, groups, Permission.bulkUpdateDescription)) {
                        log.debug("User {} does not have permission to export metadata for {}", user, pid);
                        continue;
                    }
                    ContentObject obj = (ContentObject) repoObjLoader.getRepositoryObject(pid);
                    BinaryObject mods = obj.getMODS();
                    if (mods == null) {
                        Element objectEl = new Element("object");
                        objectEl.setAttribute("pid", pid.toString());
                        xmlOutput.output(objectEl, xfop);
                        xfop.flush();
                        continue;
                    }

                    try {
                        InputStream modsStream = mods.getBinaryStream();
                        Document dsDoc = new SAXBuilder().build(modsStream);
                        Element objectEl = new Element("object");
                        objectEl.setAttribute("pid", pid.toString());
                        dsDoc.addContent(objectEl);

                        xmlOutput.output(objectEl, xfop);

                        objectEl.addContent(separator);

                        Element modsUpdateEl = new Element("update");
                        modsUpdateEl.setAttribute("type", "MODS");
                        modsUpdateEl.setAttribute("lastModified", obj.getLastModified().toString());
                        modsUpdateEl.addContent(separator);
                        modsUpdateEl.addContent(dsDoc.detachRootElement());
                        modsUpdateEl.addContent(separator);
                        objectEl.addContent(modsUpdateEl);
                        objectEl.addContent(separator);

                        xfop.write(separatorBytes);
                        xfop.flush();
                } catch (Exception e) {
                    log.error("Failed to export XML for object {}", pid, e);
                }
            }

                xfop.write("</bulkMetadata>".getBytes(utf8));
        }

            sendEmail(zipit(mdExportFile), request.getEmail());
        } catch (Exception e) {
            log.error("Failed to export metadata for user {}", user, e);
        } finally {
            log.info("Finished metadata export for {} objects in {}ms for user {}",
                    new Object[] {request.getPids().size(), System.currentTimeMillis() - startTime, user});
            GroupsThreadStore.clearStore();
        }
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }

    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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

    private File zipit(File mdExportFile) throws IOException {
        File mdExportZip = File.createTempFile("xml_export", ".zip");
        FileOutputStream dest = new FileOutputStream(mdExportZip);

        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest))) {
            FileInputStream fi = new FileInputStream(mdExportFile);
            try (BufferedInputStream origin = new BufferedInputStream(fi, BUFFER_SIZE)) {
                byte data[] = new byte[BUFFER_SIZE];

                ZipEntry entry = new ZipEntry("export.xml");
                out.putNextEntry(entry);

                int count;
                while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                    out.write(data, 0, count);
                }
            }
        }

        return mdExportZip;
    }

    private void sendEmail(File mdExportFile, String toEmail) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED);

            helper.setSubject("CDR Metadata Export");
            helper.setFrom("cdr@listserv.unc.edu");
            helper.setText("The XML metadata for " + request.getPids().size() +
                    " object(s) requested for export by " + this.user + " is attached.\n");
            helper.setTo(toEmail);
            helper.addAttachment("xml_export.zip", mdExportFile);
            mailSender.send(mimeMessage);
            log.debug("Sending XML export email to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Cannot send notification email", e);
        }
    }
}
