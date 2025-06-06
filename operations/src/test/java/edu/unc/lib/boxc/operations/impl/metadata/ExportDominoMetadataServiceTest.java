package edu.unc.lib.boxc.operations.impl.metadata;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.models.DatastreamImpl;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.sparql.function.library.date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author krwong
 */
public class ExportDominoMetadataServiceTest {
    private static final String UUID1 = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String UUID2 = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    private static final String COLLECTION_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";
    private static final String COLLECTION_UUID2 = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    private static final String ADMIN_UNIT_UUID = "5158b962-9e59-4ed8-b920-fc948213efd3";
    private static final String REF_ID_1 = "2037b882013c4cf2b1cde53402d8f388";
    private static final String REF_ID_2 = "65671fa41bb24adb907957bae3bb4f4d";

    private AutoCloseable closeable;

    @Mock
    private SolrSearchService solrSearchService;
    @Mock
    private AccessControlService aclService;
    @Mock
    private SearchResultResponse searchResultResponse;
    @Captor
    private ArgumentCaptor<SearchRequest> searchRequest;

    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private ExportDominoMetadataService csvService;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        csvService = new ExportDominoMetadataService();
        csvService.setSolrSearchService(solrSearchService);
        csvService.setAclService(aclService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void exportDominoMetadataTest() throws Exception {
        var collectionRecord = makeRecord(COLLECTION_UUID, ADMIN_UNIT_UUID, ResourceType.Collection,
                "Collection", new Date());
        var workRecord1 = makeWorkRecord(UUID1, "Work 1", REF_ID_1);
        var workRecord2 = makeWorkRecord(UUID2, "Work 2", REF_ID_2);
        mockParentResults(collectionRecord);
        mockChildrenResults(workRecord1, workRecord2);

        var resultPath = csvService.exportCsv(asPidList(COLLECTION_UUID), agent, "*", "*");
        var csvRecords = parseCsv(ExportDominoMetadataService.CSV_HEADERS, resultPath);
        assertContainsEntry(csvRecords, UUID1, REF_ID_1, "Work 1");
        assertContainsEntry(csvRecords, UUID2, REF_ID_2, "Work 2");
        assertNumberOfEntries(2, csvRecords);

        verify(solrSearchService).getSearchResults(searchRequest.capture());
        var searchState = searchRequest.getValue().getSearchState();
        assertTrue(searchState.getRangeFields().containsKey(SearchFieldKey.DATE_CREATED.name()));
        var refIdFilter = searchState.getFilters().get(0);
        assertEquals(SearchFieldKey.ASPACE_REF_ID.getSolrField() + ":*", refIdFilter.toFilterString());
        assertIterableEquals(List.of(ResourceType.Work.name()), searchState.getResourceTypes());
    }
    
    @Test
    public void parentNotFoundTest() {
        when(solrSearchService.getObjectById(any())).thenReturn(null);

        assertThrows(NotFoundException.class, () -> {
            csvService.exportCsv(asPidList(COLLECTION_UUID), agent, "*", "*");
        });
    }

    @Test
    public void userNoPermissionTest() {
        doThrow(new AccessRestrictionException())
                .when(aclService)
                .assertHasAccess(anyString(), eq(PIDs.get(COLLECTION_UUID)), any(), eq(Permission.viewHidden));

        assertThrows(AccessRestrictionException.class, () -> {
            csvService.exportCsv(asPidList(COLLECTION_UUID), agent, "*", "*");
        });
    }

    @Test
    public void parentWithNoChildrenTest() throws Exception {
        var collectionRecord = makeRecord(COLLECTION_UUID, ADMIN_UNIT_UUID, ResourceType.Collection,
                "Collection", new Date());
        mockParentResults(collectionRecord);
        when(solrSearchService.getSearchResults(any())).thenReturn(searchResultResponse);

        assertThrows(ExportDominoMetadataService.NoRecordsExportedException.class,
                () -> csvService.exportCsv(asPidList(COLLECTION_UUID), agent, "*", "*"));
    }

    @Test
    public void invalidIdTypeTest() {
        var workRecord1 = makeWorkRecord(UUID1, "Work 1", REF_ID_1);
        mockParentResults(workRecord1);

        var exception = assertThrows(InvalidOperationForObjectType.class, () -> {
            csvService.exportCsv(asPidList(UUID1), agent, "*", "*");
        });
        assertEquals("Object " + UUID1 + " of type " + workRecord1.getResourceType()
                + " is not valid for DOMino metadata export", exception.getMessage());
    }

    @Test
    public void filterForRecordsCreatedAfterStartDate() throws Exception {
        var collectionRecord1 = makeRecord(COLLECTION_UUID, ADMIN_UNIT_UUID, ResourceType.Collection,
                "Collection", new Date());
        var workRecord1 = makeWorkRecord(UUID1, "Work 1", REF_ID_1);
        Date dateCreated = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXX").parse("2019-00-00T00:00:00Z");
        var collectionRecord2 = makeRecord(COLLECTION_UUID2, ADMIN_UNIT_UUID, ResourceType.Collection,
                "Collection", dateCreated);

        mockParentResults(collectionRecord1, collectionRecord2);
        mockChildrenResults(workRecord1);

        var resultPath = csvService.exportCsv(asPidList(COLLECTION_UUID), agent,
                "2020-00-00T00:00:00Z", "*");

        var csvRecords = parseCsv(ExportDominoMetadataService.CSV_HEADERS, resultPath);
        assertContainsEntry(csvRecords, UUID1, REF_ID_1, "Work 1");
        assertNumberOfEntries(1, csvRecords);

        verify(solrSearchService).getSearchResults(searchRequest.capture());
        var searchState = searchRequest.getValue().getSearchState();
        assertTrue(searchState.getRangeFields().containsKey(SearchFieldKey.DATE_CREATED.name()));
        var refIdFilter = searchState.getFilters().get(0);
        assertEquals(SearchFieldKey.ASPACE_REF_ID.getSolrField() + ":*", refIdFilter.toFilterString());
        assertEquals("2020-00-00T00:00:00Z,*", searchState.getRangeFields()
                .get(SearchFieldKey.DATE_CREATED.name()).getParameterValue());
    }

    private void mockParentResults(ContentObjectRecord parentRec, ContentObjectRecord... parentRecs) {
        when(solrSearchService.getObjectById(any())).thenReturn(parentRec, parentRecs);
    }

    private void mockChildrenResults(ContentObjectRecord... results) {
        when(solrSearchService.getSearchResults(any())).thenReturn(makeResultResponse(results));
    }

    private SearchResultResponse makeResultResponse(ContentObjectRecord... results) {
        var resp = new SearchResultResponse();
        resp.setResultList(Arrays.asList(results));
        resp.setResultCount(results.length);
        return resp;
    }

    private static List<CSVRecord> parseCsv(String[] headers, Path csvPath) throws IOException {
        Reader reader = Files.newBufferedReader(csvPath);
        return new CSVParser(reader, CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withHeader(headers)
                .withTrim())
                .getRecords();
    }

    private void assertContainsEntry(List<CSVRecord> csvRecords, String contentId, String refId, String title) {
        for (CSVRecord record : csvRecords) {
            if (!contentId.equals(record.get(ExportDominoMetadataService.CONTENT_ID_NAME))) {
                continue;
            }
            assertEquals(contentId, record.get(ExportDominoMetadataService.CONTENT_ID_NAME));
            assertEquals(refId, record.get(ExportDominoMetadataService.REF_ID_NAME));
            assertEquals(title, record.get(ExportDominoMetadataService.WORK_TITLE_NAME));
            return;
        }
        fail("No entry found for contentId " + contentId);
    }

    private void assertNumberOfEntries(int expected, List<CSVRecord> csvParser) throws IOException {
        assertEquals(expected, csvParser.size());
    }

    private List<PID> asPidList(String... ids) {
        return Arrays.stream(ids).map(PIDs::get).collect(Collectors.toList());
    }

    private ContentObjectRecord makeWorkRecord(String uuid, String title, String refId) {
        Date dateCreated = new Date();
        var rec = (ContentObjectSolrRecord) makeRecord(uuid, COLLECTION_UUID, ResourceType.Work, title, dateCreated);
        rec.setAspaceRefId(refId);
        return rec;
    }

    private ContentObjectRecord makeRecord(String uuid, String parentUuid, ResourceType resourceType, String title,
                                           Date dateCreated) {
        var rec = new ContentObjectSolrRecord();
        rec.setId(uuid);
        rec.setAncestorPath(makeAncestorPath(parentUuid));
        rec.setResourceType(resourceType.name());
        rec.setTitle(title);
        rec.setRoleGroup(Arrays.asList("patron|public", "canViewOriginals|everyone"));
        rec.setDateCreated(dateCreated);
        return rec;
    }

    private List<String> makeAncestorPath(String parentUuid) {
        return Arrays.asList("1,collections", "2," + ADMIN_UNIT_UUID, "3," + COLLECTION_UUID, "4," + parentUuid);
    }
}
