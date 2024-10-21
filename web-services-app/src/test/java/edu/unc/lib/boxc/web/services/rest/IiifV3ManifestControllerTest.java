package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.models.DatastreamImpl;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
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
import java.util.List;
import java.util.Map;

import static edu.unc.lib.boxc.web.services.processing.IiifV3ManifestService.DURATION;
import static edu.unc.lib.boxc.web.services.processing.IiifV3ManifestService.HEIGHT;
import static edu.unc.lib.boxc.web.services.processing.IiifV3ManifestService.WIDTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private SolrSearchService solrSearchService;

    @Mock private GlobalPermissionEvaluator globalPermissionEvaluator;

    private IiifV3ManifestService manifestService;

    private MockMvc mockMvc;

    private AutoCloseable closeable;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        manifestService = new IiifV3ManifestService();
        manifestService.setSolrSearchService(solrSearchService);
        manifestService.setAccessControlService(accessControlService);
        manifestService.setBaseIiifv3Path(IIIF_BASE);
        manifestService.setBaseServicesApiPath(SERVICES_BASE);
        manifestService.setBaseAccessPath(ACCESS_BASE);
        manifestService.setGlobalPermissionEvaluator(globalPermissionEvaluator);
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
        when(solrSearchService.getObjectById(any())).thenReturn(workObj);
        when(solrSearchService.getSearchResults(any()))
                .thenReturn(MvcTestHelpers.createSearchResponse(List.of(workObj)));
        when(globalPermissionEvaluator.hasGlobalPrincipal(any())).thenReturn(true);

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
    public void testGetManifestWithAVFiles() throws Exception {
        var workObj = new ContentObjectSolrRecord();
        workObj.setId(OBJECT_ID);
        workObj.setResourceType(ResourceType.Work.name());
        workObj.setTitle("Test Work");

        var fileObj = new ContentObjectSolrRecord();
        fileObj.setId("5d72b84a-983c-4a45-8caa-dc9857987da2");
        fileObj.setResourceType(ResourceType.File.name());
        fileObj.setTitle("File Object");
        var originalDs = new DatastreamImpl("original_file|video/mp4|video.mp4|mp4|0|||240x750x500");
        fileObj.setDatastream(List.of(originalDs.toString()));
        when(solrSearchService.getObjectById(any())).thenReturn(workObj);
        when(globalPermissionEvaluator.hasGlobalPrincipal(any())).thenReturn(true);
        when(solrSearchService.getSearchResults(any()))
                .thenReturn(MvcTestHelpers.createSearchResponse(Arrays.asList(workObj, fileObj)));

        var result = mockMvc.perform(get("/iiif/v3/" + OBJECT_ID + "/manifest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        var respJson = MvcTestHelpers.getResponseAsJson(result);
        assertEquals("Manifest", respJson.get("type").textValue());
        assertEquals("http://example.com/iiif/v3/f277bb38-272c-471c-a28a-9887a1328a1f/manifest", respJson.get("id").textValue());
        assertEquals("Test Work", respJson.get("label").get("none").get(0).textValue());
        assertFalse(respJson.get("metadata").isEmpty());
        assertFalse(respJson.get("items").isEmpty());
    }

    @Test
    public void testGetCanvasWithImageFile() throws Exception {
        var fileObj = new ContentObjectSolrRecord();
        fileObj.setId(OBJECT_ID);
        fileObj.setResourceType(ResourceType.File.name());
        fileObj.setTitle("File Object");
        var originalDs = new DatastreamImpl("original_file|image/jpeg|image.jpg|jpg|0|||240x750");
        var jp2Ds = new DatastreamImpl("jp2|image/jp2|image.jp2|jp2|0|||");
        fileObj.setDatastream(Arrays.asList(originalDs.toString(), jp2Ds.toString()));
        when(solrSearchService.getObjectById(any())).thenReturn(fileObj);
        when(solrSearchService.getSearchResults(any()))
                .thenReturn(MvcTestHelpers.createSearchResponse(List.of(fileObj)));
        when(globalPermissionEvaluator.hasGlobalPrincipal(any())).thenReturn(true);

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
    public void testGetCanvasWithVideoFile() throws Exception {
        var fileObj = new ContentObjectSolrRecord();
        fileObj.setId(OBJECT_ID);
        fileObj.setResourceType(ResourceType.File.name());
        fileObj.setTitle("File Object");
        var originalDs = new DatastreamImpl("original_file|video/mp4|video.mp4|mp4|0|||240x750x500");
        fileObj.setDatastream(List.of(originalDs.toString()));
        when(solrSearchService.getSearchResults(any()))
                .thenReturn(MvcTestHelpers.createSearchResponse(List.of(fileObj)));
        when(solrSearchService.getObjectById(any())).thenReturn(fileObj);
        when(globalPermissionEvaluator.hasGlobalPrincipal(any())).thenReturn(true);

        var result = mockMvc.perform(get("/iiif/v3/" + OBJECT_ID + "/canvas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        var respJson = MvcTestHelpers.getResponseAsJson(result);
        var body = respJson.get("items").get(0).get("items").get(0).get("body");
        assertEquals("http://example.com/services/file/f277bb38-272c-471c-a28a-9887a1328a1f", body.get("id").textValue());
        assertEquals("video/mp4", body.get("format").textValue());
        assertEquals("Video", body.get("type").textValue());
        assertEquals(750, body.get(WIDTH).intValue());
        assertEquals(240, body.get(HEIGHT).intValue());
        assertEquals(500, body.get(DURATION).intValue());
    }

    @Test
    public void testGetCanvasWithAudioFile() throws Exception {
        var fileObj = new ContentObjectSolrRecord();
        fileObj.setId(OBJECT_ID);
        fileObj.setResourceType(ResourceType.File.name());
        fileObj.setTitle("File Object");
        var originalDs = new DatastreamImpl("original_file|audio/mp4|sound.mp3|mp3|0|||xx500");
        fileObj.setDatastream(List.of(originalDs.toString()));
        when(solrSearchService.getSearchResults(any()))
                .thenReturn(MvcTestHelpers.createSearchResponse(List.of(fileObj)));
        when(solrSearchService.getObjectById(any())).thenReturn(fileObj);
        when(globalPermissionEvaluator.hasGlobalPrincipal(any())).thenReturn(true);

        var result = mockMvc.perform(get("/iiif/v3/" + OBJECT_ID + "/canvas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        var respJson = MvcTestHelpers.getResponseAsJson(result);
        var body = respJson.get("items").get(0).get("items").get(0).get("body");
        assertEquals("http://example.com/services/file/f277bb38-272c-471c-a28a-9887a1328a1f", body.get("id").textValue());
        assertEquals("audio/mp4", body.get("format").textValue());
        assertEquals("Sound", body.get("type").textValue());
        assertEquals(500, body.get(DURATION).intValue());
    }

    @Test
    public void testGetCanvasWithNoExtentInformation() throws Exception {
        var fileObj = new ContentObjectSolrRecord();
        fileObj.setId(OBJECT_ID);
        fileObj.setResourceType(ResourceType.File.name());
        fileObj.setTitle("File Object");
        var originalDs = new DatastreamImpl("original_file|video/mp4|video.mp4|mp4|0|||");
        fileObj.setDatastream(List.of(originalDs.toString()));
        when(solrSearchService.getSearchResults(any()))
                .thenReturn(MvcTestHelpers.createSearchResponse(List.of(fileObj)));
        when(globalPermissionEvaluator.hasGlobalPrincipal(any())).thenReturn(true);
        when(solrSearchService.getObjectById(any())).thenReturn(fileObj);

        var result = mockMvc.perform(get("/iiif/v3/" + OBJECT_ID + "/canvas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        var respJson = MvcTestHelpers.getResponseAsJson(result);
        var body = respJson.get("items").get(0).get("items").get(0).get("body");
        assertEquals("http://example.com/services/file/f277bb38-272c-471c-a28a-9887a1328a1f", body.get("id").textValue());
        assertEquals("video/mp4", body.get("format").textValue());
        assertEquals("Video", body.get("type").textValue());
        assertNull(body.get(WIDTH));
        assertNull(body.get(HEIGHT));
        assertNull(body.get(DURATION));
    }

    @Test
    public void testGetCanvasWithNegativeDuration() throws Exception {
        var fileObj = new ContentObjectSolrRecord();
        fileObj.setId(OBJECT_ID);
        fileObj.setResourceType(ResourceType.File.name());
        fileObj.setTitle("File Object");
        var originalDs = new DatastreamImpl("original_file|video/mp4|video.mp4|mp4|0|||240x750x-1");
        fileObj.setDatastream(List.of(originalDs.toString()));
        when(solrSearchService.getSearchResults(any()))
                .thenReturn(MvcTestHelpers.createSearchResponse(List.of(fileObj)));
        when(solrSearchService.getObjectById(any())).thenReturn(fileObj);
        when(globalPermissionEvaluator.hasGlobalPrincipal(any())).thenReturn(true);

        var result = mockMvc.perform(get("/iiif/v3/" + OBJECT_ID + "/canvas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        var respJson = MvcTestHelpers.getResponseAsJson(result);
        var body = respJson.get("items").get(0).get("items").get(0).get("body");
        assertEquals("http://example.com/services/file/f277bb38-272c-471c-a28a-9887a1328a1f", body.get("id").textValue());
        assertEquals("video/mp4", body.get("format").textValue());
        assertEquals("Video", body.get("type").textValue());
        assertEquals(750, body.get(WIDTH).intValue());
        assertEquals(240, body.get(HEIGHT).intValue());
        assertNull(body.get(DURATION));
    }
}
