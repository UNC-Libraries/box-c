package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.models.DatastreamImpl;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static edu.unc.lib.boxc.web.services.processing.BulkRefIdCsvExporter.CSV_HEADERS;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.parseCsv;
import static org.mockito.MockitoAnnotations.openMocks;

public class BulkRefIdCsvExporterTest {

    private static final String WORK1_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String WORK2_UUID = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String CHILD3_UUID = "8e0040b2-9951-48a3-9d65-780ae7106951";
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

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        exporter = new BulkRefIdCsvExporter();
        exporter.setAclService(aclService);
        exporter.setSolrSearchService(solrSearchService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void exportWorkObject() throws IOException {
        var workRecord = makeRecord(WORK1_UUID, COLLECTION_UUID, ResourceType.Work,
                "Work Title 1", REF1_ID, "Hook ID 1");

        var resultPath = exporter.export(PIDs.get(WORK1_UUID), agent);
        var csvRecords = parseCsv(CSV_HEADERS, resultPath);
    }

    @Test
    public void exportWorkObjectNoRefId() throws IOException {
        var workRecord = makeRecord(WORK1_UUID, COLLECTION_UUID, ResourceType.Work,
                "Work Title 1", null, "Hook ID 1");

        var resultPath = exporter.export(PIDs.get(WORK1_UUID), agent);
        var csvRecords = parseCsv(CSV_HEADERS, resultPath);
    }

    @Test
    public void exportMultipleWorkObjectsUnderCollection() throws IOException {
        var collectionRecord = makeRecord(COLLECTION_UUID, ADMIN_UNIT_UUID, ResourceType.AdminUnit,
                "Collection 1", null, null);
        var workRecord1 = makeRecord(WORK1_UUID, COLLECTION_UUID, ResourceType.Work,
                "Work Title 1", REF1_ID, "Hook ID 1");
        var workRecord2 = makeRecord(WORK2_UUID, COLLECTION_UUID, ResourceType.Work,
                "Work Title 2", REF2_ID, "Hook ID 2");


        var resultPath = exporter.export(PIDs.get(COLLECTION_UUID), agent);
        var csvRecords = parseCsv(CSV_HEADERS, resultPath);
    }

    @Test
    public void exportWorkObjectsUnderAdminUnit() throws IOException {
        var adminUnitRecord = makeRecord(ADMIN_UNIT_UUID, CHILD1_UUID, ResourceType.AdminUnit,
                "AdminUnit 1", null, null);
        var collectionRecord = makeRecord(COLLECTION_UUID, ADMIN_UNIT_UUID, ResourceType.AdminUnit,
                "Collection 1", null, null);
        var workRecord1 = makeRecord(WORK1_UUID, COLLECTION_UUID, ResourceType.Work,
                "Work Title 1", REF1_ID, "Hook ID 1");
        var workRecord2 = makeRecord(WORK2_UUID, COLLECTION_UUID, ResourceType.Work,
                "Work Title 2", REF2_ID, "Hook ID 2");

        var resultPath = exporter.export(PIDs.get(ADMIN_UNIT_UUID), agent);
        var csvRecords = parseCsv(CSV_HEADERS, resultPath);
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
}
