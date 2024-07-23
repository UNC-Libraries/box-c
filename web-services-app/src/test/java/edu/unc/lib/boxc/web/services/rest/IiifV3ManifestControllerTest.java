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
import edu.unc.lib.boxc.search.solr.models.DatastreamImpl;
import edu.unc.lib.boxc.web.common.services.AccessCopiesService;
import edu.unc.lib.boxc.web.services.processing.IiifV3ManifestService;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
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

    private static final String OBJECT_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final PID OBJECT_PID = PIDs.get(OBJECT_ID);
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");

    @InjectMocks
    private IiifV3ManifestController manifestController;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private AccessCopiesService accessCopiesService;

    private IiifV3ManifestService manifestService;

    private MockMvc mockMvc;

    private AutoCloseable closeable;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        manifestService = new IiifV3ManifestService();
        manifestService.setAccessCopiesService(accessCopiesService);
        manifestService.setAccessControlService(accessControlService);
        manifestService.setBaseIiifv3Path(IIIF_BASE);
        manifestService.setBaseServicesApiPath(SERVICES_BASE);
        manifestService.setBaseAccessPath(ACCESS_BASE);
        manifestController.setManifestService(manifestService);
        mockMvc = MockMvcBuilders.standaloneSetup(manifestController)
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
    public void testGetManifest() throws Exception {
        var workObj = new ContentObjectSolrRecord();
        workObj.setId(OBJECT_ID);
        workObj.setResourceType(ResourceType.Work.name());
        workObj.setTitle("Test Work");
        when(accessCopiesService.listViewableFiles(eq(OBJECT_PID), any())).thenReturn(Arrays.asList(workObj));

        var result = mockMvc.perform(get("/iiif/v3/" + OBJECT_ID + "/manifest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("Manifest", respMap.get("type"));
        assertEquals("http://example.com/iiif/v3/f277bb38-272c-471c-a28a-9887a1328a1f/manifest", respMap.get("id"));
        assertEquals("Test Work", ((List) ((Map) respMap.get("label")).get("none")).get(0));
        var metadata = (List) respMap.get("metadata");
        assertFalse(metadata.isEmpty());
    }

    @Test
    public void testGetManifestNoAccess() throws Exception {
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(eq(OBJECT_PID), any(), eq(Permission.viewAccessCopies));

        mockMvc.perform(get("/iiif/v3/" + OBJECT_ID + "/manifest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetCanvas() throws Exception {
        var fileObj = new ContentObjectSolrRecord();
        fileObj.setId(OBJECT_ID);
        fileObj.setResourceType(ResourceType.File.name());
        fileObj.setTitle("File Object");
        var originalDs = new DatastreamImpl("original_file|image/jpeg|image.jpg|jpg|0|||240x750x");
        var jp2Ds = new DatastreamImpl("jp2|image/jp2|image.jp2|jp2|0|||");
        fileObj.setDatastream(Arrays.asList(originalDs.toString(), jp2Ds.toString()));
        when(accessCopiesService.listViewableFiles(eq(OBJECT_PID), any())).thenReturn(Arrays.asList(fileObj));

        var result = mockMvc.perform(get("/iiif/v3/" + OBJECT_ID + "/canvas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("Canvas", respMap.get("type"));
        assertEquals("http://example.com/iiif/v3/f277bb38-272c-471c-a28a-9887a1328a1f/canvas", respMap.get("id"));
        assertEquals(750, respMap.get("width"));
        var items = (List) respMap.get("items");
        assertFalse(items.isEmpty());
    }

    @Test
    public void testGetManifestWithAVFiles() throws Exception {
        var workObj = new ContentObjectSolrRecord();
        workObj.setId(OBJECT_ID);
        workObj.setResourceType(ResourceType.Work.name());
        workObj.setTitle("Test Work");
        when(accessCopiesService.listViewableFiles(eq(OBJECT_PID), any())).thenReturn(Arrays.asList(workObj));

        var result = mockMvc.perform(get("/iiif/v3/" + OBJECT_ID + "/manifest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("Manifest", respMap.get("type"));
        assertEquals("http://example.com/iiif/v3/f277bb38-272c-471c-a28a-9887a1328a1f/manifest", respMap.get("id"));
        assertEquals("Test Work", ((List) ((Map) respMap.get("label")).get("none")).get(0));
        var metadata = (List) respMap.get("metadata");
        assertFalse(metadata.isEmpty());
    }

    @Test
    public void testGetCanvasWithVideoFile() throws Exception {
        var fileObj = new ContentObjectSolrRecord();
        fileObj.setId(OBJECT_ID);
        fileObj.setResourceType(ResourceType.File.name());
        fileObj.setTitle("File Object");
        var originalDs = new DatastreamImpl("original_file|video/mp4|video.mp4|mp4|0|||240x750x500");
        fileObj.setDatastream(List.of(originalDs.toString()));
        when(accessCopiesService.listViewableFiles(eq(OBJECT_PID), any())).thenReturn(List.of(fileObj));

        var result = mockMvc.perform(get("/iiif/v3/" + OBJECT_ID + "/canvas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        var respJson = MvcTestHelpers.getResponseAsJson(result);
        var body = respJson.get("items").get(0).get("items").get(0).get("body");
        assertEquals(SERVICES_BASE + "file/" + OBJECT_ID, body.get("id").textValue());
        assertEquals("video/mp4", body.get("format").textValue());
        assertEquals("Video", body.get("type").textValue());
        assertEquals(750, body.get("width").intValue());
        assertEquals(240, body.get("height").intValue());
        assertEquals(500, body.get("duration").intValue());
    }

    @Test
    public void testGetCanvasWithAudioFile() throws Exception {
        var fileObj = new ContentObjectSolrRecord();
        fileObj.setId(OBJECT_ID);
        fileObj.setResourceType(ResourceType.File.name());
        fileObj.setTitle("File Object");
        var originalDs = new DatastreamImpl("original_file|audio/mp4|sound.mp3|mp3|0|||xx500");
        fileObj.setDatastream(List.of(originalDs.toString()));
        when(accessCopiesService.listViewableFiles(eq(OBJECT_PID), any())).thenReturn(List.of(fileObj));

        var result = mockMvc.perform(get("/iiif/v3/" + OBJECT_ID + "/canvas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        var respJson = MvcTestHelpers.getResponseAsJson(result);
        var body = respJson.get("items").get(0).get("items").get(0).get("body");
        assertEquals(SERVICES_BASE + "file/" + OBJECT_ID, body.get("id").textValue());
        assertEquals("Sound", body.get("type").textValue());
        assertEquals(500, body.get("duration").intValue());
    }
}
