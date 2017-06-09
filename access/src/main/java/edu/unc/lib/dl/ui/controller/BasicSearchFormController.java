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
package edu.unc.lib.dl.ui.controller;

import javax.servlet.http.HttpServletRequest;

import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.service.SearchActionService;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.UnsupportedEncodingException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Handles search requests from basic search forms.  Can handle
 * a single search box with search type specified, as well as search within searches using
 * the previous search state stored in session, and any number of navigation actions.  Constructs
 * a new search state and sends it to the search controller.
 * @author bbpennel
 */
@Controller
@RequestMapping("/basicSearch")
public class BasicSearchFormController {
    private static final Logger LOG = LoggerFactory.getLogger(BasicSearchFormController.class);
    @Autowired(required = true)
    private SearchActionService searchActionService;
    @Autowired
    private SearchSettings searchSettings;
    @Autowired
    protected SearchStateFactory searchStateFactory;

    @RequestMapping(method = RequestMethod.GET)
    public String searchForm(@RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "queryType", required = false) String queryType,
            @RequestParam(value = "container", required = false) String container,
            @RequestParam(value = "within", required = false) String searchWithin,
            @RequestParam(value = "searchType", required = false) String searchType, Model model,
            HttpServletRequest request) {
        // Query needs to be encoded before being added into the new url
        try {
            if (query != null) {
                query = URLEncoder.encode(query, "UTF-8");
            }
        } catch (UnsupportedEncodingException e1) {
        }

        StringBuilder destination = new StringBuilder();
        if (request.getParameter("queryPath") != null && request.getParameter("queryPath").equals("structure")) {
            destination.append("redirect:/structure");
        } else {
            destination.append("redirect:/search");
        }

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

    public SearchActionService getSearchActionService() {
        return searchActionService;
    }

    public void setSearchActionService(SearchActionService searchActionService) {
        this.searchActionService = searchActionService;
    }

    public void setSearchStateFactory(SearchStateFactory searchStateFactory) {
        this.searchStateFactory = searchStateFactory;
    }
}
