/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.boxc.search.solr.services;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.FacetFieldList;
import edu.unc.lib.boxc.search.api.facets.SearchFacet;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for retrieving facet listings for searches supporting selection of multiple values for the same facet.
 *
 * @author bbpennel
 */
public class MultiSelectFacetListService extends AbstractQueryService {
    private static final List<String> DEFAULT_RESOURCE_TYPES = Arrays.asList(
            ResourceType.AdminUnit.name(), ResourceType.Collection.name(),
            ResourceType.Folder.name(), ResourceType.Work.name());

    private SolrSearchService searchService;

    public SearchResultResponse getFacetListResult(SearchRequest searchRequest) {
        SearchState searchState = (SearchState) searchRequest.getSearchState().clone();

        ContentObjectRecord selectedContainer = null;
        if (searchRequest.getRootPid() != null) {
            selectedContainer = searchService.addSelectedContainer(searchRequest.getRootPid(), searchState,
                    searchRequest.isApplyCutoffs(), searchRequest.getAccessGroups());
            if (selectedContainer == null) {
                throw new NotFoundException("Invalid container selected");
            }
        }

        // Turning off rollup because it is really slow
        searchState.setRollup(false);

        // Trim out retrieval of all facets which are also filtered, since they will be loaded separately
        Set<String> facetFilterSet = searchState.getFacets().keySet();
        if (facetFilterSet.size() > 0) {
            List<String> toRetrieve = searchState.getFacetsToRetrieve().stream()
                    .filter(f -> !facetFilterSet.contains(f)).collect(Collectors.toList());
            searchState.setFacetsToRetrieve(toRetrieve);
        }

        // Calculate facets with facet filters applied
        SearchRequest facetRequest = new SearchRequest(searchState, searchRequest.getAccessGroups(), true);
        facetRequest.setApplyCutoffs(searchRequest.isApplyCutoffs());

        searchState.setRowsPerPage(0);
        // Set the resource types counted in the facets to exclude File objects
        if (searchState.getResourceTypes() == null) {
            searchState.setResourceTypes(DEFAULT_RESOURCE_TYPES);
        } else {
            searchState.setResourceTypes(searchState.getResourceTypes().stream()
                    .filter(t -> !t.equals(ResourceType.File.name()))
                    .collect(Collectors.toList()));
        }

        // Perform base search with all filters applied, generating the base result response which will be returned.
        SearchResultResponse resultResponse = searchService.getSearchResults(facetRequest);
        resultResponse.setSelectedContainer(selectedContainer);
        // Get list of facet fields without filters. Next we will add facet fields which are filtered to this list.
        FacetFieldList resultFacets = resultResponse.getFacetFields();

        // For each facet selected in the original search state, add facet list with results as
        // if the that facet were not selected
        for (Entry<String, List<SearchFacet>> facetEntry: searchRequest.getSearchState().getFacets().entrySet()) {
            String facetName = facetEntry.getKey();
            // Skip retrieval of facets that are filtered but not being retrieved in original request
            if (!searchRequest.getSearchState().getFacetsToRetrieve().contains(facetName)) {
                continue;
            }

            SearchState selectedState = (SearchState) searchState.clone();
            selectedState.getFacets().remove(facetName);

            selectedState.setFacetsToRetrieve(Arrays.asList(facetName));
            SearchRequest selectedRequest = new SearchRequest(selectedState, searchRequest.getAccessGroups(), true);
            SearchResultResponse selectedResponse = searchService.getSearchResults(selectedRequest);

            resultFacets.add(selectedResponse.getFacetFields().get(0));
        }

        resultFacets.sort(searchRequest.getSearchState().getFacetsToRetrieve());
        return resultResponse;
    }

    /**
     * Finds and returns the minimum year created for the given query
     * @param selectedState
     * @param originalRequest
     * @return String
     */
    public String getMinimumDateCreatedYear(SearchState selectedState, SearchRequest originalRequest) {
        selectedState.setRowsPerPage(1);
        selectedState.setResultFields(Collections.singletonList(SearchFieldKey.DATE_CREATED_YEAR.name()));
        selectedState.setSortNormalOrder(false);
        selectedState.setSortType("dateCreated");

        SearchRequest selectedRequest = new SearchRequest(
                selectedState, originalRequest.getAccessGroups(), false);
        SearchResultResponse selectedResponse = searchService.getSearchResults(selectedRequest);

        try {
            return selectedResponse.getResultList().get(0).getDateCreatedYear();
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public void setSearchService(SolrSearchService searchService) {
        this.searchService = searchService;
    }
}
