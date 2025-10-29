package edu.unc.lib.boxc.operations.impl.metadata;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.filters.QueryFilterFactory;
import edu.unc.lib.boxc.search.solr.ranges.RangePair;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static edu.unc.lib.boxc.model.api.ResourceType.Work;
import static java.util.Arrays.asList;

/**
 * Service which outputs a CSV listing of metadata that will be submitted to DOMino for Aspace digital object generation
 *
 * @author krwong
 */
public class ExportDominoMetadataService {
    private static final Logger log = LoggerFactory.getLogger(ExportDominoMetadataService.class);

    private static final int DEFAULT_PAGE_SIZE = 10000;

    public static final String REF_ID_NAME = "ref_id";
    public static final String CONTENT_ID_NAME = "content_id";
    public static final String WORK_TITLE_NAME = "work_title";

    public static final String[] CSV_HEADERS = {CONTENT_ID_NAME, REF_ID_NAME, WORK_TITLE_NAME};

    private AccessControlService aclService;
    private SolrSearchService solrSearchService;
    private AccessCopiesService accessCopiesService;

    private static final List<String> PARENT_REQUEST_FIELDS = asList(
            SearchFieldKey.ID.name(), SearchFieldKey.ANCESTOR_PATH.name(), SearchFieldKey.RESOURCE_TYPE.name());

    private static final List<String> METADATA_FIELDS = asList(SearchFieldKey.ID.name(),
            SearchFieldKey.TITLE.name(), SearchFieldKey.ASPACE_REF_ID.name());

    private static final List<ResourceType> ALLOWED_TYPES = Arrays.asList(ResourceType.ContentRoot,
            ResourceType.AdminUnit, ResourceType.Collection, ResourceType.Folder);

    /**
     * Export metadata for a list of pids in CSV format
     * @param pids pids of objects to export
     * @param agent user agent making the request
     * @return path to the CSV file
     */
    public Path exportCsv(List<PID> pids, AgentPrincipals agent, String startDate, String endDate) throws IOException {
        var csvPath = Files.createTempFile("metadata", ".csv");
        var completedExport = false;
        var exportedRecordCount = 0;

        try (CSVPrinter printer = createCsvPrinter(csvPath)) {
            for (PID pid : pids) {
                aclService.assertHasAccess("Insufficient permissions to export metadata for " + pid.getId(),
                        pid, agent.getPrincipals(), Permission.viewHidden);
                var parentRec = getRecord(pid, agent);
                assertParentRecordValid(pid, parentRec);

                var childRecords = getRecords(parentRec, agent, startDate, endDate);
                exportedRecordCount += childRecords.size();
                printRecords(printer, childRecords);
            }
            if (exportedRecordCount == 0) {
                throw new NoRecordsExportedException("No records exported for pids: " + pids);
            } else {
                log.info("Exported {} records for domino to {}", exportedRecordCount, csvPath);
            }
            completedExport = true;
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
        log.debug("Printing record for {}", object.getId());

        printer.print(object.getId());
        printer.print(object.getAspaceRefId());
        printer.print(object.getTitle());
        printer.println();
    }

    // Query for all children/members of the specified record, in default sort order
    private List<ContentObjectRecord> getRecords(ContentObjectRecord parentRec, AgentPrincipals agent,
                                                 String startDate, String endDate) {
        SearchState searchState = new SearchState();
        searchState.setIgnoreMaxRows(true);
        searchState.setRowsPerPage(DEFAULT_PAGE_SIZE);
        CutoffFacet selectedPath = parentRec.getPath();
        searchState.addFacet(selectedPath);
        // Limit results to only works that have ref ids
        searchState.addFilter(QueryFilterFactory.createFilter(SearchFieldKey.ASPACE_REF_ID));
        searchState.setResourceTypes(List.of(Work.name()));
        searchState.getRangeFields().put(SearchFieldKey.DATE_UPDATED.name(), new RangePair(startDate, endDate));
        searchState.setSortType("default");
        searchState.setResultFields(METADATA_FIELDS);
        var searchRequest = new SearchRequest(searchState, agent.getPrincipals());
        return solrSearchService.getSearchResults(searchRequest).getResultList();
    }

    private ContentObjectRecord getRecord(PID pid, AgentPrincipals agent) {
        var workRequest = new SimpleIdRequest(pid, PARENT_REQUEST_FIELDS, agent.getPrincipals());
        return solrSearchService.getObjectById(workRequest);
    }

    private void assertParentRecordValid(PID pid, ContentObjectRecord parentRec) {
        if (parentRec == null) {
            throw new NotFoundException("Unable to find requested record " + pid.getId()
                    + ", it either does not exist or is not accessible");
        }

        var resourceType = ResourceType.valueOf(parentRec.getResourceType());
        if (!ALLOWED_TYPES.contains(resourceType)) {
            throw new InvalidOperationForObjectType("Object " + pid.getId() + " of type "
                    + resourceType.name() + " is not valid for DOMino metadata export");
        }
    }

    private String getContentType() {
        // Check for viewable images
        if (accessCopiesService.hasViewableFiles(briefObject, principals)) {

        }
        return "link";
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    public static class NoRecordsExportedException extends RuntimeException {
        public NoRecordsExportedException(String message) {
            super(message);
        }
    }
}
