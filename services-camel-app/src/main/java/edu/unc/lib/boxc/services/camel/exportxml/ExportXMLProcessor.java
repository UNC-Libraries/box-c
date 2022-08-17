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

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.utils.EmailHandler;
import edu.unc.lib.boxc.operations.jms.exportxml.BulkXMLConstants;
import edu.unc.lib.boxc.operations.jms.exportxml.ExportXMLRequest;
import edu.unc.lib.boxc.operations.jms.exportxml.ExportXMLRequestService;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.filters.QueryFilterFactory;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.SearchStateFactory;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import io.dropwizard.metrics5.Timer;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createSAXBuilder;
import static edu.unc.lib.boxc.operations.jms.exportxml.BulkXMLConstants.BULK_MD_TAG;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Job that performs the work of retrieving metadata documents and compiling them into the export document.
 *
 * @author bbpennel
 */
public class ExportXMLProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(ExportXMLProcessor.class);
    private final List<String> resultFieldsParent = Arrays.asList(
            SearchFieldKey.ID.name(), SearchFieldKey.DATASTREAM.name());
    private final List<String> resultFieldsChildren = Arrays.asList(SearchFieldKey.ID.name());

    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private EmailHandler emailHandler;
    private SearchStateFactory searchStateFactory;
    private SolrSearchService searchService;
    private ExportXMLRequestService requestService;
    private int objectsPerExport = 100;

    private static final int BUFFER_SIZE = 2048;
    private static final String SEPERATOR = System.getProperty("line.separator");
    private static final byte[] SEPERATOR_BYTES = SEPERATOR.getBytes();
    private static final byte[] exportHeaderBytes = ("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + SEPERATOR
            + "<" + BULK_MD_TAG + ">" + SEPERATOR).getBytes(UTF_8);

    private static final Timer timer = TimerFactory.createTimerForClass(ExportXMLProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        log.debug("Processing xml import request");
        final Message in = exchange.getIn();

        long startTime = System.currentTimeMillis();
        ExportXMLRequest request = requestService.deserializeRequest((String) in.getBody());
        try (Timer.Context context = timer.time()) {
            initializedIncludedDatastreams(request);
            performExport(request, startTime);
        } catch (IOException e) {
            throw new ServiceException("Unable to write export file", e);
        }
        log.info("Finished metadata export for {} objects in {}ms for user {}",
                request.getPids().size(), System.currentTimeMillis() - startTime, request.getAgent().getUsername());
    }

    private void performExport(ExportXMLRequest request, long startTime) throws IOException {
        int originalPidCount = request.getPids().size();
        if (request.getExportChildren() || request.getOnlyIncludeValidDatastreams()) {
            adjustRequestPids(request);
            log.debug("Finished retrieving children PIDs for export in {}ms", System.currentTimeMillis() - startTime);
        }
        String username = request.getAgent().getUsername();
        int totalPids = request.getPids().size();
        if (totalPids == 0) {
            log.debug("All objects were filtered out of export request by user {} which originally listed {} objects",
                    username, originalPidCount);
            sendEmailNoResults(request, originalPidCount);
            return;
        }

        XMLOutputter xmlOutput = new XMLOutputter(Format.getRawFormat());
        int page = 0;
        while (page * objectsPerExport < totalPids) {
            int pageStart = page * objectsPerExport;
            int pageEnd = pageStart + objectsPerExport;
            if (pageEnd > totalPids) {
                pageEnd = totalPids;
            }
            // Generate export in pages, one export document per page
            List<String> pagePids = request.getPids().subList(pageStart, pageEnd);
            page++;

            // Trim off the milliseconds for filename
            String timestamp = StringUtils.substringBefore(request.getRequestedTimestamp().toString(), ".")
                    .replace(":", "-");
            String filename = String.format("xml_export_%s_%04d", timestamp, page);
            File mdExportFile = File.createTempFile(filename, ".xml");

            log.debug("Preparing to export metadata for objects {} through {} out of {} to {}",
                    pageStart, pageEnd, request.getPids().size(), filename);

            try (BufferedOutputStream xfop = new BufferedOutputStream(new FileOutputStream(mdExportFile))) {
                xfop.write(exportHeaderBytes);

                for (String pidString : pagePids) {
                    addObjectToExport(pidString, xfop, xmlOutput, request);
                }

                xfop.write(("</" + BULK_MD_TAG + ">").getBytes(UTF_8));
            }

            sendEmail(zipit(mdExportFile, filename), request, filename, pageStart, pageEnd, totalPids);
            log.info("Completed exported objects {} through {} for user {} to {}",
                    pageStart, pageEnd, username, filename);
        }
    }

    /**
     * Adjusts the list of requested PIDs by adding in children objects and/or excluding objects
     * which do not have the requested datastreams, depending on options set in the request.
     * @param request
     * @throws ServiceException
     */
    private void adjustRequestPids(ExportXMLRequest request) throws ServiceException {
        String dsField = searchService.solrField(SearchFieldKey.DATASTREAM);
        List<String> pids = new ArrayList<>();
        for (String pid : request.getPids()) {
            SearchState searchState = searchStateFactory.createSearchState();
            searchState.setResultFields(resultFieldsParent);

            // Add back in the parent pid unless we are excluding it because it has no datastreams
            SearchRequest searchRequest = new SearchRequest(searchState, request.getAgent().getPrincipals());
            ContentObjectRecord parent = searchService.addSelectedContainer(
                    PIDs.get(pid), searchState, false, request.getAgent().getPrincipals());
            if (request.getOnlyIncludeValidDatastreams()) {
                for (DatastreamType includedDs : request.getDatastreams()) {
                    Datastream ds = parent.getDatastreamObject(includedDs.getId());
                    if (ds != null && StringUtils.isEmpty(ds.getOwner())) {
                        pids.add(pid);
                        break;
                    }
                }
            } else {
                pids.add(pid);
            }
            if (!request.getExportChildren()) {
                continue;
            }

            // Expand list of requested IDs to include children objects
            searchState.setSortType("export");
            searchState.setRowsPerPage(Integer.MAX_VALUE);
            searchState.setIgnoreMaxRows(true);
            searchState.setResultFields(resultFieldsChildren);
            searchRequest.setApplyCutoffs(false);
            if (request.getOnlyIncludeValidDatastreams()) {
                searchState.addFilter(
                        QueryFilterFactory.createFilter(SearchFieldKey.DATASTREAM, request.getDatastreams()));
            }

            SearchResultResponse resultResponse = searchService.getSearchResults(searchRequest);
            if (resultResponse == null) {
                throw new ServiceException("An error occurred while retrieving children of " + pid + " for export.");
            }
            List<ContentObjectRecord> objects = resultResponse.getResultList();
            for (ContentObjectRecord object : objects) {
                pids.add(object.getPid().getId());
            }
        }
        // update the list of pids in the request with all of the child pids found
        request.setPids(pids);
    }

    private void addObjectToExport(String pidString, OutputStream xfop, XMLOutputter xmlOutput,
            ExportXMLRequest request)
            throws IOException {
        PID pid = PIDs.get(pidString);

        if (!aclService.hasAccess(pid, request.getAgent().getPrincipals(), Permission.bulkUpdateDescription)) {
            log.warn("User {} does not have permission to export metadata for {}",
                    request.getAgent().getUsername(), pid);
            return;
        }
        ContentObject obj = (ContentObject) repoObjLoader.getRepositoryObject(pid);

        Document objectDoc = new Document();
        Element objectEl = new Element(BulkXMLConstants.OBJECT_TAG);
        objectEl.setAttribute(BulkXMLConstants.PID_ATTR, pid.getQualifiedId());
        objectEl.setAttribute(BulkXMLConstants.TYPE_ATTR, obj.getResourceType().toString());
        if (obj instanceof FileObject) {
            objectEl.setAttribute(BulkXMLConstants.PARENT_ID_ATTR, obj.getParentPid().getQualifiedId());
        }
        objectDoc.addContent(objectEl);

        for (DatastreamType dsType : request.getDatastreams()) {
            PID dsPid = DatastreamPids.getDatastreamPid(pid, dsType);

            try {
                BinaryObject dsObj = repoObjLoader.getBinaryObject(dsPid);

                objectEl.addContent(SEPERATOR);

                Element datastreamEl = new Element(BulkXMLConstants.DATASTREAM_TAG);
                datastreamEl.setAttribute(BulkXMLConstants.TYPE_ATTR, dsType.getId());
                datastreamEl.setAttribute(BulkXMLConstants.MODIFIED_ATTR,
                        dsObj.getLastModified().toInstant().toString());
                String mimetype = dsObj.getMimetype();
                datastreamEl.setAttribute(BulkXMLConstants.MIMETYPE_ATTR, mimetype);
                datastreamEl.addContent(SEPERATOR);
                if (BulkXMLConstants.UPDATEABLE_DS_TYPES.contains(dsType)) {
                    datastreamEl.setAttribute(BulkXMLConstants.OPERATION_ATTR, BulkXMLConstants.OPER_UPDATE_ATTR);
                }

                if ("text/xml".equals(mimetype)) {
                    try (InputStream modsStream = dsObj.getBinaryStream()) {
                        Document dsDoc = createSAXBuilder().build(modsStream);
                        datastreamEl.addContent(dsDoc.detachRootElement());
                    }
                } else {
                    datastreamEl.addContent(IOUtils.toString(dsObj.getBinaryStream(), StandardCharsets.UTF_8));
                }
                datastreamEl.addContent(SEPERATOR);
                objectEl.addContent(datastreamEl);
                objectEl.addContent(SEPERATOR);
            } catch (NotFoundException e) {
                log.debug("Object {} has no {} datastream for export", pid.getId(), dsType.getId());
            } catch (JDOMException e) {
                log.error("Failed to parse XML document for {}", dsPid, e);
            }
        }

        xmlOutput.output(objectEl, xfop);

        xfop.write(SEPERATOR_BYTES);
    }

    private void initializedIncludedDatastreams(ExportXMLRequest request) {
        Set<DatastreamType> dses = request.getDatastreams();
        if (dses == null) {
            request.setDatastreams(BulkXMLConstants.DEFAULT_DS_TYPES);
        } else {
            dses.retainAll(BulkXMLConstants.EXPORTABLE_DS_TYPES);
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

    public void setObjectsPerExport(int objectsPerExport) {
        this.objectsPerExport = objectsPerExport;
    }

    private File zipit(File mdExportFile, String filename) throws IOException {
        File mdExportZip = File.createTempFile("xml_export", ".zip");
        FileOutputStream dest = new FileOutputStream(mdExportZip);

        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest))) {
            FileInputStream fi = new FileInputStream(mdExportFile);
            try (BufferedInputStream origin = new BufferedInputStream(fi, BUFFER_SIZE)) {
                byte data[] = new byte[BUFFER_SIZE];

                ZipEntry entry = new ZipEntry(filename + ".xml");
                out.putNextEntry(entry);

                int count;
                while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                    out.write(data, 0, count);
                }
            }
        }

        return mdExportZip;
    }

    private void sendEmail(File mdExportFile, ExportXMLRequest request, String filename,
            int pageStart, int pageEnd, int totalPids) {
        String emailBody;
        if (totalPids > objectsPerExport) {
            emailBody = "The XML metadata for objects " + (pageStart + 1) + "-" + pageEnd + " out of " + totalPids
                    + " total objects selected for export by user " + request.getAgent().getUsername()
                    + " is attached.\n";
        } else {
            emailBody = "The XML metadata for " + totalPids +
                    " object(s) requested for export by " + request.getAgent().getUsername() + " is attached.\n";
        }

        emailHandler.sendEmail(request.getEmail(), "DCR Metadata Export", emailBody, filename + ".zip", mdExportFile);
    }

    private void sendEmailNoResults(ExportXMLRequest request, int originalPidCount) {
        String emailBody = "Request to export metadata for objects initiated by " + request.getAgent().getUsername()
                + " at " + request.getRequestedTimestamp() + " returned no results.\n";
        if (request.getOnlyIncludeValidDatastreams()) {
                emailBody += "\nThe request specified " + originalPidCount + " objects for export, but "
                        + "no objects contained the requested datastreams.";
        }

        emailHandler.sendEmail(request.getEmail(), "DCR Metadata Export returned no results", emailBody, null, null);
    }
}
