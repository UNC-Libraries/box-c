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

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;

@Controller
@RequestMapping(value = {"/", ""})
public class DashboardController extends AbstractSolrSearchController {
	@RequestMapping(method = RequestMethod.GET)
	public String handleRequest(Model model, HttpServletRequest request){
		
		SearchState collectionsState = this.searchStateFactory.createSearchState();
		collectionsState.setResourceTypes(searchSettings.defaultCollectionResourceTypes);
		
		AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setAccessGroups(accessGroups);
		searchRequest.setSearchState(collectionsState);
		
		SearchResultResponse resultResponse = queryLayer.getSearchResults(searchRequest);
		// Get children counts
		queryLayer.getChildrenCounts(resultResponse.getResultList(), searchRequest.getAccessGroups());
		// Get unpublished counts
		queryLayer.getChildrenCounts(resultResponse.getResultList(), searchRequest.getAccessGroups(), "unpublished", "status:Unpublished", null);

		model.addAttribute("resultResponse", resultResponse);
		
		return "dashboard/reviewer";
	}
}
