package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.aspace.RefIdService;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EditRefIdControllerTest {
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    private static final String WORK_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private RefIdService service;
    @InjectMocks
    private EditRefIdController controller;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private WorkObject workObject;
    @Mock
    private Resource resource;
    private MockMvc mockMvc;
    private AutoCloseable closeable;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        service = new RefIdService();
        service.setAclService(accessControlService);
        service.setRepoObjLoader(repositoryObjectLoader);
        service.setRepositoryObjectFactory(repositoryObjectFactory);
        controller.setService(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
        when(repositoryObjectLoader.getRepositoryObject(eq(PIDs.get(WORK_ID)))).thenReturn(workObject);
        when(workObject.getResource()).thenReturn(resource);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testEditRefId() throws Exception {
        var refId = "2817ec3c77e5ea9846d5c070d58d402b";
        mockMvc.perform(post("/edit/aspace/updateRefId/{pid}", WORK_ID)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("aspaceRefId", refId))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    public void testEditRefIdNoAccess() throws Exception {
        var refId = "2817ec3c77e5ea9846d5c070d58d402b";
        var pid = PIDs.get(WORK_ID);
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSetImpl.class), eq(Permission.editAspaceProperties));
        mockMvc.perform(post("/edit/aspace/updateRefId/{pid}", WORK_ID)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("aspaceRefId", refId))
                .andExpect(status().isForbidden())
                .andReturn();
    }
}
