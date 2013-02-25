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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;
import edu.unc.lib.dl.ui.model.RecordNavigationState;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseRequest;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;

/**
 * Handles requests for the heirarchical structure view browse view.  The request may
 * either be for an entire stand alone view, or if the ajax option is true then a portion of
 * the tree starting from the room node.  The request can specify the max depth in terms of 
 * nodes in the tree it will return, where the max depth is limited by the application wide 
 * structured depth property. 
 * @author bbpennel
 */
@Controller
public class HierarchicalBrowseController extends AbstractSolrSearchController {
	private static final Logger LOG = LoggerFactory.getLogger(HierarchicalBrowseController.class);
	
	@RequestMapping("/browseChildren")
	public String getItemChildren(Model model, HttpServletRequest request, @RequestParam("tier") String tier) {
		LOG.debug("In browse children controller for " + tier);
		SearchRequest browseRequest = new SearchRequest();
		generateSearchRequest(request, null, browseRequest);
		
		CutoffFacet path = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH.name(), tier);
		browseRequest.getSearchState().getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), path);
		
		HierarchicalBrowseResultResponse resultResponse = queryLayer.getHierarchicalBrowseItemResult(browseRequest);
		model.addAttribute("hierarchicalViewResults", resultResponse);
		
		String searchStateUrl = SearchStateUtil.generateStateParameterString(browseRequest.getSearchState());
		model.addAttribute("searchStateUrl", searchStateUrl);
		
		model.addAttribute("template", "ajax");
		return "browseResults/singleTierBrowse";
	}
	
	@RequestMapping(value = "/browse", method = RequestMethod.GET)
	public String handleGet(Model model, HttpServletRequest request){
		LOG.debug("In Hierarchical Browse controller");
		
		boolean ajaxRequest = request.getParameter("ajax") != null && request.getParameter("ajax").equals("true");
		
		int depth = searchSettings.structuredDepthDefault;
		try {
			depth = Integer.parseInt(request.getParameter("depth"));
			if (depth > searchSettings.structuredDepthMax)
				depth = searchSettings.structuredDepthMax;
		} catch (Exception e){
			//Ignore and use the default depth.
		}
		
		//Request object for the search
		HierarchicalBrowseRequest browseRequest = new HierarchicalBrowseRequest(depth);
		generateSearchRequest(request, null, browseRequest);
		SearchState searchState = browseRequest.getSearchState();
		
		if (!searchState.getFacets().containsKey(SearchFieldKeys.ANCESTOR_PATH.name())){
			browseRequest.setRetrievalDepth(1);
		}
		
		HierarchicalBrowseResultResponse resultResponse = null;
		resultResponse = queryLayer.getHierarchicalBrowseResults(browseRequest);
		
		if (resultResponse != null){
			//Get the display values for hierarchical facets from the search results.
			if (!ajaxRequest){
				queryLayer.lookupHierarchicalDisplayValues(searchState, browseRequest.getAccessGroups());
			
				//Retrieve the facet result set
				SearchResultResponse resultResponseFacets = queryLayer.getFacetList(searchState, browseRequest.getAccessGroups(), searchState.getFacetsToRetrieve(), false);
				resultResponse.setFacetFields(resultResponseFacets.getFacetFields());
			}
			//Add the search state to the response.
			resultResponse.setSearchState(searchState);
		}
		
		model.addAttribute("resultType", "hierarchicalBrowse");
		model.addAttribute("pageSubtitle", "Browse Results");
		
		String searchStateUrl = SearchStateUtil.generateStateParameterString(searchState);
		model.addAttribute("searchStateUrl", searchStateUrl);
		model.addAttribute("resultResponse", resultResponse);
		model.addAttribute("hierarchicalViewResults", resultResponse);
		
		if (!ajaxRequest){
			//Setup parameters for full record navigation
			RecordNavigationState recordNavigationState = new RecordNavigationState();
			recordNavigationState.setSearchState(searchState);
			recordNavigationState.setSearchStateUrl(searchStateUrl);

			recordNavigationState.setRecordIdList(resultResponse.getIdList());
			recordNavigationState.setTotalResults(resultResponse.getResultCount());
			
			request.getSession().setAttribute("recordNavigationState", recordNavigationState);
		} else {
			model.addAttribute("template", "ajax");
			return "browseResults/singleTierBrowse";
		}
		
		return "browseResults";
	}
}
