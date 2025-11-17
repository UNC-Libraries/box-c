package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.collectionDisplay.CollectionDisplayPropertiesRequestSender;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CollectionDisplayPropertiesIT {
    @InjectMocks
    private EditCollectionDisplayController controller;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private CollectionDisplayPropertiesRequestSender collectionDisplayPropertiesRequestSender;
    @Mock
    private CollectionObject collectionObject;
    @Mock
    private WorkObject workObject;
    @Mock
    private Resource resource;
    @Mock
    private Statement collectionDisplayStatement;
    private MockMvc mockMvc;
    private AutoCloseable closeable;
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    private static final String COLLECTION_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String WORK_ID =  "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    private static final PID COLLECTION_PID = PIDs.get(COLLECTION_ID);
    private static final String DISPLAY_PROPERTIES = "{\"displayType\":\"gallery\",\"sortType\":\"title,reverse\",\"worksOnly\":true}";
    private static final String SORT_TYPE = "title,reverse";

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
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
    public void testGetStreamingPropertiesSuccess() throws Exception {
        when(repositoryObjectLoader.getRepositoryObject(eq(COLLECTION_PID))).thenReturn(collectionObject);
        when(collectionObject.getResource()).thenReturn(resource);
        when(resource.getProperty(eq(Cdr.collectionDefaultDisplaySettings))).thenReturn(collectionDisplayStatement);
        when(collectionDisplayStatement.getString()).thenReturn(DISPLAY_PROPERTIES);

        var result = mockMvc.perform(get("/edit/collectionDisplay/" + COLLECTION_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(DISPLAY_PROPERTIES, respMap.get("collectionDefaultDisplaySettings"));
    }

    @Test
    public void testGetCollectionDisplayPropertiesNoPermission() throws Exception {
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(COLLECTION_PID), any(), eq(Permission.viewHidden));

        mockMvc.perform(get("/edit/collectionDisplay/" + COLLECTION_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetCollectionDisplayPropertiesNotACollectionObject() throws Exception {
        when(repositoryObjectLoader.getRepositoryObject(eq(PIDs.get(WORK_ID)))).thenReturn(workObject);
        mockMvc.perform(get("/edit/collectionDisplay/" + WORK_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateCollectionDisplayPropertiesNoPermission() throws Exception {
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), eq(COLLECTION_PID), any(), eq(Permission.ingest));
        mockMvc.perform(put("/edit/collectionDisplay?id=" + COLLECTION_ID +
                        "&sortType=" + SORT_TYPE + "&worksOnly=false&displayType=list-display")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testAddCollectionDisplayPropertiesSuccess() throws Exception {
        var result = mockMvc.perform(put("/edit/collectionDisplay?id=" + COLLECTION_ID +
                        "&sortType=" + SORT_TYPE + "&worksOnly=false&displayType=list-display")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("Submitted collection display properties updates for " + COLLECTION_ID, respMap.get("status"));
    }
}
