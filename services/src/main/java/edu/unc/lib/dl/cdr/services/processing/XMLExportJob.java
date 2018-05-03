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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.cdr.services.rest.modify.ExportXMLController.XMLExportRequest;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.persist.services.EmailHandler;
import io.dropwizard.metrics5.Timer;


    /**
     * Job that performs the work of retrieving metadata documents and compiling them into the export document.
     *
     * @author bbpennel
     * @author harring
     */
public class XMLExportJob implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(XMLExportJob.class);

    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private EmailHandler emailHandler;

    private final int BUFFER_SIZE = 2048;
    private final Charset utf8 = Charset.forName("UTF-8");
    private final String separator = System.getProperty("line.separator");
    private final byte[] separatorBytes = System.getProperty("line.separator").getBytes();
    private final byte[] exportHeaderBytes = ("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + separator
            + "<bulkMetadata>" + separator).getBytes(utf8);

    private final String user;
    private final AccessGroupSet groups;
    private final XMLExportRequest request;

    private static final Timer timer = TimerFactory.createTimerForClass(XMLExportJob.class);

    public XMLExportJob(String user, AccessGroupSet groups, XMLExportRequest request) {
        this.user = user;
        this.groups = groups;
        this.request = request;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();

        try (Timer.Context context = timer.time()) {
            File mdExportFile = File.createTempFile("xml_export", ".xml");

            try (FileOutputStream xfop = new FileOutputStream(mdExportFile)) {
                xfop.write(exportHeaderBytes);

                XMLOutputter xmlOutput = new XMLOutputter(Format.getRawFormat());

                for (String pidString : request.getPids()) {
                    addObjectToExport(pidString, xfop, xmlOutput);
                }

                xfop.write("</bulkMetadata>".getBytes(utf8));
            }

            sendEmail(zipit(mdExportFile), request.getEmail());
            log.info("Finished metadata export for {} objects in {}ms for user {}",
                    new Object[] {request.getPids().size(), System.currentTimeMillis() - startTime, user});
        } catch (MessagingException e) {
            log.error("Failed to send export email to {}", request.getEmail(), e);
        } catch (IOException e) {
            throw new ServiceException("Unable to write export file", e);
        }
    }

    private void addObjectToExport(String pidString, FileOutputStream xfop, XMLOutputter xmlOutput)
            throws IOException {
        PID pid = PIDs.get(pidString);

        if (!aclService.hasAccess(pid, groups, Permission.bulkUpdateDescription)) {
            log.warn("User {} does not have permission to export metadata for {}", user, pid);
            return;
        }
        ContentObject obj = (ContentObject) repoObjLoader.getRepositoryObject(pid);
        BinaryObject mods = obj.getMODS();

        try {
            Document objectDoc = new Document();
            Element objectEl = new Element("object");
            objectEl.setAttribute("pid", pid.toString());
            objectDoc.addContent(objectEl);

            if (mods != null) {
                Document dsDoc;
                try (InputStream modsStream = mods.getBinaryStream()) {
                    dsDoc = new SAXBuilder().build(modsStream);
                }

                objectEl.addContent(separator);

                Element modsUpdateEl = new Element("update");
                modsUpdateEl.setAttribute("type", "MODS");
                modsUpdateEl.setAttribute("lastModified", obj.getLastModified().toString());
                modsUpdateEl.addContent(separator);
                modsUpdateEl.addContent(dsDoc.detachRootElement());
                modsUpdateEl.addContent(separator);
                objectEl.addContent(modsUpdateEl);
                objectEl.addContent(separator);
            }

            xmlOutput.output(objectEl, xfop);

            xfop.write(separatorBytes);
            xfop.flush();
        } catch (JDOMException e) {
            log.error("Failed to parse MODS document for {}", pid, e);
        }
    }

    public EmailHandler getEmailHandler() {
        return emailHandler;
    }

    public void setEmailHandler(EmailHandler emailHandler) {
        this.emailHandler = emailHandler;
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

    private void sendEmail(File mdExportFile, String toAddress) throws MessagingException {
        String emailBody = "The XML metadata for " + request.getPids().size() +
                " object(s) requested for export by " + this.user + " is attached.\n";

        emailHandler.sendEmail(toAddress, "CDR Metadata Export", emailBody, "xml_export.zip", mdExportFile);
    }
}
