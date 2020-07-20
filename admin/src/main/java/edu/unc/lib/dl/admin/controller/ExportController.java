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

import static edu.unc.lib.dl.model.DatastreamType.ORIGINAL_FILE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.Datastream;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.service.ChildrenCountService;
import edu.unc.lib.dl.search.solr.util.FacetConstants;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;

/**
 *
 * @author bbpennel
 *
 */
@Controller
@RequestMapping("export")
public class ExportController extends AbstractSolrSearchController {

    @Autowired
    private ChildrenCountService childrenCountService;

    protected SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");

    @RequestMapping(value = "{pid}", method = RequestMethod.GET)
    public void export(@PathVariable("pid") String pidString, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        PID pid = PIDs.get(pidString);
        String filename = pid.getId().replace(":", "_") + ".csv";
        response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.addHeader("Content-Type", "text/csv");

        SearchRequest searchRequest = generateSearchRequest(request, searchStateFactory.createSearchState());
        searchRequest.setRootPid(pid);
        searchRequest.setApplyCutoffs(false);

        SearchState searchState = searchRequest.getSearchState();
        searchState.setResultFields(Arrays.asList(SearchFieldKeys.ID.name(), SearchFieldKeys.TITLE.name(),
                SearchFieldKeys.RESOURCE_TYPE.name(), SearchFieldKeys.ANCESTOR_IDS.name(),
                SearchFieldKeys.STATUS.name(), SearchFieldKeys.DATASTREAM.name(),
                SearchFieldKeys.ANCESTOR_PATH.name(), SearchFieldKeys.CONTENT_MODEL.name(),
                SearchFieldKeys.DATE_ADDED.name(), SearchFieldKeys.DATE_UPDATED.name(),
                SearchFieldKeys.LABEL.name(), SearchFieldKeys.CONTENT_STATUS.name()));
        searchState.setSortType("export");
        searchState.setRowsPerPage(searchSettings.maxPerPage);

        BriefObjectMetadata container = queryLayer.addSelectedContainer(pid, searchState, false,
                searchRequest.getAccessGroups());
        SearchResultResponse resultResponse = queryLayer.getSearchResults(searchRequest);

        List<BriefObjectMetadata> objects = resultResponse.getResultList();
        objects.add(0, container);
        childrenCountService.addChildrenCounts(objects, searchRequest.getAccessGroups());

        try (ServletOutputStream out = response.getOutputStream()) {
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));

            try (CSVPrinter printer = CSVFormat.EXCEL.print(writer)) {
                printHeaders(printer);

                for (BriefObjectMetadata object : objects) {
                    printObject(printer, object);
                }
            }
        }
    }

    private void printHeaders(CSVPrinter printer) throws IOException {
        printer.printRecord("Object Type", "PID", "Title", "Path", "Label","Depth",
                "Deleted", "Date Added", "Date Updated", "MIME Type", "Checksum",
                "File Size (bytes)", "Number of Children", "Description");
    }

    private void printObject(CSVPrinter printer, BriefObjectMetadata object) throws IOException {

        // Vitals: object type, pid, title, path, label, depth

        printer.print(object.getResourceType());
        printer.print(object.getId());
        printer.print(object.getTitle());
        printer.print(object.getAncestorNames());

        String label = object.getLabel();

        if (label != null) {
            printer.print(label);
        } else {
            printer.print("");
        }

        printer.print(object.getAncestorPathFacet().getHighestTier());

        // Status: deleted

        if (object.getStatus() != null) {
            printer.print(object.getStatus().contains("Deleted") ||
                    object.getStatus().contains("Parent Deleted"));
        } else {
            printer.print("");
        }


        // Dates: added, updated

        Date added = object.getDateAdded();

        if (added != null) {
            printer.print(dateFormat.format(added));
        } else {
            printer.print("");
        }

        Date updated = object.getDateUpdated();

        if (updated != null) {
            printer.print(dateFormat.format(updated));
        } else {
            printer.print("");
        }

        // DATA_FILE info: mime type, checksum, file size
        Datastream dataFileDatastream = object.getDatastreamObject(ORIGINAL_FILE.getId());

        if (dataFileDatastream != null) {
            printer.print(dataFileDatastream.getMimetype());
            printer.print(dataFileDatastream.getChecksum().replace("urn:", ""));

            Long filesize = dataFileDatastream.getFilesize();

            // If we don't have a filesize for whatever reason, print a blank
            if (filesize != null && filesize >= 0) {
                printer.print(filesize);
            } else {
                printer.print("");
            }
        } else {
            printer.print("");
            printer.print("");
            printer.print("");
        }

        // Container info: child count

        Long childCount = object.getCountMap().get("child");
        if (childCount > 0) {
            printer.print(childCount.toString());
        } else {
            printer.print("");
        }

        // Description: does object have a MODS description?

        if (object.getContentStatus().contains(FacetConstants.CONTENT_NOT_DESCRIBED) ) {
            printer.print(FacetConstants.CONTENT_NOT_DESCRIBED);
        } else if (object.getContentStatus().contains(FacetConstants.CONTENT_DESCRIBED)) {
            printer.print(FacetConstants.CONTENT_DESCRIBED);
        } else {
            printer.print("");
        }

        printer.println();

    }

}