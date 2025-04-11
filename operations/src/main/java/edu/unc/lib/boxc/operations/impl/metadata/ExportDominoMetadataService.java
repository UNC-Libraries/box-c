package edu.unc.lib.boxc.operations.impl.metadata;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
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
//TODO: add ref_id support
public class ExportDominoMetadataService {
    private static final Logger log = LoggerFactory.getLogger(ExportDominoMetadataService.class);

    private static final int DEFAULT_PAGE_SIZE = 10000;

    public static final String REF_ID_NAME = "ref_id";
    public static final String CONTENT_ID_NAME = "content_id";
    public static final String WORK_TITLE_NAME = "work_title";

    public static final String[] CSV_HEADERS = {CONTENT_ID_NAME, REF_ID_NAME, WORK_TITLE_NAME};

    private AccessControlService aclService;
    private SolrSearchService solrSearchService;

    private static final List<String> PARENT_REQUEST_FIELDS = asList(
            SearchFieldKey.ID.name(), SearchFieldKey.ANCESTOR_PATH.name(), SearchFieldKey.RESOURCE_TYPE.name());

    private static final List<String> METADATA_FIELDS = asList(SearchFieldKey.ID.name(),
            SearchFieldKey.TITLE.name());

    /**
     * Export metadata for a list of pids in CSV format
     * @param pids pids of objects to export
     * @param agent user agent making the request
     * @return path to the CSV file
     */
    public Path exportCsv(List<PID> pids, AgentPrincipals agent) throws IOException {
        var csvPath = Files.createTempFile("metadata", ".csv");
        var completedExport = false;

        try (CSVPrinter printer = createCsvPrinter(csvPath)) {
            for (PID pid : pids) {
                aclService.assertHasAccess("Insufficient permissions to export metadata for " + pid.getId(),
                        pid, agent.getPrincipals(), Permission.viewHidden);
                var parentRec = getRecord(pid, agent);
                assertParentRecordValid(pid, parentRec);

                printRecords(printer, getRecords(parentRec, agent));
            }
            completedExport = true;
        } catch (AccessRestrictionException | InvalidOperationForObjectType e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to export CSV: {}", e.getMessage());
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
        log.debug("Printing record for {}", object.getId());

        printer.print(object.getId());
        printer.print(REF_ID_NAME);
        printer.print(object.getTitle());
        printer.println();
    }

    // Query for all children/members of the specified record, in default sort order
    private List<ContentObjectRecord> getRecords(ContentObjectRecord parentRec, AgentPrincipals agent) {
        SearchState searchState = new SearchState();
        searchState.setIgnoreMaxRows(true);
        searchState.setRowsPerPage(DEFAULT_PAGE_SIZE);
        CutoffFacet selectedPath = parentRec.getPath();
        searchState.addFacet(selectedPath);
        searchState.setResourceTypes(asList(Work.name()));
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
        List<ResourceType> allowedTypes = Arrays.asList(ResourceType.ContentRoot, ResourceType.AdminUnit,
                ResourceType.Collection, ResourceType.Folder);
        var resourceType = ResourceType.valueOf(parentRec.getResourceType());
        if (!allowedTypes.contains(resourceType)) {
            throw new InvalidOperationForObjectType("Object " + pid.getId() + " of type "
                    + resourceType.name() + " is not valid for DOMino metadata export");
        }
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }
}
