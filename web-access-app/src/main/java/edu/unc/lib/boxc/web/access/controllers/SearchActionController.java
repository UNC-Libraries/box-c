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
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.facets.CutoffFacetImpl;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.MultiSelectFacetListService;
import edu.unc.lib.boxc.search.solr.services.ParentCollectionFacetTitleService;
import edu.unc.lib.boxc.web.common.controllers.AbstractErrorHandlingSearchController;
import edu.unc.lib.boxc.web.common.services.AccessCopiesService;
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

    @Autowired
    private MultiSelectFacetListService multiSelectFacetListService;
    @Autowired
    private ParentCollectionFacetTitleService parentCollectionFacetTitleService;
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

    @RequestMapping(value = "/searchJson", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody
    Map<String, Object> searchJson(@RequestParam("getFacets") Optional<String> getFacets, HttpServletRequest request,
                                   HttpServletResponse response) {
        String facets = getFacets.orElse(null);
        return searchJsonRequest(request, facets, null);
    }

    @RequestMapping(value = "/searchJson/{pid}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody
    Map<String, Object> searchJson(@PathVariable("pid") String pid,
                                   @RequestParam("getFacets") Optional<String> getFacets,
                                   HttpServletRequest request, HttpServletResponse response) {
        String facets = getFacets.orElse(null);
        return searchJsonRequest(request, facets, pid);
    }

    @RequestMapping(value = "/list/{pid}", method = RequestMethod.GET)
    public String listRedirect(@PathVariable("pid") String pid) {
        return "redirect:/record/{pid}";
    }

    @RequestMapping(value = "/listJson/{pid}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody
    Map<String, Object> listJson(@PathVariable("pid") String pid, HttpServletRequest request,
                                 HttpServletResponse response) {
        SearchRequest searchRequest = generateSearchRequest(request);
        searchRequest.setRootPid(PIDs.get(pid));
        searchRequest.setApplyCutoffs(true);
        SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);
        populateThumbnailUrls(searchRequest, resultResponse);
        return getResults(resultResponse, "list", request);
    }

    @RequestMapping("/collections")
    public String browseCollections() {
        return "collectionBrowse";
    }

    @RequestMapping(value = "/collectionsJson", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody Map<String, Object> browseCollectionsJson(HttpServletRequest request,
                                                               HttpServletResponse response) {
        SearchRequest searchRequest = generateSearchRequest(request);
        searchRequest.setRootPid(RepositoryPaths.getContentRootPid());
        CutoffFacet cutoff = new CutoffFacetImpl(SearchFieldKey.ANCESTOR_PATH.name(), "1,*!2");
        searchRequest.getSearchState().addFacet(cutoff);
        searchRequest.setApplyCutoffs(true);

        SearchState searchState = searchRequest.getSearchState();
        searchState.setRowsPerPage(searchSettings.defaultCollectionsPerPage);
        searchState.setFacetsToRetrieve(searchSettings.collectionBrowseFacetNames);
        SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);
        populateThumbnailUrls(searchRequest, resultResponse);

        return getResults(resultResponse, "search", request);
    }

    private Map<String, Object> searchJsonRequest(HttpServletRequest request, String getFacets, String pid) {
        SearchRequest searchRequest = generateSearchRequest(request);
        if (pid != null) {
            searchRequest.setRootPid(PIDs.get(pid));
        }
        searchRequest.setApplyCutoffs(false);
        SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);
        populateThumbnailUrls(searchRequest, resultResponse);

        SearchState searchState = searchRequest.getSearchState();
        AccessGroupSet principals = searchRequest.getAccessGroups();
        SearchRequest facetRequest = new SearchRequest(searchState, principals, true);
        facetRequest.setApplyCutoffs(false);
        if (resultResponse.getSelectedContainer() != null) {
            SearchState facetState = (SearchState) searchState.clone();
            facetState.addFacet(resultResponse.getSelectedContainer().getPath());
            facetRequest.setSearchState(facetState);
        }

        // Retrieve the facet result set
        if (Boolean.valueOf(getFacets)) {
            SearchResultResponse resultResponseFacets = multiSelectFacetListService.getFacetListResult(facetRequest);
            parentCollectionFacetTitleService.populateTitles(resultResponseFacets.getFacetFields());
            resultResponse.setFacetFields(resultResponseFacets.getFacetFields());
        }

        return getResults(resultResponse, "search", request);
    }

    private void populateThumbnailUrls(SearchRequest searchRequest, SearchResultResponse result) {
        accessCopiesService.populateThumbnailIds(result.getResultList(),
                searchRequest.getAccessGroups(), true);
        accessCopiesService.populateThumbnailId(result.getSelectedContainer(),
                searchRequest.getAccessGroups(), true);
    }
}
