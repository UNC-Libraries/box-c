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
package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.facets.CutoffFacetImpl;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.MultiSelectFacetListService;
import edu.unc.lib.boxc.web.common.controllers.AbstractErrorHandlingSearchController;
import edu.unc.lib.boxc.web.common.services.AccessCopiesService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;

/**
 * Controller which interprets the provided search state, from either the last search state in the session or from GET
 * parameters, as well as actions performed on the state, and retrieves search results using it.
 *
 * @author bbpennel
 */
@Controller
public class SearchActionController extends AbstractErrorHandlingSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(SearchActionController.class);
    private static final int DEFAULT_COLLECTIONS_PER_PAGE = 500;

    @Autowired
    private MultiSelectFacetListService multiSelectFacetListService;
    @Autowired
    private AccessCopiesService accessCopiesService;

    @RequestMapping("/search")
    public String search() {
        return "searchResults";
    }

    @RequestMapping("/search/{pid}")
    public String search(@PathVariable("pid") String pid) {
        return "searchResults";
    }

    /**
     * Endpoint which returns search results, ignoring hierarchy, with any supplied filters limiting the results.
     * @param getFacets if true, then will retrieve facet results as well
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/searchJson", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody
    Map<String, Object> searchJson(@RequestParam("getFacets") Optional<Boolean> getFacets, HttpServletRequest request,
                                   HttpServletResponse response) {
        return searchJsonRequest(request, getFacets.orElse(false), null);
    }

    /**
     * Endpoint which returns search results containing children of the specified object, ignoring hierarchy, with any
     * supplied filters limiting the results.
     * @param pid ID of the parent object whose children will be searched.
     * @param getFacets if true, then will retrieve facet results as well
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/searchJson/{pid}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody
    Map<String, Object> searchJson(@PathVariable("pid") String pid,
                                   @RequestParam("getFacets") Optional<Boolean> getFacets,
                                   HttpServletRequest request, HttpServletResponse response) {
        return searchJsonRequest(request, getFacets.orElse(false), pid);
    }

    @RequestMapping(value = "/list/{pid}", method = RequestMethod.GET)
    public String listRedirect(@PathVariable("pid") String pid) {
        return "redirect:/record/{pid}";
    }

    /**
     * Endpoint which returns search results containing the immediate children of the object specified, with any
     * supplied filters limiting the results.
     * @param pid ID of the parent object whose immediate children will be searched.
     * @param getFacets if true, then will retrieve facet results as well
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/listJson/{pid}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody
    Map<String, Object> listJson(@PathVariable("pid") String pid,
                                 @RequestParam("getFacets") Optional<Boolean> getFacets,
                                 HttpServletRequest request, HttpServletResponse response) {
        SearchRequest searchRequest = generateSearchRequest(request);
        searchRequest.setRootPid(PIDs.get(pid));
        searchRequest.setApplyCutoffs(true);
        setDefaultRollup(searchRequest, true);
        SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);
        populateThumbnailUrls(searchRequest, resultResponse);

        if (getFacets.orElse(false)) {
            retrieveFacets(searchRequest, resultResponse);
        }
        return getResults(resultResponse, "list", request);
    }

    @RequestMapping("/collections")
    public String browseCollections() {
        return "collectionBrowse";
    }

    /**
     * Endpoint which returns all the admin units in the repository
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/collectionsJson", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody Map<String, Object> browseCollectionsJson(HttpServletRequest request,
                                                               HttpServletResponse response) {
        SearchRequest searchRequest = generateSearchRequest(request);
        searchRequest.setRootPid(RepositoryPaths.getContentRootPid());
        // Restrict results to the immediate children of the root of the repository
        CutoffFacet cutoff = new CutoffFacetImpl(SearchFieldKey.ANCESTOR_PATH.name(), "1,*!2");
        searchRequest.getSearchState().addFacet(cutoff);
        searchRequest.setApplyCutoffs(true);

        SearchState searchState = searchRequest.getSearchState();
        searchState.setRowsPerPage(DEFAULT_COLLECTIONS_PER_PAGE);
        SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);
        populateThumbnailUrls(searchRequest, resultResponse);

        return getResults(resultResponse, "search", request);
    }

    private Map<String, Object> searchJsonRequest(HttpServletRequest request, Boolean getFacets, String pid) {
        SearchRequest searchRequest = generateSearchRequest(request);
        if (pid != null) {
            searchRequest.setRootPid(PIDs.get(pid));
        }
        searchRequest.setApplyCutoffs(false);
        setDefaultRollup(searchRequest, false);

        SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);
        populateThumbnailUrls(searchRequest, resultResponse);

        if (getFacets) {
            retrieveFacets(searchRequest, resultResponse);
        }
        return getResults(resultResponse, "search", request);
    }

    private void retrieveFacets(SearchRequest searchRequest, SearchResultResponse resultResponse) {
        SearchState searchState = searchRequest.getSearchState();
        AccessGroupSet principals = searchRequest.getAccessGroups();
        SearchRequest facetRequest = new SearchRequest(searchState, principals, true);
        facetRequest.setApplyCutoffs(false);
        if (resultResponse.getSelectedContainer() != null) {
            SearchState facetState = (SearchState) searchState.clone();
            facetState.addFacet(resultResponse.getSelectedContainer().getPath());
            facetRequest.setSearchState(facetState);
        }

        SearchResultResponse resultResponseFacets = multiSelectFacetListService.getFacetListResult(facetRequest);
        resultResponse.setFacetFields(resultResponseFacets.getFacetFields());

        // Get minimum year for date created "facet" search
        if (facetRequest.getSearchState().getFacetsToRetrieve().contains(SearchFieldKey.DATE_CREATED_YEAR.name())) {
            String minSearchYear = multiSelectFacetListService.getMinimumDateCreatedYear(searchState, searchRequest);
            resultResponse.setMinimumDateCreatedYear(minSearchYear);
        }
    }

    private void setDefaultRollup(SearchRequest searchRequest, boolean isListing) {
        if (searchRequest.getSearchState().getRollup() == null) {
            var enableRollup = shouldEnableRollup(searchRequest);
            LOG.debug("Rollup not specified in request, determine rollup should be set to {}", enableRollup);
            searchRequest.getSearchState().setRollup(enableRollup);
            if (!enableRollup && !isListing) {
                LOG.debug("Removing File objects from non-rollup query {}",
                        searchRequest.getSearchState().getResourceTypes());
                searchRequest.getSearchState().getResourceTypes().remove(ResourceType.File.name());
            }
        }
    }

    private boolean shouldEnableRollup(SearchRequest searchRequest) {
        var state = searchRequest.getSearchState();
        var searchFields = state.getSearchFields();
        if (searchFields.isEmpty()) {
            return false;
        }
        for (var searchField : searchFields.entrySet()) {
            if (!StringUtils.isBlank(searchField.getValue())) {
                return true;
            }
        }
        return false;
    }

    private void populateThumbnailUrls(SearchRequest searchRequest, SearchResultResponse result) {
        accessCopiesService.populateThumbnailIds(result.getResultList(),
                searchRequest.getAccessGroups(), true);
        accessCopiesService.populateThumbnailId(result.getSelectedContainer(),
                searchRequest.getAccessGroups(), true);
    }
}
