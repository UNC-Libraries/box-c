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
package edu.unc.lib.dl.search.solr.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.FacetFieldList;
import edu.unc.lib.dl.search.solr.model.FacetFieldObject;
import edu.unc.lib.dl.search.solr.model.MultivaluedHierarchicalFacet;
import edu.unc.lib.dl.search.solr.model.SearchFacet;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.FacetFieldUtil;

/**
 * Service for retrieving facet listings for searches supporting selection of multiple values for the same facet.
 *
 * @author bbpennel
 */
public class MultiSelectFacetListService extends AbstractQueryService {

    private SolrSearchService searchService;

    public SearchResultResponse getFacetListResult(SearchRequest searchRequest) {
        SearchState searchState = (SearchState) searchRequest.getSearchState().clone();

        BriefObjectMetadata selectedContainer = null;
        if (searchRequest.getRootPid() != null) {
            selectedContainer = searchService.addSelectedContainer(searchRequest.getRootPid(), searchState,
                    searchRequest.isApplyCutoffs(), searchRequest.getAccessGroups());
            if (selectedContainer == null) {
                throw new NotFoundException("Invalid container selected");
            }
        }

        // Turning off rollup because it is really slow
        searchState.setRollup(false);

        // Calculate facets with facet filters applied
        SearchRequest facetRequest = new SearchRequest(searchState, searchRequest.getAccessGroups(), true);

        searchState.setRowsPerPage(0);
        searchState.setResourceTypes(null);

        SearchResultResponse resultResponse = searchService.getSearchResults(facetRequest);
        resultResponse.setSelectedContainer(selectedContainer);
        FacetFieldList resultFacets = resultResponse.getFacetFields();

        // For each facet selected in the original search state, overwrite facet list with facet results as
        // if the that facet were not selected
        for (Entry<String, List<SearchFacet>> facetEntry: searchRequest.getSearchState().getFacets().entrySet()) {
            String facetName = facetEntry.getKey();
            SearchState selectedState = (SearchState) searchState.clone();
            selectedState.getFacets().remove(facetName);

            selectedState.setFacetsToRetrieve(Arrays.asList(facetName));
            SearchRequest selectedRequest = new SearchRequest(selectedState, searchRequest.getAccessGroups(), true);
            SearchResultResponse selectedResponse = searchService.getSearchResults(selectedRequest);

            int index = FacetFieldList.indexOf(resultFacets, facetName);
            // For MultivaluedHierarchicalFacet need to pull in the facets for child tiers
            if (FacetFieldUtil.facetIsOfType(facetEntry.getValue(), MultivaluedHierarchicalFacet.class)) {
                List<SearchFacet> merged = new ArrayList<>(selectedResponse.getFacetFields().get(0).getValues());
                Set<String> alreadyRetrieved = new HashSet<>();
                for (SearchFacet selectedFacet: facetEntry.getValue()) {
                    MultivaluedHierarchicalFacet hierFacet = (MultivaluedHierarchicalFacet) selectedFacet;
                    // Since no facets currently use more than 2 tiers, lazily only retrieve results one tier down
                    String facetValue = hierFacet.getFacetNodes().get(0).getFacetValue();
                    // Skip if multiple selected values would retrieve same parent set
                    if (alreadyRetrieved.contains(facetValue)) {
                        continue;
                    }
                    alreadyRetrieved.add(facetValue);
                    MultivaluedHierarchicalFacet tierFacet = new MultivaluedHierarchicalFacet(
                            facetName, hierFacet.getFacetNodes().get(0).getFacetValue());

                    SearchState selectedState2 = (SearchState) searchState.clone();
                    selectedState2.setFacet(tierFacet);
                    selectedState2.setFacetsToRetrieve(Arrays.asList(facetName));

                    SearchRequest selectedRequest2 = new SearchRequest(
                            selectedState2, searchRequest.getAccessGroups(), true);
                    SearchResultResponse selectedResponse2 = searchService.getSearchResults(selectedRequest2);
                    merged.addAll(selectedResponse2.getFacetFields().get(0).getValues());
                }
                resultFacets.set(index, new FacetFieldObject(facetName, merged));
            } else {
                resultFacets.set(index, selectedResponse.getFacetFields().get(0));
            }
        }

        return resultResponse;
    }

    public void setSearchService(SolrSearchService searchService) {
        this.searchService = searchService;
    }
}
