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
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract service for retrieving lists of facet values
 *
 * @author bbpennel
 */
public class AbstractFacetListService extends AbstractQueryService {
    protected static final List<String> DEFAULT_RESOURCE_TYPES = Arrays.asList(
            ResourceType.AdminUnit.name(), ResourceType.Collection.name(),
            ResourceType.Folder.name(), ResourceType.Work.name());

    protected SolrSearchService searchService;

    protected ContentObjectRecord addSelectedContainer(SearchRequest searchRequest, SearchState searchState) {
        if (searchRequest.getRootPid() != null) {
            var selectedContainer = searchService.addSelectedContainer(searchRequest.getRootPid(),
                    searchState, searchRequest.isApplyCutoffs(), searchRequest.getAccessGroups());
            if (selectedContainer == null) {
                throw new NotFoundException("Invalid container selected");
            }
            return selectedContainer;
        }
        return null;
    }

    /**
     * Set the resource types counted in the facets to exclude File objects
     * @param searchState
     */
    protected void assignResourceTypes(SearchState searchState) {
        if (searchState.getResourceTypes() == null) {
            searchState.setResourceTypes(DEFAULT_RESOURCE_TYPES);
        } else {
            searchState.setResourceTypes(searchState.getResourceTypes().stream()
                    .filter(t -> !t.equals(ResourceType.File.name()))
                    .collect(Collectors.toList()));
        }
    }

    public void setSearchService(SolrSearchService searchService) {
        this.searchService = searchService;
    }
}
