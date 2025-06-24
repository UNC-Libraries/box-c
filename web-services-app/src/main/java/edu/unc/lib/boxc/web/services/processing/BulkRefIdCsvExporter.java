package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.filters.QueryFilterFactory;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static edu.unc.lib.boxc.model.api.ResourceType.Work;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.cleanupCsv;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.createNewCsvPrinter;
import static java.util.Arrays.asList;

public class BulkRefIdCsvExporter {
    private static final Logger log = LoggerFactory.getLogger(BulkRefIdCsvExporter.class);
    private static final int DEFAULT_PAGE_SIZE = 10000;
    public static final String PID_HEADER = "pid";
    public static final String REF_ID_HEADER = "refId";
    public static final String HOOK_ID_HEADER = "hookId";
    public static final String TITLE_HEADER = "title";
    public static final String[] CSV_HEADERS = new String[] {PID_HEADER, REF_ID_HEADER, HOOK_ID_HEADER, TITLE_HEADER};
    private static final List<String> PARENT_REQUEST_FIELDS = asList(
            SearchFieldKey.ID.name(), SearchFieldKey.ANCESTOR_PATH.name(), SearchFieldKey.RESOURCE_TYPE.name(),
            SearchFieldKey.ASPACE_REF_ID.name(), SearchFieldKey.HOOK_ID.name());
    private static final List<String> WORK_FIELDS = asList(
            SearchFieldKey.ID.name(), SearchFieldKey.ASPACE_REF_ID.name(),
            SearchFieldKey.HOOK_ID.name(), SearchFieldKey.TITLE.name());
    private SolrSearchService solrSearchService;
    private AccessControlService aclService;

    public Path export(PID pid, AgentPrincipals agent) throws IOException {
        var csvPath = Files.createTempFile("bulk_ref_ids_" + pid.getId(), ".csv");
        var completedExport = false;

        try (var csvPrinter = createNewCsvPrinter(CSV_HEADERS, csvPath)) {
            aclService.assertHasAccess("Insufficient permissions to export Aspace Ref IDs for " + pid.getId(),
                    pid, agent.getPrincipals(), Permission.editAspaceProperties);
            var parentRecord = getRecord(pid, agent);
            if (Objects.equals(parentRecord.getResourceType(), Work.name())) {
                printRecord(csvPrinter, parentRecord);
            } else {
                var childRecords = getRecords(parentRecord, agent);
                printRecords(csvPrinter, childRecords);
            }
            completedExport = true;
        } catch (AccessRestrictionException | InvalidOperationForObjectType e) {
            throw e;
        } catch (Exception e) {
            throw new RepositoryException("Failed to export CSV", e);
        } finally {
            // Cleanup the csv file if it is incomplete
            if (!completedExport) {
                cleanupCsv(csvPath);
            }
        }
        return csvPath;
    }

    // Query for all children WorkObjects of the specified record, in default sort order
    private List<ContentObjectRecord> getRecords(ContentObjectRecord parentRec, AgentPrincipals agent) {
        SearchState searchState = new SearchState();
        searchState.setIgnoreMaxRows(true);
        searchState.setRowsPerPage(DEFAULT_PAGE_SIZE);
        CutoffFacet selectedPath = parentRec.getPath();
        searchState.addFacet(selectedPath);
        // Limit results to only works that have ref ids
        searchState.addFilter(QueryFilterFactory.createFilter(SearchFieldKey.ASPACE_REF_ID));
        searchState.setResourceTypes(List.of(Work.name()));
        searchState.setSortType("default");
        searchState.setResultFields(WORK_FIELDS);
        var searchRequest = new SearchRequest(searchState, agent.getPrincipals());
        return solrSearchService.getSearchResults(searchRequest).getResultList();
    }

    private ContentObjectRecord getRecord(PID pid, AgentPrincipals agent) {
        var objectRequest = new SimpleIdRequest(pid, PARENT_REQUEST_FIELDS, agent.getPrincipals());
        return solrSearchService.getObjectById(objectRequest);
    }

    private void printRecords(CSVPrinter csvPrinter, List<ContentObjectRecord> children) throws IOException {
        for (var childRec : children) {
            printRecord(csvPrinter, childRec);
        }
    }

    // Print a single object's info to the CSV export
    private void printRecord(CSVPrinter printer, ContentObjectRecord object) throws IOException {
        log.debug("Printing record for {}", object.getId());

        printer.print(object.getId());
        printer.print(object.getAspaceRefId());
        printer.print(object.getHookId());
        printer.print(object.getTitle());
        printer.println();
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }
}
