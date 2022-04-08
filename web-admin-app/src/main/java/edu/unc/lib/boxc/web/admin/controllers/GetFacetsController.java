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
package edu.unc.lib.boxc.web.admin.controllers;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.MultiSelectFacetListService;
import edu.unc.lib.boxc.search.solr.services.SetFacetTitleByIdService;
import edu.unc.lib.boxc.search.solr.utils.SearchStateUtil;

/**
 *
 * @author bbpennel
 *
 */
@Controller
public class GetFacetsController extends AbstractSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(GetFacetsController.class);

    @Autowired
    private MultiSelectFacetListService multiSelectFacetListService;
    @Autowired
    private SetFacetTitleByIdService setFacetTitleByIdService;

    @RequestMapping("/facets/{pid}")
    public String getFacets(@PathVariable("pid") String pid, Model model, HttpServletRequest request) {
        SearchRequest searchRequest = generateSearchRequest(request);
        searchRequest.setRootPid(PIDs.get(pid));

        return getFacets(searchRequest, model);
    }

    @RequestMapping("/facets")
    public String getFacets(Model model, HttpServletRequest request) {
        SearchRequest searchRequest = generateSearchRequest(request);
        return getFacets(searchRequest, model);
    }

    protected String getFacets(SearchRequest searchRequest, Model model) {
        if (searchRequest.getSearchState().getFacetsToRetrieve() == null) {
            searchRequest.getSearchState().setFacetsToRetrieve(searchSettings.getFacetNames());
        }

        searchRequest.setApplyCutoffs(false);
        searchRequest.setRetrieveFacets(true);
        LOG.debug("Retrieving facet list");
        // Retrieve the facet result set
        SearchResultResponse resultResponse = multiSelectFacetListService.getFacetListResult(searchRequest);
        setFacetTitleByIdService.populateTitles(resultResponse.getFacetFields());
        model.addAttribute("facetFields", resultResponse.getFacetFields());
        String searchStateUrl = SearchStateUtil.generateSearchParameterString(searchRequest.getSearchState());
        model.addAttribute("searchStateUrl", searchStateUrl);
        model.addAttribute("template", "ajax");
        model.addAttribute("searchState", searchRequest.getSearchState());
        model.addAttribute("queryMethod", "search");
        model.addAttribute("selectedContainer", resultResponse.getSelectedContainer());
        return "/jsp/util/facetList";
    }
}
