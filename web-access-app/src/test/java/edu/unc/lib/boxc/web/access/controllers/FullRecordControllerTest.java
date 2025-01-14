package edu.unc.lib.boxc.web.access.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.services.ObjectAclFactory;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.services.ChildrenCountService;
import edu.unc.lib.boxc.search.solr.services.GetCollectionIdService;
import edu.unc.lib.boxc.search.solr.services.NeighborQueryService;
import edu.unc.lib.boxc.search.solr.services.SearchStateFactory;
import edu.unc.lib.boxc.search.solr.services.SetFacetTitleByIdService;
import edu.unc.lib.boxc.web.common.services.AccessCopiesService;
import edu.unc.lib.boxc.web.common.services.FindingAidUrlService;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import edu.unc.lib.boxc.web.common.services.WorkFilesizeService;
import edu.unc.lib.boxc.web.common.services.XmlDocumentFilteringService;
import edu.unc.lib.boxc.web.common.utils.SerializationUtil;
import edu.unc.lib.boxc.web.common.view.XSLViewResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static edu.unc.lib.boxc.web.common.services.AccessCopiesService.PDF_MIMETYPE_REGEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author bbpennel
 */
public class FullRecordControllerTest {
    private static final String PID_1 = "bc9795df-0de9-444c-a4ac-9585378b2d88";
    private static final String PID_2 = "5c79c898-8698-4fba-84d3-64317b3c73f5";
    protected MockMvc mvc;
    private AutoCloseable closeable;
    @InjectMocks
    private FullRecordController controller;

    @Mock
    private AccessControlService aclService;
    @Mock
    private ChildrenCountService childrenCountService;
    @Mock
    private NeighborQueryService neighborService;
    @Mock
    private GetCollectionIdService collectionIdService;
    @Mock
    private FindingAidUrlService findingAidUrlService;
    @Mock
    private AccessCopiesService accessCopiesService;
    @Mock
    private WorkFilesizeService workFilesizeService;
    @Mock
    private XmlDocumentFilteringService xmlDocumentFilteringService;
    @Mock
    private ObjectAclFactory objectAclFactory;

    @Mock
    private XSLViewResolver xslViewResolver;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;

    @Mock
    protected SolrQueryLayerService queryLayer;
    @Mock
    protected SearchSettings searchSettings;
    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;
    @Mock
    protected SearchStateFactory searchStateFactory;
    @Mock
    private SetFacetTitleByIdService setFacetTitleByIdService;

