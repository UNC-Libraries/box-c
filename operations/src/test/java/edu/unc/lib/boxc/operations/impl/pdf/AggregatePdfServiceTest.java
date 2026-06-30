package edu.unc.lib.boxc.operations.impl.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequest;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.models.DatastreamImpl;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import pdf4u.CLIMain;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.RESULT_HANDWRITTEN_PRINT;

public class AggregatePdfServiceTest {
    private static final String PARENT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String COLLECTION_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";
    private static final String ADMIN_UNIT_UUID = "5158b962-9e59-4ed8-b920-fc948213efd3";

    @Captor
    private ArgumentCaptor<String[]> captor;
    @Mock
    private MachineGeneratedContentService mgContentService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private SolrSearchService solrSearchService;

    private AgentPrincipals agent;
    private AutoCloseable closeable;
    private AggregatePdfService pdfService;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);

        agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));

        pdfService = new AggregatePdfService();
        pdfService.setMachineGeneratedContentService(mgContentService);
        pdfService.setRepositoryObjectLoader(repositoryObjectLoader);
        pdfService.setSolrSearchService(solrSearchService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void generateAggregatePdfTest() throws Exception {
        try (MockedStatic<CLIMain> mockedStatic = Mockito.mockStatic(CLIMain.class)) {
            var parentRec = makeWorkRecord(PARENT_UUID, "Work");
            var rec1 = makeRecord(CHILD1_UUID, PARENT_UUID, ResourceType.File, "File One",
                    "file1.png", "image/png");
            var rec2 = makeRecord(CHILD2_UUID, PARENT_UUID, ResourceType.File, "File Two",
                    "file2.png", "image/png");

            mockParentResults(parentRec);
            mockChildrenResults(rec1, rec2);
            mockOriginalFile(CHILD1_UUID, "file1.png");
            mockOriginalFile(CHILD2_UUID, "file2.png");

            String defaultJson1 = loadDefaultJson();
            JsonNode defaultNode1 = MachineGeneratedContentService.MAPPER.readTree(defaultJson1);
            when(mgContentService.loadMachineGeneratedDescription(PIDs.get(CHILD1_UUID))).thenReturn(defaultJson1);
            when(mgContentService.deserializeMachineGeneratedDescription(defaultJson1)).thenReturn(defaultNode1);
            when(mgContentService.extractTextType(defaultNode1)).thenReturn(RESULT_HANDWRITTEN_PRINT);

            String defaultJson2 = loadDefaultJson();
            JsonNode defaultNode2 = MachineGeneratedContentService.MAPPER.readTree(defaultJson2);
            when(mgContentService.loadMachineGeneratedDescription(PIDs.get(CHILD2_UUID))).thenReturn(defaultJson2);
            when(mgContentService.deserializeMachineGeneratedDescription(defaultJson2)).thenReturn(defaultNode2);
            when(mgContentService.extractTextType(defaultNode2)).thenReturn(RESULT_HANDWRITTEN_PRINT);

            var workObject = mock(WorkObject.class);
            when(repositoryObjectLoader.getWorkObject(PIDs.get(PARENT_UUID))).thenReturn(workObject);

            PdfRequest request = new PdfRequest();
            request.setWorkPid(PARENT_UUID);
            request.setMimetype("image/png");
            request.setAgent(agent);

            mockedStatic.when(() -> CLIMain.runCommand(any(String[].class))).thenReturn(0);

            Path result = pdfService.generateAggregatePdf(request);

            assertNotNull(result);
            assertTrue(result.toString().endsWith(".pdf"));

            mockedStatic.verify(() -> CLIMain.runCommand(captor.capture()), times(1));
            var cmd = Arrays.stream(captor.getValue()).toList();
            assertNotNull(cmd);
            assertTrue(cmd.contains("pdf4u"));
            assertTrue(FilenameUtils.getBaseName(cmd.get(3)).startsWith(PARENT_UUID));
            assertEquals(RESULT_HANDWRITTEN_PRINT + "," + RESULT_HANDWRITTEN_PRINT, cmd.get(9));
        }
    }

    @Test
    public void createInputListFileTest() throws Exception {
        var parentRec = makeWorkRecord(PARENT_UUID, "Work");
        var rec1 = makeRecord(CHILD1_UUID, PARENT_UUID, ResourceType.File, "File One",
                "file1.png", "image/png");
        var rec2 = makeRecord(CHILD2_UUID, PARENT_UUID, ResourceType.File, "File Two",
                "file2.png", "image/png");

        mockParentResults(parentRec);
        mockChildrenResults(rec1, rec2);
        mockOriginalFile(CHILD1_UUID, "file1.png");
        mockOriginalFile(CHILD2_UUID, "file2.png");

        PdfRequest request = new PdfRequest();
        request.setWorkPid(PARENT_UUID);
        request.setMimetype("image/png");
        request.setAgent(agent);

        var inputFilePath = pdfService.createInputListFile(request);
        List<String> lines = Files.readAllLines(inputFilePath, StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertEquals("file1.png", lines.get(0));
        assertEquals("file2.png", lines.get(1));
    }

    @Test
    public void createTranscriptListFileTest() throws Exception {
        var parentRec = makeWorkRecord(PARENT_UUID, "Work");
        var rec1 = makeRecord(CHILD1_UUID, PARENT_UUID, ResourceType.File, "File One",
                "file1.png", "image/png");
        var rec2 = makeRecord(CHILD2_UUID, PARENT_UUID, ResourceType.File, "File Two",
                "file2.png", "image/png");

        mockParentResults(parentRec);
        mockChildrenResults(rec1, rec2);
        mockOriginalFile(CHILD1_UUID, "file1.png");
        mockOriginalFile(CHILD2_UUID, "file2.png");

        PdfRequest request = new PdfRequest();
        request.setWorkPid(PARENT_UUID);
        request.setMimetype("image/png");
        request.setAgent(agent);

        var transcriptListFile = pdfService.createTranscriptListFile(request);
        List<String> lines = Files.readAllLines(transcriptListFile, StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains(CHILD1_UUID + "_transcript"));
        assertTrue(lines.get(1).contains(CHILD2_UUID + "_transcript"));
    }

    @Test
    public void getTextTypeTest() throws Exception {
        var parentRec = makeWorkRecord(PARENT_UUID, "Work");
        var rec1 = makeRecord(CHILD1_UUID, PARENT_UUID, ResourceType.File, "File One",
                "file1.png", "image/png");
        var rec2 = makeRecord(CHILD2_UUID, PARENT_UUID, ResourceType.File, "File Two",
                "file2.png", "image/png");

        mockParentResults(parentRec);
        mockChildrenResults(rec1, rec2);
        mockOriginalFile(CHILD1_UUID, "photo.jpg");
        mockOriginalFile(CHILD2_UUID, "file2.png");

        String defaultJson1 = loadDefaultJson();
        JsonNode defaultNode1 = MachineGeneratedContentService.MAPPER.readTree(defaultJson1);
        when(mgContentService.loadMachineGeneratedDescription(PIDs.get(CHILD1_UUID))).thenReturn(defaultJson1);
        when(mgContentService.deserializeMachineGeneratedDescription(defaultJson1)).thenReturn(defaultNode1);
        when(mgContentService.extractTextType(defaultNode1)).thenReturn(RESULT_HANDWRITTEN_PRINT);

        String defaultJson2 = loadDefaultJson();
        JsonNode defaultNode2 = MachineGeneratedContentService.MAPPER.readTree(defaultJson2);
        when(mgContentService.loadMachineGeneratedDescription(PIDs.get(CHILD2_UUID))).thenReturn(defaultJson2);
        when(mgContentService.deserializeMachineGeneratedDescription(defaultJson2)).thenReturn(defaultNode2);
        when(mgContentService.extractTextType(defaultNode2)).thenReturn(RESULT_HANDWRITTEN_PRINT);

        PdfRequest request = new PdfRequest();
        request.setWorkPid(PARENT_UUID);
        request.setMimetype("image/png");
        request.setAgent(agent);

        var textType = pdfService.createTextTypeList(request);
        assertEquals(List.of(RESULT_HANDWRITTEN_PRINT, RESULT_HANDWRITTEN_PRINT), textType);
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
        return makeRecord(uuid, COLLECTION_UUID, ResourceType.Work, title, null, "image/png");
    }

    private ContentObjectRecord makeRecord(String uuid, String parentUuid, ResourceType resourceType, String title,
                                           String filename, String mimetype) {
        var rec = new ContentObjectSolrRecord();
        rec.setId(uuid);
        rec.setAncestorPath(makeAncestorPath(parentUuid));
        rec.setResourceType(resourceType.name());
        rec.setTitle(title);
        rec.setFileFormatType(Arrays.asList(mimetype));
        rec.setTranscript("Transcript for " + title);

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

    private void mockOriginalFile(String childUuid, String contentUri) {
        var childPid = PIDs.get(childUuid);
        var originalFilePid = DatastreamPids.getOriginalFilePid(childPid);

        var binaryObject = mock(BinaryObject.class);
        when(binaryObject.getContentUri()).thenReturn(URI.create(contentUri));

        when(repositoryObjectLoader.getBinaryObject(originalFilePid)).thenReturn(binaryObject);
    }

    private String loadDefaultJson() throws Exception {
        return Files.readString(
                Path.of("src/test/resources/machineGeneratedDescriptionDefaults.json"));
    }
}
