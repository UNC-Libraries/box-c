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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.dl.search.solr.model.GenericFacet;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;

@Controller
public class ReviewController extends AbstractSearchController {

	@RequestMapping(value = "review", method = RequestMethod.GET)
	public String getReviewList(Model model, HttpServletRequest request) {
		SearchRequest searchRequest = generateSearchRequest(request);
		searchRequest.setRootPid(collectionsPid.getPid());
		doReviewList(searchRequest, model, request);

		return "search/resultList";
	}

	@RequestMapping(value = "review/{pid}", method = RequestMethod.GET)
	public String getReviewList(@PathVariable("pid") String pid, Model model, HttpServletRequest request) {
		SearchRequest searchRequest = generateSearchRequest(request);
		searchRequest.setRootPid(pid);

		doReviewList(searchRequest, model, request);

		return "search/resultList";
	}

	private void doReviewList(SearchRequest searchRequest, Model model, HttpServletRequest request) {
		SearchState responseState = (SearchState) searchRequest.getSearchState().clone();
		
		searchRequest.setApplyCutoffs(false);
		SearchState searchState = (SearchState) searchRequest.getSearchState();
		
		GenericFacet facet = new GenericFacet("STATUS", "Unpublished");
		searchState.getFacets().put("STATUS", facet);

		SearchResultResponse resultResponse = getSearchResults(searchRequest);

		String searchStateUrl = SearchStateUtil.generateStateParameterString(responseState);
		model.addAttribute("searchStateUrl", searchStateUrl);
		model.addAttribute("resultResponse", resultResponse);
		model.addAttribute("queryMethod", "review");
		request.getSession().setAttribute("resultOperation", "review");
	}
}