    @Mock
    private ContentObjectRecord briefObject;
    @Mock
    private ContentObjectRecord childBriefObject;
    
    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);
        TestHelper.setContentBase("http://localhost:48085/rest");

        mvc = MockMvcBuilders.standaloneSetup(controller).build();

        SerializationUtil.injectSettings(searchSettings, null, globalPermissionEvaluator);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testHandlePdfViewerRequestWorkWithPdf() throws Exception {
        when(briefObject.getId()).thenReturn(PID_1);
        when(briefObject.getResourceType()).thenReturn(ResourceType.Work.name());
        when(queryLayer.getObjectById(any())).thenReturn(briefObject);

        when(childBriefObject.getId()).thenReturn(PID_2);
        when(accessCopiesService.getFirstMatchingChild(any(), any(), any())).thenReturn(childBriefObject);

        mvc.perform(get("/api/record/" + PID_1 + "/pdfViewer"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("viewerPid", PID_2))
                .andReturn();
    }

    @Test
    public void testHandlePdfViewerRequestWorkWithNoPdf() throws Exception {
        when(briefObject.getId()).thenReturn(PID_1);
        when(briefObject.getResourceType()).thenReturn(ResourceType.Work.name());
        when(queryLayer.getObjectById(any())).thenReturn(briefObject);

        when(accessCopiesService.getFirstMatchingChild(any(), any(), any())).thenReturn(null);

        mvc.perform(get("/api/record/" + PID_1 + "/pdfViewer"))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    public void testHandlePdfViewerRequestPdfFile() throws Exception {
        when(briefObject.getId()).thenReturn(PID_1);
        when(briefObject.getResourceType()).thenReturn(ResourceType.File.name());
        when(queryLayer.getObjectById(any())).thenReturn(briefObject);

        when(accessCopiesService.getDatastreamPid(any(), any(), any())).thenReturn(PID_1);

        mvc.perform(get("/api/record/" + PID_1 + "/pdfViewer"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("viewerPid", PID_1))
                .andReturn();
    }

    @Test
    public void testHandlePdfViewerRequestNonPdfFile() throws Exception {
        when(briefObject.getId()).thenReturn(PID_1);
        when(briefObject.getResourceType()).thenReturn(ResourceType.File.name());
        when(queryLayer.getObjectById(any())).thenReturn(briefObject);

        when(accessCopiesService.getDatastreamPid(any(), any(), any())).thenReturn(null);

        mvc.perform(get("/api/record/" + PID_1 + "/pdfViewer"))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void testHandlePdfViewerRequestNoPermission() throws Exception {
        var pid = PIDs.get(PID_1);
        when(briefObject.getId()).thenReturn(PID_1);
        when(briefObject.getResourceType()).thenReturn(ResourceType.File.name());
        when(queryLayer.getObjectById(any())).thenReturn(briefObject);

        doThrow(new AccessRestrictionException("No permission")).when(aclService).assertHasAccess(
                any(), eq(pid), any(), eq(Permission.viewOriginal));

        mvc.perform(get("/api/record/" + PID_1 + "/pdfViewer"))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void testHandleJsonRequestWorkWithPdf() throws Exception {
        when(briefObject.getId()).thenReturn(PID_1);
        when(briefObject.getParentCollection()).thenReturn(PID_2);
        when(briefObject.getResourceType()).thenReturn(ResourceType.Work.name());
        when(queryLayer.getObjectById(any())).thenReturn(briefObject);

        when(childBriefObject.getId()).thenReturn(PID_2);
        when(accessCopiesService.getFirstMatchingChild(any(), any(), any())).thenReturn(childBriefObject);

        when(childrenCountService.getChildrenCount(any(), any())).thenReturn(1L);
        when(neighborService.getNeighboringItems(any(), anyInt(), any())).thenReturn(Collections.emptyList());
        when(workFilesizeService.getTotalFilesize(any(), any())).thenReturn(100L);

        when(accessCopiesService.getDatastreamPid(any(), any(), eq(PDF_MIMETYPE_REGEX))).thenReturn(PID_1);

        var result = mvc.perform(get("/api/record/" + PID_1 + "/json"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(PID_1, respMap.get("viewerPid"));
        assertEquals("pdf", respMap.get("viewerType"));
        assertEquals(100, respMap.get("totalDownloadSize"));
        assertNotNull(respMap.get("briefObject"));
    }

    @Test
    public void testHandleJsonRequestWorkWithPdfWithoutPermission() throws Exception {
        when(briefObject.getId()).thenReturn(PID_1);
        when(briefObject.getParentCollection()).thenReturn(PID_2);
        when(briefObject.getResourceType()).thenReturn(ResourceType.Work.name());
        when(queryLayer.getObjectById(any())).thenReturn(briefObject);

        when(childBriefObject.getId()).thenReturn(PID_2);
        when(accessCopiesService.getFirstMatchingChild(any(), any(), any())).thenReturn(childBriefObject);

        when(childrenCountService.getChildrenCount(any(), any())).thenReturn(1L);
        when(neighborService.getNeighboringItems(any(), anyInt(), any())).thenReturn(Collections.emptyList());
        when(workFilesizeService.getTotalFilesize(any(), any())).thenReturn(100L);

        // Returning the child for viewer pid, but user doesn't have permission to view child
        when(accessCopiesService.getDatastreamPid(any(), any(), eq(PDF_MIMETYPE_REGEX))).thenReturn(PID_2);
        when(aclService.hasAccess(eq(PIDs.get(PID_2)), any(), eq(Permission.viewOriginal))).thenReturn(false);

        var result = mvc.perform(get("/api/record/" + PID_1 + "/json"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertNull(respMap.get("viewerPid"));
        assertNull(respMap.get("viewerType"));
        assertEquals(100, respMap.get("totalDownloadSize"));
        assertNotNull(respMap.get("briefObject"));
    }

    public static Map<String, Object> getMapFromResponse(MvcResult result) throws Exception {
        MapType type = defaultInstance().constructMapType(HashMap.class, String.class, Object.class);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(result.getResponse().getContentAsString(), type);
    }
}
