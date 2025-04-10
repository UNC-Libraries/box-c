package edu.unc.lib.boxc.operations.impl.metadata;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author krwong
 */
public class ExportMetadataCsvServiceTest {
    private static final String UUID1 = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String UUID2 = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    private static final String COLLECTION_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";
    private static final String ADMIN_UNIT_UUID = "5158b962-9e59-4ed8-b920-fc948213efd3";

    private AutoCloseable closeable;

    @Mock
    private SolrSearchService solrSearchService;
    @Mock
    private AccessControlService aclService;
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private ExportMetadataCsvService csvService;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        csvService = new ExportMetadataCsvService();
        csvService.setSolrSearchService(solrSearchService);
        csvService.setAclService(aclService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void metadataCsvTest() throws Exception {
        var workRecord1 = makeWorkRecord(UUID1, "Work 1");
        var workRecord2 = makeWorkRecord(UUID2, "Work 2");
        mockResults(workRecord1, workRecord2);
        when(solrSearchService.getSearchResults(any()))
                .thenReturn(makeResultResponse(workRecord1))
                .thenReturn(makeResultResponse(workRecord2));

        var resultPath = csvService.exportCsv(asPidList(UUID1, UUID2), agent);
        var csvRecords = parseCsv(ExportMetadataCsvService.CSV_HEADERS, resultPath);
        assertContainsEntry(csvRecords, "ref_id", UUID1, "Work 1");
        assertContainsEntry(csvRecords, "ref_id", UUID2, "Work 2");
        assertNumberOfEntries(2, csvRecords);
    }

    private void mockResults(ContentObjectRecord parentRec, ContentObjectRecord... parentRecs) {
        when(solrSearchService.getObjectById(any())).thenReturn(parentRec, parentRecs);
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

    private void assertContainsEntry(List<CSVRecord> csvRecords, String refId, String uuid,
                                     String title) {
        for (CSVRecord record : csvRecords) {
            if (!uuid.equals(record.get(ExportMetadataCsvService.UUID))) {
                continue;
            }
            assertEquals(refId, record.get(ExportMetadataCsvService.REF_ID));
            assertEquals(uuid, record.get(ExportMetadataCsvService.UUID));
            assertEquals(title, record.get(ExportMetadataCsvService.WORK_TITLE));
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
        rec.setRoleGroup(Arrays.asList("patron|public", "canViewOriginals|everyone"));
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
