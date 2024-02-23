package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.CdrView;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.viewSettings.ViewSettingRequestSender;
import edu.unc.lib.boxc.web.services.rest.MvcTestHelpers;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ViewSettingIT {
    @InjectMocks
    private ViewSettingController controller;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private ViewSettingRequestSender viewSettingRequestSender;
    @Mock
    private FileObject fileObject;
    @Mock
    private WorkObject workObject;
    @Mock
    private WorkObject workObject2;
    @Mock
    private Resource resource;
    @Mock
    private Statement stmt;
    private MockMvc mockMvc;
    private AutoCloseable closeable;
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    private static final String OBJECT_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final PID OBJECT_PID = PIDs.get(OBJECT_ID);

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
        when(repositoryObjectLoader.getRepositoryObject(eq(PIDs.get(OBJECT_ID)))).thenReturn(workObject);
        when(workObject.getResource()).thenReturn(resource);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetViewSetting() throws Exception {
        var paged = "paged";
        when(resource.getProperty(eq(CdrView.viewBehavior))).thenReturn(stmt);
        when(stmt.getString()).thenReturn(paged);

        var result = mockMvc.perform(get("/edit/viewSettings/" + OBJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(paged, respMap.get("viewBehavior"));
    }

    @Test
    public void testGetViewSettingNoPermission() throws Exception {
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(OBJECT_PID), any(), eq(Permission.viewHidden));

        mockMvc.perform(get("/edit/viewSettings/" + OBJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetViewSettingWithNullViewBehavior() throws Exception {
        var result = mockMvc.perform(get("/edit/viewSettings/" + OBJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().is2xxSuccessful())
                        .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertNull(respMap.get("viewBehavior"));
    }

    @Test
    public void testGetViewSettingNotAWork() throws Exception {
        var fileId = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
        when(repositoryObjectLoader.getRepositoryObject(eq(PIDs.get(fileId)))).thenReturn(fileObject);

        mockMvc.perform(get("/edit/viewSettings/" + fileId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateViewSettingSingleObject() throws Exception {
        var result = mockMvc.perform(put("/edit/viewSettings?targets=" +
                        OBJECT_ID + "&behavior=continuous"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("Submitted view setting updates for 1 object(s)", respMap.get("status"));
    }

    @Test
    public void testUpdateViewSettingMultipleObjects() throws Exception {
        var workId = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
        when(repositoryObjectLoader.getRepositoryObject(eq(PIDs.get(workId)))).thenReturn(workObject2);
        var result = mockMvc.perform(put("/edit/viewSettings?targets=" + OBJECT_ID + "," +
                        workId + "&behavior=continuous"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("Submitted view setting updates for 2 object(s)", respMap.get("status"));
    }
    @Test
    public void testUpdateViewSettingNoPermission() throws Exception {
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(OBJECT_PID), any(), eq(Permission.editViewSettings));
        mockMvc.perform(put("/edit/viewSettings?targets=" + OBJECT_ID + "&behavior=continuous"))
                .andExpect(status().isForbidden());
    }
    @Test
    public void testUpdateViewSettingWithInvalidValue() throws Exception {
        mockMvc.perform(put("/edit/viewSettings?targets=" + OBJECT_ID + "&behavior=good"))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void testUpdateViewSettingNotAWork() throws Exception {
        var fileId = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
        when(repositoryObjectLoader.getRepositoryObject(eq(PIDs.get(fileId)))).thenReturn(fileObject);

        mockMvc.perform(put("/edit/viewSettings?targets=" + fileId + "&behavior=continuous"))
                .andExpect(status().isBadRequest());
    }
}
