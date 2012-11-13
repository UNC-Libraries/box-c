package edu.unc.lib.dl.admin.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.dl.search.solr.controller.AbstractSolrSearchController;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.security.access.AccessGroupSet;

@Controller
@RequestMapping("/")
public class DashboardController extends AbstractSolrSearchController {
	@RequestMapping(method = RequestMethod.GET)
	public String handleRequest(Model model, HttpServletRequest request){
		
		SearchState collectionsState = this.searchStateFactory.createSearchState();
		collectionsState.setResourceTypes(searchSettings.defaultCollectionResourceTypes);
		
		AccessGroupSet accessGroups = getUserAccessGroups(request);
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setAccessGroups(accessGroups);
		searchRequest.setSearchState(collectionsState);
		
		SearchResultResponse resultResponse = queryLayer.getSearchResults(searchRequest);

		model.addAttribute("userAccessGroups", searchRequest.getAccessGroups());
		model.addAttribute("resultResponse", resultResponse);
		
		return "reviewer_dashboard";
	}
}
