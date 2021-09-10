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
package edu.unc.lib.boxc.services.camel.exportxml;

import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createSAXBuilder;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.utils.EmailHandler;
import edu.unc.lib.boxc.operations.jms.exportxml.ExportXMLRequest;
import edu.unc.lib.boxc.operations.jms.exportxml.ExportXMLRequestService;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.SearchStateFactory;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import io.dropwizard.metrics5.Timer;

/**
 * Job that performs the work of retrieving metadata documents and compiling them into the export document.
 *
 * @author bbpennel
 */
public class ExportXMLProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(ExportXMLProcessor.class);
    private final List<String> resultFields = Arrays.asList(SearchFieldKey.ID.name());

    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private EmailHandler emailHandler;
    private SearchStateFactory searchStateFactory;
    private SolrSearchService searchService;
    private ExportXMLRequestService requestService;

    private static final int BUFFER_SIZE = 2048;
    private static final String SEPERATOR = System.getProperty("line.separator");
    private static final byte[] SEPERATOR_BYTES = SEPERATOR.getBytes();
    private static final byte[] exportHeaderBytes = ("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + SEPERATOR
            + "<bulkMetadata>" + SEPERATOR).getBytes(UTF_8);

    private static final Timer timer = TimerFactory.createTimerForClass(ExportXMLProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        log.debug("Processing xml import request");
        final Message in = exchange.getIn();

        long startTime = System.currentTimeMillis();
        ExportXMLRequest request = requestService.deserializeRequest((String) in.getBody());
        if (request.getExportChildren()) {
            addChildPIDsToRequest(request);
            log.debug("Finished retrieving children PIDs for export in {}ms", System.currentTimeMillis() - startTime);
        }

        log.debug("Preparing to export metadata for {} objects", request.getPids().size());
        try (Timer.Context context = timer.time()) {
            File mdExportFile = File.createTempFile("xml_export", ".xml");

            try (FileOutputStream xfop = new FileOutputStream(mdExportFile)) {
                xfop.write(exportHeaderBytes);

                XMLOutputter xmlOutput = new XMLOutputter(Format.getRawFormat());

                for (String pidString : request.getPids()) {
                    addObjectToExport(pidString, xfop, xmlOutput, request);
                }

                xfop.write("</bulkMetadata>".getBytes(UTF_8));
            }

            sendEmail(zipit(mdExportFile), request);
            log.info("Finished metadata export for {} objects in {}ms for user {}",
                    request.getPids().size(), System.currentTimeMillis() - startTime, request.getAgent().getUsername());
        } catch (IOException e) {
            throw new ServiceException("Unable to write export file", e);
        }
    }

    private void addChildPIDsToRequest(ExportXMLRequest request) throws ServiceException {
        List<String> pids = new ArrayList<>();
        for (String pid : request.getPids()) {
            SearchState searchState = searchStateFactory.createSearchState();
            searchState.setResultFields(resultFields);
            searchState.setSortType("export");
            searchState.setRowsPerPage(Integer.MAX_VALUE);
            searchState.setIgnoreMaxRows(true);

            SearchRequest searchRequest = new SearchRequest(searchState, request.getAgent().getPrincipals());
            searchRequest.setRootPid(PIDs.get(pid));
            searchRequest.setApplyCutoffs(false);
            searchService.addSelectedContainer(
                    PIDs.get(pid), searchState, false, request.getAgent().getPrincipals());
            SearchResultResponse resultResponse = searchService.getSearchResults(searchRequest);
            if (resultResponse == null) {
                throw new ServiceException("An error occurred while retrieving children of " + pid + " for export.");
            }

            // Add back in the parent pid
            pids.add(pid);

            List<ContentObjectRecord> objects = resultResponse.getResultList();
            for (ContentObjectRecord object : objects) {
                pids.add(object.getPid().toString());
            }
        }
        // update the list of pids in the request with all of the child pids found
        request.setPids(pids);
    }

    private void addObjectToExport(String pidString, FileOutputStream xfop, XMLOutputter xmlOutput,
            ExportXMLRequest request)
            throws IOException {
        PID pid = PIDs.get(pidString);

        if (!aclService.hasAccess(pid, request.getAgent().getPrincipals(), Permission.bulkUpdateDescription)) {
            log.warn("User {} does not have permission to export metadata for {}",
                    request.getAgent().getUsername(), pid);
            return;
        }
        ContentObject obj = (ContentObject) repoObjLoader.getRepositoryObject(pid);
        BinaryObject mods = obj.getDescription();

        try {
            Document objectDoc = new Document();
            Element objectEl = new Element("object");
            objectEl.setAttribute("pid", pid.getQualifiedId());
            objectEl.setAttribute("type", obj.getResourceType().toString());
            objectDoc.addContent(objectEl);

            if (mods != null) {
                Document dsDoc;
                try (InputStream modsStream = mods.getBinaryStream()) {
                    dsDoc = createSAXBuilder().build(modsStream);
                }

                objectEl.addContent(SEPERATOR);

                Element modsUpdateEl = new Element("update");
                modsUpdateEl.setAttribute("type", "MODS");
                modsUpdateEl.setAttribute("lastModified", mods.getLastModified().toString());
                modsUpdateEl.addContent(SEPERATOR);
                modsUpdateEl.addContent(dsDoc.detachRootElement());
                modsUpdateEl.addContent(SEPERATOR);
                objectEl.addContent(modsUpdateEl);
                objectEl.addContent(SEPERATOR);
            }

            xmlOutput.output(objectEl, xfop);

            xfop.write(SEPERATOR_BYTES);
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

    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setSearchStateFactory(SearchStateFactory searchStateFactory) {
        this.searchStateFactory = searchStateFactory;
    }

    public void setSearchService(SolrSearchService searchService) {
        this.searchService = searchService;
    }

    public void setRequestService(ExportXMLRequestService requestService) {
        this.requestService = requestService;
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

    private void sendEmail(File mdExportFile, ExportXMLRequest request) {
        String emailBody = "The XML metadata for " + request.getPids().size() +
                " object(s) requested for export by " + request.getAgent().getUsername() + " is attached.\n";

        emailHandler.sendEmail(request.getEmail(), "DCR Metadata Export", emailBody, "xml_export.zip", mdExportFile);
    }
}
