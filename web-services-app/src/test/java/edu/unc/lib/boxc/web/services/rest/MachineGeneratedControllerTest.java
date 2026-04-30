package edu.unc.lib.boxc.web.services.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.AccessCopiesService;
import edu.unc.lib.boxc.search.solr.services.ChildrenCountService;
import edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.search.solr.services.MultiSelectFacetListService;
import edu.unc.lib.boxc.search.solr.services.SearchResultResponseDecoratorService;
import edu.unc.lib.boxc.search.solr.services.SearchStateFactory;
import edu.unc.lib.boxc.search.solr.services.SetFacetTitleByIdService;
import edu.unc.lib.boxc.search.solr.utils.SearchStateUtil;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import edu.unc.lib.boxc.web.common.utils.SerializationUtil;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static edu.unc.lib.boxc.web.services.rest.MachineGeneratedSearchController.NO_RESULTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
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
        machineGeneratedContentService = new MachineGeneratedContentService();
        controller = new MachineGeneratedSearchController();
        controller.setAccessControlService(accessControlService);
        controller.setSearchResultResponseDecoratorService(searchResultResponseDecorator);
        controller.setMachineGeneratedContentService(machineGeneratedContentService);
        controller.setQueryLayer(queryLayer);
        controller.setSearchStateFactory(searchStateFactory);
        controller.setChildrenCountService(childrenCountService);
        controller.setSetFacetTitleByIdService(setFacetTitleByIdService);
        var searchStateUtil = new SearchStateUtil();
        searchStateUtil.setSearchSettings(searchSettings);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        searchState = new SearchState();
        searchState.setFacetsToRetrieve(Collections.emptyList());

        TestHelper.setContentBase("http://localhost:48085/rest");

        when(searchStateFactory.createSearchState(any())).thenReturn(searchState);
        doAnswer((Answer<Void>) invocation -> null)
                .when(searchResultResponseDecorator).populateThumbnailUrls(any(), any());
        doAnswer((Answer<Void>) invocation -> null)
                .when(searchResultResponseDecorator).retrieveFacets(any(), any());
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
        var fileRec1 = createContentObjectRecord(FILE1_ID, "1");
        var fileRec2 = createContentObjectRecord(FILE2_ID,"2");
        when(queryLayer.performSearch(any())).thenReturn(searchResultResponse);
        when(searchResultResponse.getResultList()).thenReturn(Arrays.asList(fileRec1, fileRec2));
        when(searchResultResponse.getSearchState()).thenReturn(searchState);

        MvcResult result = mvc.perform(get("/machineGeneratedSearch/" + PARENT1_ID))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        var apiResponseString = result.getResponse().getContentAsString();
        var apiResponseJson = deserializeApiResponse(apiResponseString).get("metadata");
        var firstResult = apiResponseJson.get(0);
        var secondResult = apiResponseJson.get(1);
        assertEquals("Mountain landscape with snow-covered peaks", firstResult.get("mgAltText").textValue());
        assertEquals("alt text 1", firstResult.get("altText").textValue());
        assertEquals("alt text 2", secondResult.get("altText").textValue());
        assertEquals("A scenic mountain landscape with snow-capped peaks rising above a forested valley",
                secondResult.get("mgFullDescription").textValue());
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
