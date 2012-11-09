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

import edu.unc.lib.dl.search.solr.exception.InvalidHierarchicalFacetException;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.service.SearchActionService;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;

import java.net.URLDecoder;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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
	@Autowired(required=true)
	private SearchActionService searchActionService;
	@Autowired
	private SearchSettings searchSettings;
	@Autowired
	protected SearchStateFactory searchStateFactory;
	
	@RequestMapping(method = RequestMethod.GET)
	public String handleRequest(Model model, HttpServletRequest request){
		SearchState searchState = null;
		try {
			request.setCharacterEncoding("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		String searchWithin = request.getParameter(searchSettings.searchStateParam("SEARCH_WITHIN"));
		
		LOG.debug("Search Within:" + searchWithin);
		if (searchWithin != null && searchWithin.length() > 0){
			try {
				searchWithin = URLDecoder.decode(searchWithin, "UTF-8");
			} catch (Exception e){
				LOG.error("Failed to decode searchWithin " + searchWithin, e);
			}
			HashMap<String,String[]> parameters = SearchStateUtil.getParametersAsHashMap(searchWithin);
			searchState = searchStateFactory.createSearchState(parameters);
			
			if (searchState.getFacets() != null && searchState.getFacets().containsKey(SearchFieldKeys.ANCESTOR_PATH)){
				Object ancestorPathObject = searchState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH);
				if (ancestorPathObject != null && ancestorPathObject instanceof CutoffFacet){
					CutoffFacet ancestorPath  = (CutoffFacet)searchState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH);
					ancestorPath.setCutoff(null);
				}
			}
		}
		
		StringBuffer actions = new StringBuffer();
		String query = request.getParameter("query");
		LOG.debug("Query:" + query);
		if (query != null){
			query = query.replaceAll(",", "%2C");
			String queryType = request.getParameter("queryType");
			if (queryType != null){
				actions.append(searchSettings.actionName("ADD_SEARCH_FIELD")).append(':')
					.append(queryType).append(',').append(query);
			}
		}

		String otherActions = request.getParameter(searchSettings.searchStateParam("ACTIONS"));
		if (otherActions != null){
			if (actions.length() > 0)
				actions.append('|');
			actions.append(otherActions);
		}
		
		if (searchState == null)
			searchState = searchStateFactory.createSearchState();
		
		LOG.debug("Actions:" + actions.toString());
		try {
			searchActionService.executeActions(searchState, actions.toString());
		} catch (InvalidHierarchicalFacetException e){
			LOG.warn("An invalid facet was provided: " + request.getQueryString(), e);
		}
		request.getSession().setAttribute("searchState", searchState);
		
		model.addAllAttributes(SearchStateUtil.generateStateParameters(searchState));

		if (request.getParameter("queryPath") != null && request.getParameter("queryPath").equals("browse"))
			return "redirect:/browse";
		return "redirect:/search";
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
