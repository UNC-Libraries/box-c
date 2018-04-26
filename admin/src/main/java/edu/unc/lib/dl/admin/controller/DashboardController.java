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

import static edu.unc.lib.dl.acl.util.GroupsThreadStore.getAgentPrincipals;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.service.ChildrenCountService;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;

/**
 *
 * @author bbpennel
 *
 */
@Controller
@RequestMapping(value = {"/", ""})
public class DashboardController extends AbstractSolrSearchController {
    private static final int ROWS_PER_PAGE = 5000;
    private static final int FACET_CUTOFF = 2;
    private static final String FACET_STRING = "1,*";

    @Autowired
    private ChildrenCountService childrenCountService;

    @RequestMapping(method = RequestMethod.GET)
    public String handleRequest(Model model, HttpServletRequest request) {
        SearchState collectionsState = this.searchStateFactory.createSearchState();
        collectionsState.setResourceTypes(searchSettings.defaultCollectionResourceTypes);
        collectionsState.setRowsPerPage(ROWS_PER_PAGE);
        CutoffFacet depthFacet = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH.name(), FACET_STRING);
        depthFacet.setCutoff(FACET_CUTOFF);
        collectionsState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), depthFacet);

        AccessGroupSet accessGroups = getAgentPrincipals().getPrincipals();
        SearchRequest searchRequest = new SearchRequest(collectionsState, accessGroups);

        SearchResultResponse resultResponse = queryLayer.getSearchResults(searchRequest);
        // Get children counts
        childrenCountService.addChildrenCounts(resultResponse.getResultList(), searchRequest.getAccessGroups());

        // Get unpublished counts
        StringBuilder reviewFilter = new StringBuilder("isPart:false AND status:Unpublished AND roleGroup:");
        reviewFilter.append(SolrQueryLayerService.getWriteRoleFilter(GroupsThreadStore.getGroups()));
        SolrQuery unpublishedQuery = new SolrQuery();
        unpublishedQuery.setQuery(reviewFilter.toString());

        childrenCountService.addChildrenCounts(resultResponse.getResultList(), searchRequest.getAccessGroups(),
                "unpublished", unpublishedQuery);

        model.addAttribute("resultResponse", resultResponse);

        return "dashboard/reviewer";
    }
}
