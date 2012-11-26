package edu.unc.lib.dl.admin.controller;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.GenericFacet;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.security.access.AccessGroupSet;
import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;
import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;

@Controller
public class ReviewController extends AbstractSolrSearchController {
	private static final Logger log = LoggerFactory.getLogger(ReviewController.class);
	private List<String> containerFieldList = Arrays.asList(SearchFieldKeys.ID, SearchFieldKeys.TITLE, SearchFieldKeys.ANCESTOR_PATH);
	private List<String> resultsFieldList = Arrays.asList(SearchFieldKeys.ID, SearchFieldKeys.TITLE, SearchFieldKeys.CREATOR,
			SearchFieldKeys.DATASTREAM, SearchFieldKeys.DATE_ADDED);

	@RequestMapping(value = "{prefix}/{id}/review", method = RequestMethod.GET)
	public String getReviewList(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id, Model model,
			HttpServletRequest request) {
		log.debug("Reviewing " + idPrefix + id);
		String idString = idPrefix + ":" + id;
		AccessGroupSet accessGroups = getUserAccessGroups(request);
		
		// Retrieve the record for the container being reviewed
		SimpleIdRequest containerRequest = new SimpleIdRequest(idString, containerFieldList, accessGroups);
		BriefObjectMetadataBean containerBean = queryLayer.getObjectById(containerRequest);
		CutoffFacet path = containerBean.getPath();
		if (path == null) {
			log.debug("Could not find path for " + idString + " while trying to generate review list");
			throw new ResourceNotFoundException("The requested record either does not exist or is not accessible");
		}
		model.addAttribute("containerBean", containerBean);

		// Retrieve the list of unpublished (not belonging to an unpublished parent) items within this container.
		SearchState reviewListState = this.searchStateFactory.createSearchState();
		GenericFacet facet = new GenericFacet("STATUS", "Unpublished");
		reviewListState.getFacets().put("STATUS", facet);
		reviewListState.getFacets().put("ANCESTOR_PATH", path);

		reviewListState.setRowsPerPage(500);
		reviewListState.setResultFields(resultsFieldList);

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
