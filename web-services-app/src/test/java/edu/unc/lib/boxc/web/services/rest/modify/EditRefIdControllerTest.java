package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.aspace.RefIdService;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequest;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequestSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getEmail;
import static edu.unc.lib.boxc.web.services.rest.modify.EditRefIdController.CSV_HEADERS;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.createCsvPrinter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EditRefIdControllerTest {
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    private static final String WORK1_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String WORK2_ID = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    private static final String REF1_ID = "2817ec3c77e5ea9846d5c070d58d402b";
    private static final String REF2_ID = "1651ewt75rgs1517g4re2rte16874se";
    private RefIdService service;
    private MockMvc mockMvc;
    private Path csvPath;
    private String email;
    private AgentPrincipals agent;
    private AutoCloseable closeable;
    @InjectMocks
    private EditRefIdController controller;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private IndexingMessageSender indexingMessageSender;
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
        csvPath = tmpFolder.resolve("bulkRefId");
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

        createCsv();
        var file = mockCsvRequestBody();

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
        createCsv();
        var file = mockCsvRequestBody();

        doThrow(IOException.class).when(sender).sendToQueue(any());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/edit/aspace/updateRefIds/")
                        .file(file))
                .andExpect(status().isInternalServerError())
                .andReturn();
    }

    private void createCsv() throws IOException {
        try (var csvPrinter = createCsvPrinter(CSV_HEADERS, csvPath)) {
            csvPrinter.printRecord(WORK1_ID, REF1_ID);
            csvPrinter.printRecord(WORK2_ID, REF2_ID);
        }
    }

    private MockMultipartFile mockCsvRequestBody() throws Exception {
        var inputStream = Files.newInputStream(csvPath);
        return new MockMultipartFile("file", inputStream);
    }
}
