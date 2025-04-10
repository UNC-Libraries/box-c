package edu.unc.lib.boxc.operations.impl.metadata;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Service which outputs a CSV listing of metadata that will be submitted to DOMino for Aspace digital object generation
 *
 * @author krwong
 */
//TODO: add ref_id support
public class ExportMetadataCsvService {
    private static final Logger log = LoggerFactory.getLogger(ExportMetadataCsvService.class);

    private static final int DEFAULT_PAGE_SIZE = 10000;

    public static final String REF_ID = "ref_id";
    public static final String UUID = "uuid";
    public static final String WORK_TITLE = "work_title";

    public static final String[] CSV_HEADERS = {REF_ID, UUID, WORK_TITLE};

    private AccessControlService aclService;
    private SolrSearchService solrSearchService;

    private static final List<String> PARENT_REQUEST_FIELDS = Arrays.asList(
            SearchFieldKey.ID.name(), SearchFieldKey.ANCESTOR_PATH.name());

    private static final List<String> METADATA_FIELDS = Arrays.asList(SearchFieldKey.ID.name(),
            SearchFieldKey.TITLE.name());

    /**
     * Export metadata for a list of pids in CSV format
     * @param pids pids of objects to export
     * @param agent user agent making the request
     * @return path to the CSV file
     */
    public Path exportCsv(List<PID> pids, AgentPrincipals agent) throws Exception {
        var csvPath = Files.createTempFile("metadata", ".csv");
        var completedExport = false;

        try (CSVPrinter printer = createCsvPrinter(csvPath)) {
            for (PID pid : pids) {
                aclService.assertHasAccess("Insufficient permissions to export metadata for " + pid.getId(),
                        pid, agent.getPrincipals(), Permission.viewHidden);
                var workRec = getRecord(pid, agent);
                assertWorkRecordValid(pid, workRec);

                printRecords(printer, getRecords(workRec, agent));
            }
            completedExport = true;
        } catch (IOException e) {
            throw new RepositoryException("Failed to export CSV: ", e);
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
        printer.print("ref_id");
        printer.print(object.getId());
        printer.print(object.getTitle());
        printer.println();
    }

    // Query for all immediate children/members of the specified record, in default sort order
    private List<ContentObjectRecord> getRecords(ContentObjectRecord parentRec, AgentPrincipals agent) {
        SearchState searchState = new SearchState();
        searchState.setIgnoreMaxRows(true);
        searchState.setRowsPerPage(DEFAULT_PAGE_SIZE);
        CutoffFacet selectedPath = parentRec.getPath();
        searchState.addFacet(selectedPath);
        searchState.setSortType("default");
        searchState.setResultFields(METADATA_FIELDS);
        var searchRequest = new SearchRequest(searchState, agent.getPrincipals());
        return solrSearchService.getSearchResults(searchRequest).getResultList();
    }

    private ContentObjectRecord getRecord(PID pid, AgentPrincipals agent) {
        var workRequest = new SimpleIdRequest(pid, PARENT_REQUEST_FIELDS, agent.getPrincipals());
        return solrSearchService.getObjectById(workRequest);
    }

    private void assertWorkRecordValid(PID pid, ContentObjectRecord workRec) {
        if (workRec == null) {
            throw new NotFoundException("Unable to find requested record " + pid.getId()
                    + ", it either does not exist or is not accessible");
        }
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }
}
