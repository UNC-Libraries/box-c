package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static edu.unc.lib.boxc.web.services.processing.BulkRefIdCsvExporter.EXPORT_CSV_HEADERS;
import static edu.unc.lib.boxc.web.services.processing.BulkRefIdCsvExporter.HOOK_ID_HEADER;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.PID_HEADER;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.REF_ID_HEADER;
import static edu.unc.lib.boxc.web.services.processing.BulkRefIdCsvExporter.TITLE_HEADER;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.parseCsv;
import static edu.unc.lib.boxc.web.services.utils.ExporterTestUtil.assertNumberOfEntries;
import static edu.unc.lib.boxc.web.services.utils.ExporterTestUtil.makeEmptyResponse;
import static edu.unc.lib.boxc.web.services.utils.ExporterTestUtil.mockSearchResults;
import static edu.unc.lib.boxc.web.services.utils.ExporterTestUtil.mockSingleRecordResults;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class BulkRefIdCsvExporterTest {

    private static final String WORK1_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String WORK2_UUID = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String COLLECTION_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";
    private static final String ADMIN_UNIT_UUID = "5158b962-9e59-4ed8-b920-fc948213efd3";
    private static final String REF1_ID = "2817ec3c77e5ea9846d5c070d58d402b";
    private static final String REF2_ID = "1651ewt75rgs1517g4re2rte16874se";
    private AutoCloseable closeable;

    @Mock
    private SolrSearchService solrSearchService;
    @Mock
    private AccessControlService aclService;
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private BulkRefIdCsvExporter exporter;
    private PID workPid;


    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        exporter = new BulkRefIdCsvExporter();
        exporter.setAclService(aclService);
        exporter.setSolrSearchService(solrSearchService);
        workPid = PIDs.get(WORK1_UUID);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void exportWorkObject() throws IOException {
        var workRecord = makeRecord(WORK1_UUID, COLLECTION_UUID, ResourceType.Work,
                "Work Title 1", REF1_ID, "Hook ID 1");

        mockSingleRecordResults(solrSearchService, workRecord);

        var resultPath = exporter.export(workPid, agent);
        var csvRecords = parseCsv(EXPORT_CSV_HEADERS, resultPath);
        assertNumberOfEntries(1, csvRecords);
        assertContainsEntry(csvRecords, WORK1_UUID, REF1_ID, "Hook ID 1", "Work Title 1");
    }

    @Test
    public void exportParentWithNoRefIdChildren() throws IOException {
        var collectionRecord = makeRecord(COLLECTION_UUID, ADMIN_UNIT_UUID, ResourceType.AdminUnit,
                "Collection 1", null, null);

        mockSingleRecordResults( solrSearchService, collectionRecord);
        when(solrSearchService.getSearchResults(any())).thenReturn(makeEmptyResponse());

        var resultPath = exporter.export(PIDs.get(COLLECTION_UUID), agent);
        var csvRecords = parseCsv(EXPORT_CSV_HEADERS, resultPath);
        assertNumberOfEntries(0, csvRecords);
    }

    @Test
    public void exportMultipleWorkObjectsUnderCollection() throws IOException {
        var collectionRecord = makeRecord(COLLECTION_UUID, ADMIN_UNIT_UUID, ResourceType.AdminUnit,
                "Collection 1", null, null);
        var workRecord1 = makeRecord(WORK1_UUID, COLLECTION_UUID, ResourceType.Work,
                "Work Title 1", REF1_ID, "Hook ID 1");
        var workRecord2 = makeRecord(WORK2_UUID, COLLECTION_UUID, ResourceType.Work,
                "Work Title 2", REF2_ID, "Hook ID 2");


        mockSingleRecordResults(solrSearchService, collectionRecord);
        mockSearchResults(solrSearchService, workRecord1, workRecord2);

        var resultPath = exporter.export(PIDs.get(COLLECTION_UUID), agent);
        var csvRecords = parseCsv(EXPORT_CSV_HEADERS, resultPath);
        assertNumberOfEntries(2, csvRecords);
        assertContainsEntry(csvRecords, WORK1_UUID, REF1_ID, "Hook ID 1", "Work Title 1");
        assertContainsEntry(csvRecords, WORK2_UUID, REF2_ID, "Hook ID 2", "Work Title 2");
    }

    @Test
    public void exportWorkObjectsUnderAdminUnit() throws IOException {
        var adminUnitRecord = makeRecord(ADMIN_UNIT_UUID, CHILD1_UUID, ResourceType.AdminUnit,
                "AdminUnit 1", null, null);
        var workRecord1 = makeRecord(WORK1_UUID, COLLECTION_UUID, ResourceType.Work,
                "Work Title 1", REF1_ID, "Hook ID 1");
        var workRecord2 = makeRecord(WORK2_UUID, COLLECTION_UUID, ResourceType.Work,
                "Work Title 2", REF2_ID, null);

        mockSingleRecordResults(solrSearchService, adminUnitRecord);
        mockSearchResults(solrSearchService, workRecord1, workRecord2);

        var resultPath = exporter.export(PIDs.get(ADMIN_UNIT_UUID), agent);
        var csvRecords = parseCsv(EXPORT_CSV_HEADERS, resultPath);
        assertNumberOfEntries( 2, csvRecords);
        assertContainsEntry(csvRecords, WORK1_UUID, REF1_ID, "Hook ID 1", "Work Title 1");
        assertContainsEntry(csvRecords, WORK2_UUID, REF2_ID, "", "Work Title 2");
    }

    @Test
    public void testNoPermission() {
        assertThrows(AccessRestrictionException.class, () -> {
            makeRecord(WORK1_UUID, COLLECTION_UUID, ResourceType.Work,
                    "Work Title 1", REF1_ID, "Hook ID 1");
            doThrow(new AccessRestrictionException()).when(aclService).assertHasAccess(any(),eq(workPid),
                    any(), eq(Permission.editAspaceProperties));

            exporter.export(workPid, agent);
        });
    }

    @Test
    public void testExceptionIsThrown() {
        assertThrows(RepositoryException.class, () -> {
            makeRecord(WORK1_UUID, COLLECTION_UUID, ResourceType.Work,
                    "Work Title 1", REF1_ID, "Hook ID 1");

            exporter.export(PIDs.get(WORK1_UUID), agent);
        });
    }

    private ContentObjectRecord makeRecord(String uuid, String parentUuid, ResourceType resourceType,
                                           String title, String aspaceRefId, String hookId) {
        var rec = new ContentObjectSolrRecord();
        rec.setId(uuid);
        rec.setAncestorPath(makeAncestorPath(parentUuid));
        rec.setResourceType(resourceType.name());
        rec.setTitle(title);
        rec.setAspaceRefId(aspaceRefId);
        rec.setHookId(hookId);
        return rec;
    }

    private List<String> makeAncestorPath(String parentUuid) {
        return Arrays.asList("1,collections", "2," + ADMIN_UNIT_UUID, "3," + COLLECTION_UUID, "4," + parentUuid);
    }

    private void assertContainsEntry(List<CSVRecord> csvRecords, String uuid, String refId, String hookId, String title) {
        for (CSVRecord record : csvRecords) {
            if (!uuid.equals(record.get(PID_HEADER))) {
                continue;
            }
            assertEquals(refId, record.get(REF_ID_HEADER));
            assertEquals(hookId, record.get(HOOK_ID_HEADER));
            assertEquals(title, record.get(TITLE_HEADER));
            return;
        }
        fail("No entry found for uuid " + uuid);
    }
}
