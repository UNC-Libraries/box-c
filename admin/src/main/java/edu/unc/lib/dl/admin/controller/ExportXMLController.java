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
package edu.unc.lib.dl.admin.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fedora.DatastreamDocument;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;

/**
 * Responds to requests to generate an XML document containing metadata for objects in the selected set of objects,
 * and sends the document to the provided email address.
 *
 * @author sreenug
 * @author bbpennel
 * @date Jul 7, 2015
 */
@Controller
public class ExportXMLController {
    private static final Logger log = LoggerFactory.getLogger(ExportXMLController.class);

    @Autowired
    private SearchStateFactory searchStateFactory;
    @Autowired
    private SolrQueryLayerService queryLayer;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private AccessControlService aclService;

    private final List<String> resultFields = Arrays.asList(SearchFieldKeys.ID.name());

    private final int BUFFER_SIZE = 2048;
    private final Charset utf8 = Charset.forName("UTF-8");
    private final String separator = System.getProperty("line.separator");
    private final byte[] separatorBytes = System.getProperty("line.separator").getBytes();
    private final byte[] exportHeaderBytes = ("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + separator
            + "<bulkMetadata>" + separator).getBytes(utf8);

    /**
     * Exports an XML document containing metadata for all objects specified plus all of their children
     *
     * @param exportRequest
     * @param request
     * @return
     * @throws IOException
     * @throws FedoraException
     */
    @RequestMapping(value = "exportContainerXML", method = RequestMethod.POST)
    public @ResponseBody
    Object exportFolder(@RequestBody XMLExportRequest exportRequest,
            HttpServletRequest request) throws IOException, FedoraException {

        List<String> pids = new ArrayList<>();
        for (String pid : exportRequest.getPids()) {
            SearchState searchState = searchStateFactory.createSearchState();
            searchState.setResultFields(resultFields);
            searchState.setSortType("export");
            searchState.setRowsPerPage(Integer.MAX_VALUE);

            SearchRequest searchRequest = new SearchRequest(searchState, GroupsThreadStore.getGroups());

            BriefObjectMetadata container = queryLayer.addSelectedContainer(pid, searchState, false);
            SearchResultResponse resultResponse = queryLayer.getSearchResults(searchRequest);

            List<BriefObjectMetadata> objects = resultResponse.getResultList();
            objects.add(0, container);

            for (BriefObjectMetadata object : objects) {
                pids.add(object.getPid().toString());
            }
        }

        XMLExportRunnable runnable = new XMLExportRunnable(new XMLExportRequest(pids, exportRequest.getEmail()),
                GroupsThreadStore.getUsername(), GroupsThreadStore.getGroups());

        Thread thread = new Thread(runnable);
        thread.start();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Metadata export for " + pids.size()
                + " objects has begun, you will receive the data via email soon");
        return response;
    }

    /**
     * Generates an XML document containing metadata for all objects in the provided list of PIDs.
     *
     * @param exportRequest
     * @return
     * @throws IOException
     * @throws FedoraException
     */
    @RequestMapping(value = "exportXML", method = RequestMethod.POST)
    public @ResponseBody
    Object exportSet(@RequestBody XMLExportRequest exportRequest) throws IOException, FedoraException {

        XMLExportRunnable runnable = new XMLExportRunnable(
                exportRequest, GroupsThreadStore.getUsername(), GroupsThreadStore.getGroups());

        Thread thread = new Thread(runnable);
        thread.start();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Metadata export for " + exportRequest.getPids().size()
                + " has begun, you will receive the data via email soon");
        return response;
    }

    public static class XMLExportRequest {
        private List<String> pids;
        private String email;

        public XMLExportRequest() {
        }

        public XMLExportRequest(List<String> pids, String email) {
            this.pids = pids;
            this.email = email;
        }

        public List<String> getPids() {
            return pids;
        }

        public void setPids(List<String> pids) {
            this.pids = pids;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    /**
     * Runnable which performs the work of retrieving metadata documents and compiling them into the export document.
     *
     * @author bbpennel
     * @date Jul 7, 2015
     */
    public class XMLExportRunnable implements Runnable {
        private final String user;
        private final AccessGroupSet groups;
        private final XMLExportRequest request;

        public XMLExportRunnable(XMLExportRequest request, String user, AccessGroupSet groups) {
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
                        PID pid = new PID(pidString);

                        if (!aclService.hasAccess(pid, groups, Permission.editDescription)) {
                            log.debug("User {} does not have permission to export metadata for {}", user, pid);
                            continue;
                        }

                        try {
                            Document objectDoc = new Document();
                            Element objectEl = new Element("object");
                            objectEl.setAttribute("pid", pid.toString());
                            objectDoc.addContent(objectEl);

                            DatastreamDocument modsDS = null;

                            if (modsDS != null) {
                                objectEl.addContent(separator);

                                Element modsUpdateEl = new Element("update");
                                modsUpdateEl.setAttribute("type", "MODS");
                                modsUpdateEl.setAttribute("lastModified", modsDS.getLastModified());
                                modsUpdateEl.addContent(separator);
                                modsUpdateEl.addContent(modsDS.getDocument().detachRootElement());
                                modsUpdateEl.addContent(separator);
                                objectEl.addContent(modsUpdateEl);
                                objectEl.addContent(separator);
                            }

                            xmlOutput.output(objectEl, xfop);

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

        public void sendEmail(File mdExportFile, String toEmail) {
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
}
