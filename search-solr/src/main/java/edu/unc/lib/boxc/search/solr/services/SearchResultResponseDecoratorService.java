package edu.unc.lib.boxc.search.solr.services;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;

/**
 * Service for decorating SearchResultResponse objects
 */
public class SearchResultResponseDecoratorService {
    private AccessCopiesService accessCopiesService;
    private MultiSelectFacetListService multiSelectFacetListService;

    /**
     * Adds thumbnail URLs to the search result response
     * @param accessGroup principals for the thumbnails
     * @param result search result response to decorate
     */
    public void populateThumbnailUrls(AccessGroupSet accessGroup, SearchResultResponse result) {
        accessCopiesService.populateThumbnailInfoForList(result.getResultList(),
                accessGroup, true);
        accessCopiesService.populateThumbnailInfo(result.getSelectedContainer(),
                accessGroup, true);
    }

    /**
     * Gets facets for the search result response and populates them
     * @param searchRequest search request that produced the search result response
     * @param resultResponse search result response to decorate
     */
    public void retrieveFacets(SearchRequest searchRequest, SearchResultResponse resultResponse) {
        SearchState searchState = searchRequest.getSearchState();
        AccessGroupSet principals = searchRequest.getAccessGroups();
        SearchState facetState = (SearchState) searchState.clone();
        SearchRequest facetRequest = new SearchRequest(facetState, principals, true);
        facetRequest.setApplyCutoffs(false);
        if (resultResponse.getSelectedContainer() != null) {
            facetState.addFacet(resultResponse.getSelectedContainer().getPath());
        }

        SearchResultResponse resultResponseFacets = multiSelectFacetListService.getFacetListResult(facetRequest);
        resultResponse.setFacetFields(resultResponseFacets.getFacetFields());

        // Get minimum year for date created "facet" search
        if (facetState.getFacetsToRetrieve().contains(SearchFieldKey.DATE_CREATED_YEAR.name())) {
            String minSearchYear = multiSelectFacetListService.getMinimumDateCreatedYear(facetState, searchRequest);
            resultResponse.setMinimumDateCreatedYear(minSearchYear);
        }
    }

    public void setAccessCopiesService(AccessCopiesService accessCopiesService) {
        this.accessCopiesService = accessCopiesService;
    }

    public void setMultiSelectFacetListService(MultiSelectFacetListService multiSelectFacetListService) {
        this.multiSelectFacetListService = multiSelectFacetListService;
    }
}
