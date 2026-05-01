package edu.unc.lib.boxc.search.solr.services;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.facets.FacetFieldList;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class SearchResultResponseDecoratorServiceTest {
    @Mock
    private AccessCopiesService accessCopiesService;
    @Mock
    private MultiSelectFacetListService multiSelectFacetListService;
    @Mock
    private AccessGroupSet principals;
    @Mock
    private SearchResultResponse searchResultResponse, resultResponseFacets;
    @Mock
    private SearchRequest searchRequest;
    @Mock
    private FacetFieldList facetFieldList;
    @Mock
    private ContentObjectRecord contentObjectRecord1, contentObjectRecord2, selectedContainer;
    @Captor
    private ArgumentCaptor<SearchRequest> searchRequestArgumentCaptor;
    @Captor
    private ArgumentCaptor<SearchState> searchStateArgumentCaptor;
    private List<ContentObjectRecord> contentObjectList = Arrays.asList(contentObjectRecord1, contentObjectRecord2);
    private SearchResultResponseDecoratorService searchResultResponseDecorator;
    private SearchState searchState;
    private AutoCloseable closeable;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        searchResultResponseDecorator = new SearchResultResponseDecoratorService();
        searchResultResponseDecorator.setAccessCopiesService(accessCopiesService);
        searchResultResponseDecorator.setMultiSelectFacetListService(multiSelectFacetListService);
        when(searchResultResponse.getResultList()).thenReturn(contentObjectList);

        searchState = new SearchState();
        searchState.setFacetsToRetrieve(Collections.emptyList());
        when(searchRequest.getSearchState()).thenReturn(searchState);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void populateThumbnailUrlsTest() {
        searchResultResponseDecorator.populateThumbnailUrls(principals, searchResultResponse);

        verify(accessCopiesService).populateThumbnailInfoForList(
                eq(contentObjectList), eq(principals), eq(true));
        verify(accessCopiesService).populateThumbnailInfoForList(
                eq(contentObjectList), eq(principals), eq(true));
    }

    @Test
    public void retrieveFacetsTest() {
        when(multiSelectFacetListService.getFacetListResult(any())).thenReturn(resultResponseFacets);
        when(resultResponseFacets.getFacetFields()).thenReturn(facetFieldList);
        searchResultResponseDecorator.retrieveFacets(searchRequest, searchResultResponse);

        verify(multiSelectFacetListService, times(1))
                .getFacetListResult(searchRequestArgumentCaptor.capture());

        var searchRequest = searchRequestArgumentCaptor.getValue();
        assertFalse(searchRequest.isApplyCutoffs());
    }

    @Test
    public void retrieveFacetsWithSelectedContainerTest() {
        when(multiSelectFacetListService.getFacetListResult(any())).thenReturn(resultResponseFacets);
        when(resultResponseFacets.getFacetFields()).thenReturn(facetFieldList);
        when(searchResultResponse.getSelectedContainer()).thenReturn(selectedContainer);
        var cutoffFacet = mock(CutoffFacet.class);
        when(selectedContainer.getPath()).thenReturn(cutoffFacet);
        searchState.setFacetsToRetrieve(List.of(SearchFieldKey.DATE_CREATED_YEAR.name()));

        searchResultResponseDecorator.retrieveFacets(searchRequest, searchResultResponse);

        assertFalse(searchRequest.isApplyCutoffs());
        verify(multiSelectFacetListService).getMinimumDateCreatedYear(
                searchStateArgumentCaptor.capture(), eq(searchRequest));

        var searchState = searchStateArgumentCaptor.getValue();
        assertFalse(searchState.getFacets().isEmpty());
    }

    @Test
    public void retrieveFacetsWithMinimumYearTest() {
        when(multiSelectFacetListService.getFacetListResult(any())).thenReturn(resultResponseFacets);
        when(resultResponseFacets.getFacetFields()).thenReturn(facetFieldList);
        searchState.setFacetsToRetrieve(List.of(SearchFieldKey.DATE_CREATED_YEAR.name()));
        searchResultResponseDecorator.retrieveFacets(searchRequest, searchResultResponse);

        verify(multiSelectFacetListService).getFacetListResult(any(SearchRequest.class));
        verify(multiSelectFacetListService).getMinimumDateCreatedYear(
                any(SearchState.class), eq(searchRequest));
    }
}
