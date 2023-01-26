package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.models.DatastreamImpl;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static edu.unc.lib.boxc.search.api.FacetConstants.MARKED_FOR_DELETION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * @author bbpennel
 */
public class MemberOrderCsvExporterTest {
    private static final String PARENT1_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String PARENT2_UUID = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String CHILD3_UUID = "8e0040b2-9951-48a3-9d65-780ae7106951";
    private static final String COLLECTION_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";
    private static final String ADMIN_UNIT_UUID = "5158b962-9e59-4ed8-b920-fc948213efd3";

    @Mock
    private SolrSearchService solrSearchService;
    @Mock
    private AccessControlService aclService;
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private MemberOrderCsvExporter csvService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        csvService = new MemberOrderCsvExporter();
        csvService.setSolrSearchService(solrSearchService);
        csvService.setAclService(aclService);
    }

    @Test
    public void exportUnorderedObjectTest() throws Exception {
        var parentRec = makeWorkRecord(PARENT1_UUID, "Work");
        var rec1 = makeRecord(CHILD1_UUID, PARENT1_UUID, ResourceType.File, "File One",
                "file1.txt", "text/plain", null);
        var rec2 = makeRecord(CHILD2_UUID, PARENT1_UUID, ResourceType.File, "File Two",
                "file2.png", "image/png", null);
        mockParentResults(parentRec);
        mockChildrenResults(rec1, rec2);

        var resultPath = csvService.export(asPidList(PARENT1_UUID), agent);
        var csvRecords = parseCsv(resultPath);
        assertNumberOfEntries(2, csvRecords);
        assertContainsEntry(csvRecords, CHILD1_UUID, PARENT1_UUID, "File One",
                "file1.txt", "text/plain", false, null);
        assertContainsEntry(csvRecords, CHILD2_UUID, PARENT1_UUID, "File Two",
                "file2.png", "image/png", false, null);
    }

    @Test
    public void exportPartiallyOrderedObjectTest() throws Exception {
        var parentRec = makeWorkRecord(PARENT1_UUID, "Work");
        var rec1 = makeRecord(CHILD1_UUID, PARENT1_UUID, ResourceType.File, "File One",
                "file1.txt", "text/plain", 0);
        var rec2 = makeRecord(CHILD2_UUID, PARENT1_UUID, ResourceType.File, "File Two",
                "file2.png", "image/png", null);
        mockParentResults(parentRec);
        mockChildrenResults(rec1, rec2);

        var resultPath = csvService.export(asPidList(PARENT1_UUID), agent);
        var csvRecords = parseCsv(resultPath);
        assertContainsEntry(csvRecords, CHILD1_UUID, PARENT1_UUID, "File One",
                "file1.txt", "text/plain", false, 0);
        assertContainsEntry(csvRecords, CHILD2_UUID, PARENT1_UUID, "File Two",
                "file2.png", "image/png", false, null);
        assertNumberOfEntries(2, csvRecords);
    }

    @Test
    public void exportOrderedObjectWithDeletedChildTest() throws Exception {
        var parentRec = makeWorkRecord(PARENT1_UUID, "Work");
        var rec1 = makeRecord(CHILD1_UUID, PARENT1_UUID, ResourceType.File, "File One",
                "file1.txt", "text/plain", 0);
        var rec2 = makeRecord(CHILD2_UUID, PARENT1_UUID, ResourceType.File, "File Two",
                "file2.png", "image/png", 1);
        ((ContentObjectSolrRecord) rec2).setStatus(Arrays.asList(MARKED_FOR_DELETION));
        mockParentResults(parentRec);
        mockChildrenResults(rec1, rec2);

        var resultPath = csvService.export(asPidList(PARENT1_UUID), agent);
        var csvRecords = parseCsv(resultPath);
        assertContainsEntry(csvRecords, CHILD1_UUID, PARENT1_UUID, "File One",
                "file1.txt", "text/plain", false, 0);
        assertContainsEntry(csvRecords, CHILD2_UUID, PARENT1_UUID, "File Two",
                "file2.png", "image/png", true, 1);
        assertNumberOfEntries(2, csvRecords);
    }

    @Test
    public void exportMultipleOrderedObjectsTest() throws Exception {
        var parent1Rec = makeWorkRecord(PARENT1_UUID, "Work 1");
        var rec1 = makeRecord(CHILD1_UUID, PARENT1_UUID, ResourceType.File, "File One",
                "file1.txt", "text/plain", 0);
        var rec2 = makeRecord(CHILD2_UUID, PARENT1_UUID, ResourceType.File, "File Two",
                "file2.png", "image/png", 1);
        var parent2Rec = makeWorkRecord(PARENT2_UUID, "Work 2");
        var rec3 = makeRecord(CHILD3_UUID, PARENT2_UUID, ResourceType.File, "File Three",
                "file3.txt", "text/plain", 0);
        mockParentResults(parent1Rec, parent2Rec);
        when(solrSearchService.getSearchResults(any()))
                .thenReturn(makeResultResponse(rec1, rec2))
                .thenReturn(makeResultResponse(rec3));

        var resultPath = csvService.export(asPidList(PARENT1_UUID, PARENT2_UUID), agent);
        var csvRecords = parseCsv(resultPath);
        assertContainsEntry(csvRecords, CHILD1_UUID, PARENT1_UUID, "File One",
                "file1.txt", "text/plain", false, 0);
        assertContainsEntry(csvRecords, CHILD2_UUID, PARENT1_UUID, "File Two",
                "file2.png", "image/png", false, 1);
        assertContainsEntry(csvRecords, CHILD3_UUID, PARENT2_UUID, "File Three",
                "file3.txt", "text/plain", false, 0);
        assertNumberOfEntries(3, csvRecords);
    }

    @Test
    public void exportFileObjectOrderTest() throws Exception {
        var rec = makeRecord(CHILD1_UUID, PARENT1_UUID, ResourceType.File, "File One",
                "file1.txt", "text/plain", 0);
        resourceTypeNotSupportedTest(rec);
    }

    @Test
    public void exportAdminUnitOrderTest() throws Exception {
        var rec = makeRecord(CHILD1_UUID, PARENT1_UUID, ResourceType.AdminUnit, "Administrivia",
                null, null, null);
        resourceTypeNotSupportedTest(rec);
    }

    @Test
    public void exportCollectionOrderTest() throws Exception {
        var rec = makeRecord(CHILD1_UUID, PARENT1_UUID, ResourceType.Collection, "Coll",
                null, null, null);
        resourceTypeNotSupportedTest(rec);
    }

    @Test
    public void exportFolderOrderTest() throws Exception {
        var rec = makeRecord(CHILD1_UUID, PARENT1_UUID, ResourceType.Folder, "Folding",
                null, null, null);
        resourceTypeNotSupportedTest(rec);
    }

    private void resourceTypeNotSupportedTest(ContentObjectRecord rec) throws Exception {
        mockParentResults(rec);
        try {
            csvService.export(asPidList(CHILD1_UUID), agent);
            fail();
        } catch (InvalidOperationForObjectType e) {
            assertEquals("Object 83c2d7f8-2e6b-4f0b-ab7e-7397969c0682 of type " + rec.getResourceType()
                            + " does not support member ordering", e.getMessage());
        }
    }

    @Test
    public void exportInsufficientPermissionsTest() throws Exception {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException())
                    .when(aclService)
                    .assertHasAccess(anyString(), eq(PIDs.get(PARENT1_UUID)), any(), eq(Permission.viewHidden));
            csvService.export(asPidList(PARENT1_UUID), agent);
        });
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

    // Calls to get parent record will return the provided records, in order
    private void mockParentResults(ContentObjectRecord parentRec, ContentObjectRecord... parentRecs) {
        when(solrSearchService.getObjectById(any())).thenReturn(parentRec, parentRecs);
    }

    private void assertContainsEntry(List<CSVRecord> csvRecords, String uuid, String parentUuid,
                                     String title, String filename, String mimetype, boolean deleted, Integer order) {
        for (CSVRecord record : csvRecords) {
            if (!uuid.equals(record.get(MemberOrderCsvConstants.PID_HEADER))) {
                continue;
            }
            assertEquals(parentUuid, record.get(MemberOrderCsvConstants.PARENT_PID_HEADER));
            assertEquals(ResourceType.File.name(), record.get(MemberOrderCsvConstants.OBJ_TYPE_HEADER));
            assertEquals(title, record.get(MemberOrderCsvConstants.TITLE_HEADER));
            assertEquals(filename, record.get(MemberOrderCsvConstants.FILENAME_HEADER));
            assertEquals(mimetype, record.get(MemberOrderCsvConstants.MIME_TYPE_HEADER));
            assertEquals(Boolean.toString(deleted), record.get(MemberOrderCsvConstants.DELETED_HEADER));
            var expectOrder = order == null ? "" : order.toString();
            assertEquals(expectOrder, record.get(MemberOrderCsvConstants.ORDER_HEADER));
            return;
        }
        fail("No entry found for uuid " + uuid);
    }

    private void assertNumberOfEntries(int expected, List<CSVRecord> csvParser) throws IOException {
        assertEquals(expected, csvParser.size());
    }

    private List<PID> asPidList(String... ids) {
        return Arrays.stream(ids).map(PIDs::get).collect(Collectors.toList());
    }

    private List<CSVRecord> parseCsv(Path csvPath) throws IOException {
        Reader reader = Files.newBufferedReader(csvPath);
        return new CSVParser(reader, CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withHeader(MemberOrderCsvConstants.CSV_HEADERS)
                .withTrim())
                .getRecords();
    }

    private ContentObjectRecord makeWorkRecord(String uuid, String title) {
        return makeRecord(uuid, COLLECTION_UUID, ResourceType.Work, title, null, null, null);
    }

    private ContentObjectRecord makeRecord(String uuid, String parentUuid, ResourceType resourceType, String title,
                                           String filename, String mimetype, Integer order) {
        var rec = new ContentObjectSolrRecord();
        rec.setId(uuid);
        rec.setAncestorPath(makeAncestorPath(parentUuid));
        rec.setResourceType(resourceType.name());
        rec.setTitle(title);
        rec.setFileFormatType(Arrays.asList(mimetype));
        rec.setMemberOrderId(order);
        if (filename != null) {
            var datastream = new DatastreamImpl(null, DatastreamType.ORIGINAL_FILE.getId(), 0l, mimetype,
                    filename, null, null, null);
            rec.setDatastream(Arrays.asList(datastream.toString()));
        }
        return rec;
    }

    private List<String> makeAncestorPath(String parentUuid) {
        return Arrays.asList("1,collections", "2," + ADMIN_UNIT_UUID, "3," + COLLECTION_UUID, "4," + parentUuid);
    }
}
