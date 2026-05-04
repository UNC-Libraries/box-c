package edu.unc.lib.boxc.web.services.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.ChildrenCountService;
import edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.search.solr.services.SearchResultResponseDecoratorService;
import edu.unc.lib.boxc.search.solr.services.SearchStateFactory;
import edu.unc.lib.boxc.search.solr.services.SetFacetTitleByIdService;
import edu.unc.lib.boxc.search.solr.utils.SearchStateUtil;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static edu.unc.lib.boxc.web.services.rest.MachineGeneratedSearchController.MG_ALT_TEXT_KEY;
import static edu.unc.lib.boxc.web.services.rest.MachineGeneratedSearchController.MG_FULL_DESCRIPTION_KEY;
import static edu.unc.lib.boxc.web.services.rest.MachineGeneratedSearchController.MG_REVIEW_ASSESSMENT_KEY;
import static edu.unc.lib.boxc.web.services.rest.MachineGeneratedSearchController.MG_SAFETY_ASSESSMENT_KEY;
import static edu.unc.lib.boxc.web.services.rest.MachineGeneratedSearchController.MG_TRANSCRIPT_KEY;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MachineGeneratedControllerTest {
    private static final String PARENT1_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String FILE1_ID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String FILE2_ID = "b150dca9-c4cf-4651-aeef-3ce9e279178f";
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private SolrQueryLayerService queryLayer;
    @Mock
    private SearchStateFactory searchStateFactory;
    private SearchState searchState;
    @Mock
    private MachineGeneratedContentService machineGeneratedContentService;
    @Mock
    private SearchResultResponse searchResultResponse;
    @Mock
    private ChildrenCountService childrenCountService;
    @Mock
    private SetFacetTitleByIdService setFacetTitleByIdService;
    @Mock
    private SearchSettings searchSettings;
    @Mock
    private SearchResultResponseDecoratorService searchResultResponseDecorator;
    @InjectMocks
    private MachineGeneratedSearchController controller;
    private MockMvc mvc;
    private AutoCloseable closeable;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    public void init() {
        closeable = openMocks(this);
        controller.setMachineGeneratedContentService(machineGeneratedContentService);
        var searchStateUtil = new SearchStateUtil();
        searchStateUtil.setSearchSettings(searchSettings);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        searchState = new SearchState();
        searchState.setFacetsToRetrieve(Collections.emptyList());

        TestHelper.setContentBase("http://localhost:48085/rest");

        when(searchStateFactory.createSearchState(any())).thenReturn(searchState);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testNoPermissionErrorResponse() throws Exception {
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(anyString(), any(), any(AccessGroupSetImpl.class), eq(viewHidden));

        mvc.perform(get("/machineGeneratedSearch/" + PARENT1_ID))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void testNoSearchResultsErrorResponse() throws Exception {
        when(queryLayer.performSearch(any())).thenReturn(null);

        MvcResult result = mvc.perform(get("/machineGeneratedSearch/" + PARENT1_ID))
                .andExpect(status().isOk())
                .andReturn();

        var apiResponseString = result.getResponse().getContentAsString();
        var apiResponseJson = deserializeApiResponse(apiResponseString);
        assertTrue(apiResponseJson.isEmpty());
    }

    @Test
    public void testSuccessResponse() throws Exception {
        JsonNode mgDescJson = MachineGeneratedContentService.MAPPER.readTree(loadDefaultJson());
        var fileRec1 = createContentObjectRecord(FILE1_ID, "1");
        var fileRec2 = createContentObjectRecord(FILE2_ID,"2");
        when(queryLayer.performSearch(any())).thenReturn(searchResultResponse);
        when(searchResultResponse.getResultList()).thenReturn(Arrays.asList(fileRec1, fileRec2));
        when(searchResultResponse.getSearchState()).thenReturn(searchState);
        when(machineGeneratedContentService.deserializeMachineGeneratedDescription(any())).thenReturn(mgDescJson);

        var result = mvc.perform(get("/machineGeneratedSearch/" + PARENT1_ID))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(searchResultResponseDecorator).populateThumbnailUrls(
                any(AccessGroupSet.class), eq(searchResultResponse));
        verify(searchResultResponseDecorator).retrieveFacets(
                any(SearchRequest.class), eq(searchResultResponse));
        verify(machineGeneratedContentService, times(2)).extractAltText(eq(mgDescJson));
        verify(machineGeneratedContentService, times(2)).extractTranscript(eq(mgDescJson));
        verify(machineGeneratedContentService, times(2)).extractFullDescription(eq(mgDescJson));
        verify(machineGeneratedContentService, times(2)).extractReviewAssessment(eq(mgDescJson));
        verify(machineGeneratedContentService, times(2)).extractSafetyAssessment(eq(mgDescJson));

        var response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        var apiResponseJson = deserializeApiResponse(response);
        var metadata = apiResponseJson.get("metadata");

        // check that the metadata has the added fields
        var firstObjMetadata = metadata.get(0);
        assertTrue(firstObjMetadata.has(MG_ALT_TEXT_KEY));
        assertTrue(firstObjMetadata.has(MG_TRANSCRIPT_KEY));
        assertTrue(firstObjMetadata.has(MG_FULL_DESCRIPTION_KEY));
        assertTrue(firstObjMetadata.has(MG_REVIEW_ASSESSMENT_KEY));
        assertTrue(firstObjMetadata.has(MG_SAFETY_ASSESSMENT_KEY));

        var secondObjMetadata = metadata.get(0);
        assertTrue(secondObjMetadata.has(MG_ALT_TEXT_KEY));
        assertTrue(secondObjMetadata.has(MG_TRANSCRIPT_KEY));
        assertTrue(secondObjMetadata.has(MG_FULL_DESCRIPTION_KEY));
        assertTrue(secondObjMetadata.has(MG_REVIEW_ASSESSMENT_KEY));
        assertTrue(secondObjMetadata.has(MG_SAFETY_ASSESSMENT_KEY));
    }

    private ContentObjectSolrRecord createContentObjectRecord(String id, String suffix) throws Exception {
        var rec = new ContentObjectSolrRecord();
        rec.setId(id);
        rec.setTitle("title" + suffix);
        rec.setAltText("alt text " + suffix);
        rec.setMgContentTags(List.of("content", "tag"));
        rec.setMgDescription(loadDefaultJson());

        return rec;
    }

    private String loadDefaultJson() throws Exception {
        return Files.readString(
                Path.of("src/test/resources/datastream/machineGeneratedDescriptionDefaults.json"));
    }

    private JsonNode deserializeApiResponse(String apiResponse) throws JsonProcessingException {
        return MAPPER.readTree(apiResponse);
    }
}
