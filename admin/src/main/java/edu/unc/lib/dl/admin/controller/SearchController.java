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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;
import edu.unc.lib.dl.ui.util.SerializationUtil;

/**
 *
 * @author bbpennel
 *
 */
@Controller
public class SearchController extends AbstractSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(SearchController.class);

    /**
     * Handle the input from the search form and redirect the user to a search
     * result page
     *
     * @param query
     * @param queryType
     * @param container
     * @param searchWithin
     * @param searchType
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "doSearch", produces = "text/html")
    public String searchForm(@RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "queryType", required = false) String queryType,
            @RequestParam(value = "container", required = false) String container,
            @RequestParam(value = "within", required = false) String searchWithin,
            @RequestParam(value = "searchType", required = false) String searchType) {
        return "redirect:" + getSearchString(query, queryType, container, searchWithin, searchType);
    }

    @RequestMapping(value = "doSearch", produces = "application/json")
    public @ResponseBody String searchFormJSON(@RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "queryType", required = false) String queryType,
            @RequestParam(value = "container", required = false) String container,
            @RequestParam(value = "within", required = false) String searchWithin,
            @RequestParam(value = "searchType", required = false) String searchType, HttpServletRequest request) {
        return request.getContextPath() + getSearchString(query, queryType, container, searchWithin, searchType);
    }

    public String getSearchString(String query, String queryType, String container, String searchWithin,
            String searchType) {
        // Query needs to be encoded before being added into the new url
        try {
            query = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
        }
        StringBuilder destination = new StringBuilder("/search");
        if (!"".equals(searchType) && container != null && container.length() > 0) {
            destination.append('/').append(container);
        }

        if ("within".equals(searchType) && searchWithin != null) {
            try {
                searchWithin = URLDecoder.decode(searchWithin, "UTF-8");
                HashMap<String, String[]> parameters = SearchStateUtil.getParametersAsHashMap(searchWithin);
                SearchState withinState = searchStateFactory.createSearchState(parameters);
                if (withinState.getSearchFields().size() > 0) {
                    String queryKey = searchSettings.searchFieldKey(queryType);
                    String typeValue = withinState.getSearchFields().get(queryKey);
                    if (queryKey != null) {
                        if (typeValue == null) {
                            withinState.getSearchFields().put(queryKey, query);
                        } else {
                            withinState.getSearchFields().put(queryKey, typeValue + " " + query);
                        }
                        String searchStateUrl = SearchStateUtil.generateStateParameterString(withinState);
                        destination.append('?').append(searchStateUrl);
                    }
                } else {
                    destination.append('?').append(queryType).append('=').append(query);
                    destination.append('&').append(searchWithin);
                }
            } catch (Exception e) {
                LOG.error("Failed to decode searchWithin " + searchWithin, e);
            }
        } else {
            destination.append('?').append(queryType).append('=').append(query);
        }
        return destination.toString();
    }

    /**
     * Retrieve search results, including the recursive children of the
     * currently selected container
     *
     * @param pid
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "search/{pid}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody Map<String, Object> searchJSON(@PathVariable("pid") String pid, HttpServletRequest request,
            HttpServletResponse response) {
        SearchResultResponse resultResponse = getSearchResults(getSearchRequest(pid, request));
        return getResults(resultResponse, "search", request);
    }

    @RequestMapping(value = "search", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody Map<String, Object> searchJSON(HttpServletRequest request, HttpServletResponse response) {
        SearchResultResponse resultResponse = getSearchResults(getSearchRequest(null, request));
        return getResults(resultResponse, "search", request);
    }

    @RequestMapping(value = "search", method = RequestMethod.GET)
    public String searchRoot(Model model, HttpServletRequest request) {
        return "search/resultList";
    }

    @RequestMapping(value = "search/{pid}", method = RequestMethod.GET)
    public String search(Model model, HttpServletRequest request) {
        return "search/resultList";
    }

    private SearchRequest getSearchRequest(String pid, HttpServletRequest request) {
        SearchRequest searchRequest = generateSearchRequest(request);
        searchRequest.setRootPid(pid);
        searchRequest.setApplyCutoffs(false);
        return searchRequest;
    }

    /**
     * List search results, limited to the currently selected container
     *
     * @param pid
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "list/{pid}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody Map<String, Object> listJSON(@PathVariable("pid") String pid, HttpServletRequest request,
            HttpServletResponse response) {
        SearchResultResponse resultResponse = getSearchResults(getListRequest(pid, request));
        return getResults(resultResponse, "list", request);
    }

    @RequestMapping(value = "list", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody Map<String, Object> listJSON(HttpServletRequest request, HttpServletResponse response) {
        SearchResultResponse resultResponse = getSearchResults(
                getListRequest(RepositoryPaths.getContentRootPid().toString(), request));
        return getResults(resultResponse, "list", request);
    }

    @RequestMapping(value = "list", method = RequestMethod.GET, produces = "text/html")
    public String listRootContents(Model model, HttpServletRequest request) {
        return "search/resultList";
    }

    @RequestMapping(value = "list/{pid}", method = RequestMethod.GET, produces = "text/html")
    public String list(Model model, HttpServletRequest request) {
        return "search/resultList";
    }

    private SearchRequest getListRequest(String pid, HttpServletRequest request) {
        SearchRequest searchRequest = generateSearchRequest(request);
        searchRequest.setRootPid(pid);
        return searchRequest;
    }

    private Map<String, Object> getResults(SearchResultResponse resp, String queryMethod, HttpServletRequest request) {
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
        results.put("queryMethod", queryMethod);
        results.put("onyen", GroupsThreadStore.getUsername());
        results.put("email", GroupsThreadStore.getEmail());

        if (resp.getSelectedContainer() != null) {
            results.put("container", SerializationUtil.metadataToMap(resp.getSelectedContainer(), groups));
        }

        return results;
    }
}
