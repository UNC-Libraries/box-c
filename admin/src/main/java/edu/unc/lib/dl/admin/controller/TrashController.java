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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;
import edu.unc.lib.dl.ui.util.SerializationUtil;

/**
 * 
 * @author bbpennel
 *
 */
@Controller
public class TrashController extends AbstractSearchController {
    protected static List<String> resultsFieldList = Arrays.asList(SearchFieldKeys.ID.name(),
            SearchFieldKeys.TITLE.name(), SearchFieldKeys.DATE_UPDATED.name(), SearchFieldKeys.RESOURCE_TYPE.name(),
            SearchFieldKeys.CONTENT_MODEL.name(), SearchFieldKeys.STATUS.name(), SearchFieldKeys.ANCESTOR_PATH.name(),
            SearchFieldKeys.VERSION.name(), SearchFieldKeys.ROLE_GROUP.name(), SearchFieldKeys.RELATIONS.name());

    @RequestMapping(value = "trash", produces = "text/html")
    public String trashForEverything() {
        return "search/trashList";
    }

    @RequestMapping(value = "trash/{pid}", produces = "text/html")
    public String trashForContainer() {
        return "search/trashList";
    }

    @RequestMapping(value = "trash/{pid}", produces = "application/json")
    public @ResponseBody Map<String, Object> trashJSON(@PathVariable("pid") String pid, HttpServletRequest request,
            HttpServletResponse response) {
        return getResults(getRequest(pid, request));
    }

    @RequestMapping(value = "trash", produces = "application/json")
    public @ResponseBody Map<String, Object> trashJSON(HttpServletRequest request, HttpServletResponse response) {
        return getResults(getRequest(null, request));
    }

    private SearchRequest getRequest(String pid, HttpServletRequest request) {
        SearchRequest searchRequest = generateSearchRequest(request);
        searchRequest.setRootPid(pid);

        SearchState state = searchRequest.getSearchState();
        state.setRowsPerPage(searchSettings.maxPerPage);
        // Force deleted status into the request
        state.getFacets().put(SearchFieldKeys.STATUS.name(), "Deleted");

        return searchRequest;
    }

    private Map<String, Object> getResults(SearchRequest searchRequest) {
        searchRequest.getSearchState().setRowsPerPage(searchSettings.maxPerPage);
        // Force deleted status into the request
        searchRequest.getSearchState().getFacets().put(SearchFieldKeys.STATUS.name(), "Deleted");

        SearchResultResponse resp = getSearchResults(searchRequest, resultsFieldList);

        AccessGroupSet groups = GroupsThreadStore.getGroups();
        List<Map<String, Object>> resultList = SerializationUtil.resultsToList(resp, groups);
        Map<String, Object> results = new HashMap<>();
        results.put("metadata", resultList);

        SearchState state = resp.getSearchState();
        results.put("pageStart", state.getStartRow());
        results.put("resultCount", resp.getResultCount());
        results.put("searchStateUrl", SearchStateUtil.generateStateParameterString(state));
        results.put("searchQueryUrl", SearchStateUtil.generateSearchParameterString(state));
        results.put("queryMethod", "trash");
        results.put("resultOperation", "trash");

        if (resp.getSelectedContainer() != null) {
            results.put("container", SerializationUtil.metadataToMap(resp.getSelectedContainer(), groups));
        }

        return results;
    }
}
