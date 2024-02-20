package edu.unc.lib.boxc.web.services.rest;

import de.digitalcollections.iiif.model.jackson.IiifObjectMapper;
import de.digitalcollections.iiif.model.openannotation.Annotation;
import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import de.digitalcollections.iiif.model.sharedcanvas.Sequence;
import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.models.DatastreamImpl;
import edu.unc.lib.boxc.web.common.services.AccessCopiesService;
import edu.unc.lib.boxc.web.services.processing.ImageServerV2Service;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author bbpennel
 */
public class ImageServerV2ControllerTest {
    private static final String IIIF_BASE = "http://example.com/iiif/v2/";
    private static final String SERVICES_BASE = "http://example.com/services/";
    private static final String ACCESS_BASE = "http://example.com/";

    private static final String OBJECT_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String FILE_ID = "b150dca9-c4cf-4651-aeef-3ce9e279178f";
    private static final PID OBJECT_PID = PIDs.get(OBJECT_ID);
    private static final PID FILE_PID = PIDs.get(FILE_ID);
    private final static String USERNAME = "test_user";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");

    @InjectMocks
    private ImageServerV2Controller imageController;

    private HttpClientConnectionManager httpClientConnectionManager;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private AccessCopiesService accessCopiesService;

    private ImageServerV2Service imageService;

    private MockMvc mockMvc;

    private AutoCloseable closeable;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        imageService = new ImageServerV2Service();
        imageService.setImageServerProxyBasePath(IIIF_BASE);
        imageService.setBasePath(SERVICES_BASE);
        imageService.setAccessAppPath(ACCESS_BASE);
        httpClientConnectionManager = new PoolingHttpClientConnectionManager();
        imageService.setHttpClientConnectionManager(httpClientConnectionManager);
        imageController.setImageServerV2Service(imageService);
        mockMvc = MockMvcBuilders.standaloneSetup(imageController)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
        when(accessControlService.hasAccess(any(), any(), any())).thenReturn(true);
    }

    @AfterEach
    void closeService() throws Exception {
        httpClientConnectionManager.shutdown();
        closeable.close();
    }

    @Test
    public void testGetManifest() throws Exception {
        var workObj = new ContentObjectSolrRecord();
        workObj.setId(OBJECT_ID);
        workObj.setResourceType(ResourceType.Work.name());
        workObj.setTitle("Test Work");
        var fileObj = new ContentObjectSolrRecord();
        fileObj.setId(FILE_ID);
        fileObj.setResourceType(ResourceType.File.name());
        fileObj.setTitle("file");
        var originalDs = new DatastreamImpl("original_file|image/jpeg|image.jpg|jpg|0|||375x250");
        var jp2Ds = new DatastreamImpl("jp2|image/jp2|image.jp2|jp2|0|||");
        fileObj.setDatastream(Arrays.asList(originalDs.toString(), jp2Ds.toString()));
        var viewableRecords = new ArrayList<ContentObjectRecord>(Arrays.asList(workObj, fileObj));
        when(accessCopiesService.listViewableFiles(eq(OBJECT_PID), any())).thenReturn(viewableRecords);

        var result = mockMvc.perform(get("/iiif/v2/" + OBJECT_ID + "/jp2/manifest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Manifest respManifest = parseManifestResponse(result);
        assertHasImageManifest(respManifest, FILE_PID, "Test Work", "file");
    }

    @Test
    public void testGetManifestNoAccess() throws Exception {
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), any(), any(), eq(Permission.viewAccessCopies));

        mockMvc.perform(get("/iiif/v2/" + OBJECT_ID + "/jp2/manifest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetManifestNoImages() throws Exception {
        when(accessCopiesService.listViewableFiles(eq(OBJECT_PID), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/iiif/v2/" + OBJECT_ID + "/jp2/manifest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    private void assertHasImageManifest(Manifest manifest, PID filePid, String label, String fileLabel) throws Exception {
        assertEquals(label, manifest.getLabelString());
        Sequence sequence = manifest.getSequences().get(0);
        List<Canvas> canvases = sequence.getCanvases();
        assertEquals(1, canvases.size());
        assertManifestContainsImage(manifest, 0, filePid, fileLabel);
    }

    private void assertManifestContainsImage(Manifest manifest, int index, PID filePid, String label) {
        Sequence sequence = manifest.getSequences().get(0);
        List<Canvas> canvases = sequence.getCanvases();
        Canvas canvas = canvases.get(index);
        assertEquals(label, canvas.getLabelString());
        assertEquals(375, canvas.getHeight().intValue());
        assertEquals(250, canvas.getWidth().intValue());
        List<Annotation> images = canvas.getImages();
        assertEquals(2, images.size());
        Annotation jp2Image = images.get(0);
        assertEquals("http://example.com/services/iiif/v2/" + filePid.getId() + "/jp2",
                jp2Image.getResource().getServices().get(0).getIdentifier().toString());
        Annotation thumbImage = images.get(1);
        assertEquals("http://example.com/services/services/api/thumb/" + filePid.getId() + "/large",
                thumbImage.getResource().getIdentifier().toString());
    }

    private Manifest parseManifestResponse(MvcResult result) throws Exception {
        MockHttpServletResponse response = result.getResponse();
        Manifest manifest = new IiifObjectMapper().readValue(response.getContentAsString(), Manifest.class);
        return manifest;
    }
}
