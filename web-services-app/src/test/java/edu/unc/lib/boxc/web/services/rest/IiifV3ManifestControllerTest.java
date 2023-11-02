package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.web.common.services.AccessCopiesService;
import edu.unc.lib.boxc.web.services.processing.IiifV3ManifestService;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author bbpennel
 */
public class IiifV3ManifestControllerTest {
    private static final String IIIF_BASE = "http://example.com/iiif/v3/";
    private static final String SERVICES_BASE = "http://example.com/services/";
    private static final String ACCESS_BASE = "http://example.com/";

    private static final String WORK_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final PID WORK_PID = PIDs.get(WORK_ID);
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");

    @InjectMocks
    private IiifV3ManifestController manifestController;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private AccessCopiesService accessCopiesService;

    private IiifV3ManifestService manifestService;

    private ContentObjectSolrRecord workObj;

    private MockMvc mockMvc;

    private AutoCloseable closeable;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        manifestService = new IiifV3ManifestService();
        manifestService.setAccessCopiesService(accessCopiesService);
        manifestService.setBaseIiifv3Path(IIIF_BASE);
        manifestService.setBaseServicesPath(SERVICES_BASE);
        manifestService.setBaseAccessPath(ACCESS_BASE);
        manifestController.setManifestService(manifestService);
        mockMvc = MockMvcBuilders.standaloneSetup(manifestController)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);

        workObj = new ContentObjectSolrRecord();
        workObj.setId(WORK_ID);
        workObj.setResourceType(ResourceType.Work.name());
        workObj.setTitle("Test Work");
    }

    @Test
    public void testGetManifest() throws Exception {
        when(accessCopiesService.listViewableFiles(eq(WORK_PID), any())).thenReturn(Arrays.asList(workObj));

        var result = mockMvc.perform(get("/iiif/v3/" + WORK_ID + "/manifest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        System.out.println(respMap);
        assertEquals("Manifest", respMap.get("type"));
        assertEquals("http://example.com/iiif/v3/f277bb38-272c-471c-a28a-9887a1328a1f/manifest", respMap.get("id"));
        assertEquals("Test Work", ((List) ((Map) respMap.get("label")).get("none")).get(0));
        var metadata = (List) respMap.get("metadata");
        assertFalse(metadata.isEmpty());
    }

    @Test
    public void testGetManifestNoAccess() throws Exception {
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(eq(WORK_PID), any(), eq(Permission.viewAccessCopies));
        when(accessCopiesService.listViewableFiles(eq(WORK_PID), any())).thenReturn(Arrays.asList(workObj));

        mockMvc.perform(get("/iiif/v3/" + WORK_ID + "/manifest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
