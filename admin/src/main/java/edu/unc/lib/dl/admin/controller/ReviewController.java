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
package edu.unc.lib.dl.admin.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.search.solr.model.GenericFacet;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;
import edu.unc.lib.dl.ui.util.SerializationUtil;

/**
 * Controller for the review workflow interface
 *
 * @author bbpennel
 * @date Jan 20, 2015
 */
@Controller
public class ReviewController extends AbstractSearchController {

    @RequestMapping(value = "review", method = RequestMethod.GET, produces = "text/html")
    public String getReviewList(Model model, HttpServletRequest request) {
        return "search/reviewList";
    }

    @RequestMapping(value = "review/{pid}", method = RequestMethod.GET, produces = "text/html")
    public String getReviewList(@PathVariable("pid") String pid, Model model, HttpServletRequest request) {
        return "search/reviewList";
    }

    @RequestMapping(value = "review/{pid}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody Map<String, Object> reviewJSON(@PathVariable("pid") String pid, HttpServletRequest request,
            HttpServletResponse response) {
        return getResults(getReviewRequest(pid, request));
    }

    @RequestMapping(value = "review", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody Map<String, Object> reviewJSON(HttpServletRequest request, HttpServletResponse response) {
        return getResults(getReviewRequest(null, request));
    }

    private Map<String, Object> getResults(SearchRequest searchRequest) {
        SearchResultResponse resp = getSearchResults(searchRequest);

        AccessGroupSet groups = GroupsThreadStore.getGroups();
        List<Map<String, Object>> resultList = SerializationUtil.resultsToList(resp, groups);
        Map<String, Object> results = new HashMap<>();
        results.put("metadata", resultList);

        SearchState state = resp.getSearchState();
        results.put("pageStart", state.getStartRow());
        results.put("pageRows", state.getRowsPerPage());
        results.put("resultCount", resp.getResultCount());
        results.put("searchStateUrl", SearchStateUtil.generateStateParameterString(state));
        results.put("searchQueryUrl", SearchStateUtil.generateSearchParameterString(state));
        results.put("queryMethod", "review");
        results.put("resultOperation", "review");

        long invalidVocabCount = queryLayer.getInvalidVocabularyCount(searchRequest);
        results.put("invalidVocabCount", invalidVocabCount);

        if (resp.getSelectedContainer() != null) {
            results.put("container", SerializationUtil.metadataToMap(resp.getSelectedContainer(), groups));
        }

        return results;
    }

    private SearchRequest getReviewRequest(String pid, HttpServletRequest request) {
        SearchRequest searchRequest = generateSearchRequest(request);
        searchRequest.setRootPid(pid);

        searchRequest.setApplyCutoffs(false);

        SearchState searchState = searchRequest.getSearchState();
        searchState.setIncludeParts(false);
        GenericFacet facet = new GenericFacet("STATUS", "Unpublished");
        searchState.getFacets().put("STATUS", facet);

        searchState.setPermissionLimits(Arrays.asList(Permission.publish, Permission.editDescription));

        return searchRequest;
    }
}
