package edu.unc.lib.boxc.web.services.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.search.solr.services.SearchStateFactory;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static edu.unc.lib.boxc.web.services.rest.MachineGeneratedSearchController.NO_RESULTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
    @Mock
    private SearchState searchState;
    @Mock
    private MachineGeneratedContentService machineGeneratedContentService;
    @Mock
    private SearchResultResponse searchResultResponse;
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
        controller.setMachineGeneratedContentService(machineGeneratedContentService);
        controller.setQueryLayer(queryLayer);
        controller.setSearchStateFactory(searchStateFactory);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();

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
        assertEquals(NO_RESULTS, apiResponseJson.get("errorMessage").textValue());
        assertTrue(apiResponseJson.get("results").isEmpty());
    }

    @Test
    public void testSuccessResponse() throws Exception {
        when(queryLayer.performSearch(any())).thenReturn(searchResultResponse);
        var fileRec1 = createContentObjectRecord(FILE1_ID, "1");
        var fileRec2 = createContentObjectRecord(FILE2_ID,"2");
        when(searchResultResponse.getResultList()).thenReturn(Arrays.asList(fileRec1, fileRec2));

        MvcResult result = mvc.perform(get("/machineGeneratedSearch/" + PARENT1_ID))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        var apiResponseString = result.getResponse().getContentAsString();
        var apiResponseJson = deserializeApiResponse(apiResponseString).get("results");
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
