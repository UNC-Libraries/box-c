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
package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static edu.unc.lib.boxc.search.api.FacetConstants.MARKED_FOR_DELETION;

/**
 * Service which handles export of CSV representations of member order
 *
 * @author bbpennel
 */
public class MemberOrderCsvExporter {
    private static final int DEFAULT_PAGE_SIZE = 10000;

    public static final String OBJ_TYPE_HEADER = "Object Type";
    public static final String PID_HEADER = "PID";
    public static final String PARENT_PID_HEADER = "Parent PID";
    public static final String TITLE_HEADER = "Title";
    public static final String FILENAME_HEADER = "Filename";
    public static final String DELETED_HEADER = "Deleted";
    public static final String MIME_TYPE_HEADER = "MIME Type";
    public static final String ORDER_HEADER = "Member Order";

    public static final String[] CSV_HEADERS = new String[] {
            PARENT_PID_HEADER, PID_HEADER, TITLE_HEADER, OBJ_TYPE_HEADER, FILENAME_HEADER,
            MIME_TYPE_HEADER, DELETED_HEADER, ORDER_HEADER};

    private static final List<String> PARENT_REQUEST_FIELDS = Arrays.asList(
            SearchFieldKey.ID.name(), SearchFieldKey.ANCESTOR_PATH.name(), SearchFieldKey.RESOURCE_TYPE.name());

    private static final List<String> MEMBER_REQUEST_FIELDS = Arrays.asList(
            SearchFieldKey.ID.name(), SearchFieldKey.TITLE.name(), SearchFieldKey.DATASTREAM.name(),
            SearchFieldKey.STATUS.name(), SearchFieldKey.FILE_FORMAT_TYPE.name(), SearchFieldKey.RESOURCE_TYPE.name(),
            SearchFieldKey.ANCESTOR_PATH.name(), SearchFieldKey.MEMBER_ORDER_ID.name());

    private SolrSearchService solrSearchService;
    private AccessControlService aclService;

    /**
     * Export order information for a list of pids in CSV format
     * @param pids pids of objects to export
     * @param agent user agent making the request
     * @return path to the CSV file
     */
    public Path export(List<PID> pids, AgentPrincipals agent) throws IOException {
        var csvPath = Files.createTempFile("member_order", ".csv");
        var completedExport = false;
        try (var csvPrinter = createCsvPrinter(csvPath)) {
            for (PID parentPid : pids) {
                aclService.assertHasAccess("Insufficient permissions to order members of " + parentPid.getId(),
                        parentPid, agent.getPrincipals(), Permission.viewHidden);
                var parentRec = getParentRecord(parentPid, agent);
                assertParentRecordValid(parentPid, parentRec);

                printRecords(csvPrinter, getChildrenRecords(parentRec, agent));
            }
            completedExport = true;
        } catch (AccessRestrictionException | InvalidOperationForObjectType e) {
            throw e;
        } catch (Exception e) {
            throw new RepositoryException("Failed to export CSV", e);
        } finally {
            // Cleanup the csv file if it is incomplete
            if (!completedExport) {
                Files.deleteIfExists(csvPath);
            }
        }
        return csvPath;
    }

    private CSVPrinter createCsvPrinter(Path csvPath) throws IOException {
        var writer = Files.newBufferedWriter(csvPath);
        return new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader(CSV_HEADERS));
    }

    private void printRecords(CSVPrinter csvPrinter, List<ContentObjectRecord> children) throws IOException {
        for (var childRec : children) {
            printRecord(csvPrinter, childRec);
        }
    }

    // Print a single objects metadata to the CSV export
    private void printRecord(CSVPrinter printer, ContentObjectRecord object) throws IOException {
        printer.print(object.getAncestorPathFacet().getHighestTierNode().getSearchKey());
        printer.print(object.getId());
        printer.print(object.getTitle());
        printer.print(object.getResourceType());
        var original = object.getDatastreamObject(DatastreamType.ORIGINAL_FILE.getId());
        if (original == null || StringUtils.isBlank(original.getFilename())) {
            printer.print("");
        } else {
            printer.print(original.getFilename());
        }
        printer.print(object.getFileFormatType() == null ? "" : object.getFileFormatType().get(0));
        var deleted = object.getStatus() != null && object.getStatus().contains(MARKED_FOR_DELETION);
        printer.print(deleted);
        printer.print(object.getMemberOrderId() == null ? "" : object.getMemberOrderId());
        printer.println();
    }

    // Query for all immediate children/members of the specified record, in default sort order
    private List<ContentObjectRecord> getChildrenRecords(ContentObjectRecord parentRec, AgentPrincipals agent) {
        SearchState searchState = new SearchState();
        searchState.setIgnoreMaxRows(true);
        searchState.setRowsPerPage(DEFAULT_PAGE_SIZE);
        CutoffFacet selectedPath = parentRec.getPath();
        searchState.addFacet(selectedPath);
        searchState.setSortType("default");
        searchState.setResultFields(MEMBER_REQUEST_FIELDS);
        var searchRequest = new SearchRequest(searchState, agent.getPrincipals());
        return solrSearchService.getSearchResults(searchRequest).getResultList();
    }

    private ContentObjectRecord getParentRecord(PID pid, AgentPrincipals agent) {
        var parentRequest = new SimpleIdRequest(pid, PARENT_REQUEST_FIELDS, agent.getPrincipals());
        return solrSearchService.getObjectById(parentRequest);
    }

    private void assertParentRecordValid(PID pid, ContentObjectRecord parentRec) {
        if (parentRec == null) {
            throw new NotFoundException("Unable to find requested record " + pid.getId()
                    + ", it either does not exist or is not accessible");
        }
        if (!ResourceType.Work.nameEquals(parentRec.getResourceType())) {
            throw new InvalidOperationForObjectType("Object " + pid.getId() + " of type " + parentRec.getResourceType()
                    + " does not support member ordering");
        }
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }
}
