package edu.unc.lib.dl.admin.controller;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.GenericFacet;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.security.access.AccessGroupSet;
import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;
import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;

@Controller
@RequestMapping(value = { "/review*", "/review" })
public class ReviewController extends AbstractSolrSearchController {
	private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

	@RequestMapping(value = "{prefix}/{id}", method = RequestMethod.GET)
	public String getReviewList(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id, Model model,
			HttpServletRequest request) {
		log.debug("two path " + idPrefix + id);
		String idString = idPrefix + ":" + id;
		AccessGroupSet accessGroups = getUserAccessGroups(request);
		CutoffFacet path = queryLayer.getAncestorPath(idString, accessGroups);
		if (path == null) {
			log.debug("Could not find ancestor path for " + idString + " while trying to generate review list");
			throw new ResourceNotFoundException("The requested record either does not exist or is not accessible");
		}
		model.addAttribute("pathCrumb", path);

		SearchState reviewListState = this.searchStateFactory.createSearchState();
		GenericFacet facet = new GenericFacet("STATUS", "Unpublished");
		reviewListState.getFacets().put("STATUS", facet);
		reviewListState.getFacets().put("ANCESTOR_PATH", path);

		reviewListState.setRowsPerPage(500);
		reviewListState.setResultFields(Arrays.asList(SearchFieldKeys.ID, SearchFieldKeys.TITLE, SearchFieldKeys.CREATOR,
				SearchFieldKeys.DATASTREAM, SearchFieldKeys.DATE_ADDED));

		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setAccessGroups(accessGroups);
		searchRequest.setSearchState(reviewListState);

		SearchResultResponse resultResponse = queryLayer.getSearchResults(searchRequest);
		log.debug("Retrieved " + resultResponse.getResultCount() + " results for the review list");
		model.addAttribute("userAccessGroups", searchRequest.getAccessGroups());
		model.addAttribute("resultResponse", resultResponse);

		return "search/reviewList";
		// return "dashboard/reviewer";
	}
}
