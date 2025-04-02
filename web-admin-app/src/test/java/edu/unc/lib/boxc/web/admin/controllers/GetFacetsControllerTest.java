package edu.unc.lib.boxc.web.admin.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import edu.unc.lib.boxc.common.test.TestHelpers;
import edu.unc.lib.boxc.search.api.facets.FacetFieldList;
import edu.unc.lib.boxc.search.api.requests.SearchState;

import edu.unc.lib.boxc.search.solr.services.SearchStateFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.facets.FacetFieldObject;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.MultiSelectFacetListService;
import edu.unc.lib.boxc.search.solr.utils.SearchStateUtil;








public class GetFacetsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MultiSelectFacetListService multiSelectFacetListService;

    @Mock
    private SearchSettings searchSettings;

    @Mock
    private SearchRequest searchRequest;

    @Mock
    private SearchState searchState;

    @Mock
    private SearchResultResponse resultResponse;

    @Mock
    private SearchStateFactory searchStateFactory;

    @InjectMocks
    private GetFacetsController controller;

    @Captor
    private ArgumentCaptor<SearchRequest> searchRequestCaptor;

    private AutoCloseable closeable;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        // Partial mock of the controller to handle generateSearchRequest method
//        controller = spy(new GetFacetsController());

        // Set up our mocked dependencies
//        when(controller.generateSearchRequest(any(HttpServletRequest.class))).thenReturn(searchRequest);
//        when(searchRequest.getSearchState()).thenReturn(searchState);
        when(searchSettings.getFacetNames()).thenReturn(Arrays.asList("format", "contentType"));

        FacetFieldList facetFieldList = new FacetFieldList();
        facetFieldList.add(new FacetFieldObject("format", null));
        facetFieldList.add(new FacetFieldObject("contentType", null));
        when(resultResponse.getFacetFields()).thenReturn(facetFieldList);
        when(resultResponse.getSelectedContainer()).thenReturn(null);

        when(searchStateFactory.createSearchState(anyMap())).thenReturn(searchState);

        // Set up multiSelectFacetListService to return our mocked response
        when(multiSelectFacetListService.getFacetListResult(any(SearchRequest.class))).thenReturn(resultResponse);

        // Set searchSettings field on controller via reflection to avoid NPE
//        try {
//            java.lang.reflect.Field field = AbstractSearchController.class.getDeclaredField("searchSettings");
//            field.setAccessible(true);
//            field.set(controller, searchSettings);
//        } catch (Exception e) {
//            // This would typically fail the test
//            throw new RuntimeException("Failed to set searchSettings field", e);
//        }

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetFacetsWithoutPid() throws Exception {
        mockMvc.perform(get("/facets"))
                .andExpect(status().isOk());

        verify(multiSelectFacetListService).getFacetListResult(searchRequestCaptor.capture());
        assertNull(searchRequestCaptor.getValue().getRootPid());
    }

    @Test
    public void testGetFacetsWithPid() throws Exception {
        PID pid = PIDs.get(UUID.randomUUID().toString());

        mockMvc.perform(get("/facets/" + pid.getId()))
                .andExpect(status().isOk());

        verify(multiSelectFacetListService).getFacetListResult(searchRequestCaptor.capture());
        assertEquals(pid, searchRequestCaptor.getValue().getRootPid());
    }

    @Test
    public void testGetFacetsWithNullFacetsToRetrieve() throws Exception {
        when(searchState.getFacetsToRetrieve()).thenReturn(null);

        mockMvc.perform(get("/facets"))
                .andExpect(status().isOk());

        verify(searchState).setFacetsToRetrieve(Arrays.asList("format", "contentType"));
    }

    @Test
    public void testGetFacetsWithExistingFacetsToRetrieve() throws Exception {
        when(searchState.getFacetsToRetrieve()).thenReturn(Arrays.asList("existingFacet"));

        mockMvc.perform(get("/facets"))
                .andExpect(status().isOk());

        verify(searchState, never()).setFacetsToRetrieve(Arrays.asList("format", "contentType"));
    }
}