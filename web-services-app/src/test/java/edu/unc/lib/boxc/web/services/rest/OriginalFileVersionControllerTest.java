package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.OriginalFileVersionService;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.fcrepo.client.FcrepoClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OriginalFileVersionControllerTest {
    private static final String OBJECT_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final PID OBJECT_PID = PIDs.get(OBJECT_ID);
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    private OriginalFileVersionService service;
    private MockMvc mockMvc;
    private AutoCloseable closeable;
    @InjectMocks
    private OriginalFileVersionsController versionsController;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private FcrepoClient fcrepoClient;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        service = new OriginalFileVersionService();
        service.setFcrepoClient(fcrepoClient);
        versionsController.setAccessControlService(accessControlService);
        versionsController.setService(service);
        mockMvc = MockMvcBuilders.standaloneSetup(versionsController)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void noPermissionTest() throws Exception {
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(eq(OBJECT_PID), any(), eq(Permission.viewMetadata));

        mockMvc.perform(get("/version/" + OBJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

//    @Test
//    public void successTest() throws Exception {
//        var result = mockMvc.perform(get("/version/" + OBJECT_ID)
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andReturn();
//    }
}
