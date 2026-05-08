package edu.unc.lib.boxc.operations.impl.pdf;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequest;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.models.DatastreamImpl;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import pdf4u.CLIMain;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class PdfDerivativeServiceTest {
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String COLLECTION_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";
    private static final String ADMIN_UNIT_UUID = "5158b962-9e59-4ed8-b920-fc948213efd3";

    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private SolrSearchService solrSearchService;

    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private AutoCloseable closeable;
    private PdfDerivativeService pdfService;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        pdfService = new PdfDerivativeService();
        pdfService.setRepositoryObjectLoader(repositoryObjectLoader);
        pdfService.setSolrSearchService(solrSearchService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void generatePdfDerivativeTest() throws Exception {
        try (MockedStatic<CLIMain> mockedStatic = Mockito.mockStatic(CLIMain.class)) {
            var pid = makePid();
            var pidString = pid.getId();
            var parentRec = makeWorkRecord(pidString, "Work");
            var rec1 = makeRecord(CHILD1_UUID, pidString, ResourceType.File, "File One",
                    "file1.txt", "image/png", null);
            var rec2 = makeRecord(CHILD2_UUID, pidString, ResourceType.File, "File Two",
                    "file2.png", "image/png", null);
            mockParentResults(parentRec);
            mockChildrenResults(rec1, rec2);

            PdfRequest request = new PdfRequest();
            request.setWorkPid(pid);
            request.setMimetype("image/png");
            request.setAgent(agent);

            mockedStatic.when(() -> CLIMain.runCommand(any(String[].class))).thenReturn(0);

            Path result = pdfService.generatePdfDerivative(request);

            assertNotNull(result);
            assertTrue(result.toString().endsWith(".pdf"));

            mockedStatic.verify(() -> CLIMain.runCommand(argThat(command ->
                    command != null
                            && command.length == 10
                            && "pdf4u".equals(command[0])
                            && "add_ocr".equals(command[1])
                            && "-i".equals(command[2])
                            && "-o".equals(command[4])
                            && "-t".equals(command[6])
                            && "-tt".equals(command[8])
                            && "HANDWRITTEN-PRINT".equals(command[9])
            )));
        }
    }

    @Test
    public void getInputFilePathTest() throws Exception {
        var pid = makePid();
        var pidString = pid.getId();
        var parentRec = makeWorkRecord(pidString, "Work");
        var rec1 = makeRecord(CHILD1_UUID, pidString, ResourceType.File, "File One",
                "file1.png", "image/png", null);
        var rec2 = makeRecord(CHILD2_UUID, pidString, ResourceType.File, "File Two",
                "file2.png", "image/png", null);
        mockParentResults(parentRec);
        mockChildrenResults(rec1, rec2);

        PdfRequest request = new PdfRequest();
        request.setWorkPid(pid);
        request.setMimetype("image/png");
        request.setAgent(agent);

        var inputFilePath = pdfService.getInputFiles(request);
        List<String> lines = Files.readAllLines(Path.of(inputFilePath), StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertEquals("file1.png", lines.get(0));
        assertEquals("file2.png", lines.get(1));
    }

    @Test
    public void getTranscriptFilePathTest() throws Exception {
        // todo
    }

    @Test
    public void getTextTypeTest() throws Exception {
        // todo
    }

    public static SearchResultResponse makeResultResponse(ContentObjectRecord... results) {
        var resp = new SearchResultResponse();
        resp.setResultList(Arrays.asList(results));
        resp.setResultCount(results.length);
        return resp;
    }

    public static void mockSingleRecordResults(SolrSearchService solrSearchService, ContentObjectRecord parentRec, ContentObjectRecord... parentRecs) {
        when(solrSearchService.getObjectById(any())).thenReturn(parentRec, parentRecs);
    }

    public static void mockSearchResults(SolrSearchService solrSearchService, ContentObjectRecord... results) {
        when(solrSearchService.getSearchResults(any())).thenReturn(makeResultResponse(results));
    }

    private void mockChildrenResults(ContentObjectRecord... results) {
        mockSearchResults(solrSearchService, results);
    }

    private void mockParentResults(ContentObjectRecord parentRec, ContentObjectRecord... parentRecs) {
        mockSingleRecordResults(solrSearchService, parentRec, parentRecs);
    }

    private ContentObjectRecord makeWorkRecord(String uuid, String title) {
        return makeRecord(uuid, COLLECTION_UUID, ResourceType.Work, title, null, "image/png", null);
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
