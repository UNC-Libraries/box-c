package edu.unc.lib.dl.admin.controller;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;
import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;

public class SearchController extends AbstractSolrSearchController {
	private static final Logger LOG = LoggerFactory.getLogger(SearchController.class);

	@RequestMapping("/search/{pid}")
	public String search(@PathVariable("pid") String pid, Model model, HttpServletRequest request) {
		SearchRequest searchRequest = generateSearchRequest(request);
		searchRequest.setRootPid(pid);
		doSearch(searchRequest, model);
		return "search/resultList";
	}

	@RequestMapping("/search")
	public String search(Model model, HttpServletRequest request) {
		SearchRequest searchRequest = generateSearchRequest(request);
		doSearch(searchRequest, model);

		return "search/resultList";
	}

	protected void doSearch(SearchRequest searchRequest, Model model) {
		LOG.debug("Performing search");

		// Request object for the search
		SearchState responseState = (SearchState) searchRequest.getSearchState().clone();
		SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);

		if (resultResponse != null) {
			queryLayer.populateBreadcrumbs(searchRequest, resultResponse);
		}

		String searchStateUrl = SearchStateUtil.generateStateParameterString(responseState);
		model.addAttribute("searchStateUrl", searchStateUrl);
		model.addAttribute("resultResponse", resultResponse);
	}
	
	@RequestMapping("/facets/{pid}")
	public String getFacets(@PathVariable("pid") String pid, Model model, HttpServletRequest request) {
		SearchRequest searchRequest = generateSearchRequest(request);
		searchRequest.setRootPid(pid);
		
		return getFacets(searchRequest, model, searchSettings.getFacetNames());
	}

	@RequestMapping("/facets")
	public String getFacets(Model model, HttpServletRequest request) {
		SearchRequest searchRequest = generateSearchRequest(request);
		return getFacets(searchRequest, model, searchSettings.getFacetNames());
	}

	protected String getFacets(SearchRequest searchRequest, Model model, Collection<String> facetsToRetrieve) {
		LOG.debug("Retrieving facet list");
		// Retrieve the facet result set
		SearchResultResponse resultResponse = queryLayer.getFacetList(searchRequest.getSearchState(),
				searchRequest.getAccessGroups(), facetsToRetrieve, false);
		model.addAttribute("facetFields", resultResponse.getFacetFields());
		model.addAttribute("template", "ajax");
		return "/jsp/util/facetList";
	}
}
