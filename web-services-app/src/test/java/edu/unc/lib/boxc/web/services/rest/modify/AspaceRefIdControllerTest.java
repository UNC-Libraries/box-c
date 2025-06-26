package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.aspace.RefIdService;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequest;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequestSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.web.services.processing.BulkRefIdCsvExporter;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getEmail;
import static edu.unc.lib.boxc.web.services.processing.BulkRefIdCsvExporter.EXPORT_CSV_HEADERS;
import static edu.unc.lib.boxc.web.services.processing.BulkRefIdCsvExporter.HOOK_ID_HEADER;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.PID_HEADER;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.REF_ID_HEADER;
import static edu.unc.lib.boxc.web.services.processing.BulkRefIdCsvExporter.TITLE_HEADER;
import static edu.unc.lib.boxc.web.services.rest.MvcTestHelpers.parseCsvResponse;
import static edu.unc.lib.boxc.web.services.rest.modify.AspaceRefIdController.IMPORT_CSV_HEADERS;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.createCsvPrinter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AspaceRefIdControllerTest {
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    private static final String WORK1_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String WORK2_ID = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    private static final String REF1_ID = "2817ec3c77e5ea9846d5c070d58d402b";
    private static final String REF2_ID = "1651ewt75rgs1517g4re2rte16874se";
    private static final String HOOK_ID = "Hook ID 1";
    private static final String TITLE = "Title 1";
    private RefIdService service;
    private MockMvc mockMvc;
    private Path csvPath;
    private Path csvExportPath;
    private String email;
    private AgentPrincipals agent;
    private AutoCloseable closeable;
    @InjectMocks
    private AspaceRefIdController controller;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private IndexingMessageSender indexingMessageSender;
    @Mock
    private BulkRefIdCsvExporter exporter;
    @Mock
    private WorkObject workObject;
    @Mock
    private Resource resource;
    @TempDir
    public Path tmpFolder;
    @Mock
    private BulkRefIdRequestSender sender;
    @Captor
    private ArgumentCaptor<BulkRefIdRequest> requestCaptor;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        service = new RefIdService();
        service.setAclService(accessControlService);
        service.setRepoObjLoader(repositoryObjectLoader);
        service.setRepositoryObjectFactory(repositoryObjectFactory);
        service.setIndexingMessageSender(indexingMessageSender);
        controller.setService(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
        csvPath = tmpFolder.resolve("bulkRefIdImport");
        csvExportPath = tmpFolder.resolve("bulkRefIdExport");
        agent = getAgentPrincipals();
        email = getEmail();
        when(repositoryObjectLoader.getRepositoryObject(eq(PIDs.get(WORK1_ID)))).thenReturn(workObject);
        when(workObject.getResource()).thenReturn(resource);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testEditRefId() throws Exception {
        var refId = "2817ec3c77e5ea9846d5c070d58d402b";
        mockMvc.perform(post("/edit/aspace/updateRefId/{pid}", WORK1_ID)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("aspaceRefId", refId))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    public void testEditRefIdNoAccess() throws Exception {
        var refId = "2817ec3c77e5ea9846d5c070d58d402b";
        var pid = PIDs.get(WORK1_ID);
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSetImpl.class), eq(Permission.editAspaceProperties));
        mockMvc.perform(post("/edit/aspace/updateRefId/{pid}", WORK1_ID)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("aspaceRefId", refId))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void importRefIdsSuccess() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put(WORK1_ID, REF1_ID);
        map.put(WORK2_ID, REF2_ID);

        createImportCsv();
        var file = mockCsvRequestBody(csvPath);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/edit/aspace/updateRefIds/")
                        .file(file))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(sender).sendToQueue(requestCaptor.capture());
        var request = requestCaptor.getValue();
        assertEquals(map, request.getRefIdMap());
        assertEquals(agent, request.getAgent());
        assertEquals(email, request.getEmail());
    }

    @Test
    public void importRefIdsSuccessWithExportCsv() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put(WORK1_ID, REF1_ID);
        createExportCsv(false);
        var file = mockCsvRequestBody(csvExportPath);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/edit/aspace/updateRefIds/")
                        .file(file))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(sender).sendToQueue(requestCaptor.capture());
        var request = requestCaptor.getValue();
        assertEquals(map, request.getRefIdMap());
        assertEquals(agent, request.getAgent());
        assertEquals(email, request.getEmail());
    }

    @Test
    public void importRefIdsError() throws Exception {
        createImportCsv();
        var file = mockCsvRequestBody(csvPath);

        doThrow(IOException.class).when(sender).sendToQueue(any());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/edit/aspace/updateRefIds/")
                        .file(file))
                .andExpect(status().isInternalServerError())
                .andReturn();
    }

    @Test
    public void exportRefIdsSuccess() throws Exception {
        createExportCsv(true);
        when(exporter.export(any(), any())).thenReturn(csvExportPath);

        MvcResult result = mockMvc.perform(get("/edit/aspace/exportRefIds/{pid}", WORK1_ID))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        List<CSVRecord> csvList = parseCsvResponse(response, EXPORT_CSV_HEADERS);

        assertEquals(2, csvList.size(), "Unexpected number of results");
        var csvRecord1 = csvList.get(0);
        assertEquals(WORK1_ID, csvRecord1.get(PID_HEADER));
        assertEquals(REF1_ID, csvRecord1.get(REF_ID_HEADER));
        assertEquals(HOOK_ID, csvRecord1.get(HOOK_ID_HEADER));
        assertEquals(TITLE, csvRecord1.get(TITLE_HEADER));

        var csvRecord2 = csvList.get(1);
        assertEquals(WORK2_ID, csvRecord2.get(PID_HEADER));
        assertEquals("", csvRecord2.get(REF_ID_HEADER));
        assertEquals("", csvRecord2.get(HOOK_ID_HEADER));
        assertEquals("No Ref ID here", csvRecord2.get(TITLE_HEADER));

    }

    @Test
    public void exportRefIdsNoPermission() throws Exception {
        doThrow(new AccessRestrictionException()).when(exporter).export(any(), any());

        mockMvc.perform(get("/edit/aspace/exportRefIds/{pid}", WORK1_ID))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void exportRefIdsError() throws Exception {
        doThrow(new RepositoryException("Failed export")).when(exporter).export(any(), any());

        mockMvc.perform(get("/edit/aspace/exportRefIds/{pid}", WORK1_ID))
                .andExpect(status().isInternalServerError())
                .andReturn();
    }

    private void createImportCsv() throws IOException {
        try (var csvPrinter = createCsvPrinter(IMPORT_CSV_HEADERS, csvPath)) {
            csvPrinter.printRecord(WORK1_ID, REF1_ID);
            csvPrinter.printRecord(WORK2_ID, REF2_ID);
        }
    }

    private void createExportCsv(boolean forExportOnly) throws IOException {
        try (var csvPrinter = createCsvPrinter(EXPORT_CSV_HEADERS, csvExportPath)) {
            csvPrinter.printRecord(WORK1_ID, REF1_ID, HOOK_ID, TITLE);
            if (forExportOnly) {
                // TODO BXC-4982
                // this is temporary only until we account for imports not updating to blank Ref IDs
                csvPrinter.printRecord(WORK2_ID, "", "", "No Ref ID here");
            }
        }
    }

    private MockMultipartFile mockCsvRequestBody(Path csvPath) throws Exception {
        var inputStream = Files.newInputStream(csvPath);
        return new MockMultipartFile("file", inputStream);
    }
}
