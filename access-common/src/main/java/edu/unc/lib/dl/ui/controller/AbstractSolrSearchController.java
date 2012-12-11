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
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.search.solr.exception.InvalidHierarchicalFacetException;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.service.SearchActionService;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseRequest;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;

/**
 * Abstract base class for controllers which interact with solr services.
 * @author bbpennel
 */
public abstract class AbstractSolrSearchController {
	private final Logger LOG = LoggerFactory.getLogger(AbstractSolrSearchController.class);
	
	@Autowired(required=true)
	protected SolrQueryLayerService queryLayer;
	//@Autowired(required=true)
	//protected SearchStateValidator briefSearchRequestValidator;
	@Autowired(required=true)
	protected SearchActionService searchActionService;
	@Autowired
	protected SearchSettings searchSettings;
	@Autowired
	protected SearchStateFactory searchStateFactory;
	
	protected SearchRequest generateSearchRequest(HttpServletRequest request){
		return this.generateSearchRequest(request, null, new SearchRequest());
	}
	
	protected SearchRequest generateSearchRequest(HttpServletRequest request, SearchState searchState){
		return this.generateSearchRequest(request, searchState, new SearchRequest());
	}
	
	/**
	 * Builds a search request model object from the provided http servlet request and the provided 
	 * search state.  If the search state is null, then it will attempt to retrieve it from first
	 * the session and if that fails, then from current GET parameters.  Validates the search state
	 * and applies any actions provided as well.
	 * @param request
	 * @return
	 */
	protected SearchRequest generateSearchRequest(HttpServletRequest request, SearchState searchState, SearchRequest searchRequest){
		
		//Get user access groups.  Fill this in later, for now just set to public
		HttpSession session = request.getSession();
		//Get the access group list
		AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
		searchRequest.setAccessGroups(accessGroups);
		
		//Retrieve the last search state
		if (searchState == null){
			searchState = (SearchState)session.getAttribute("searchState");
			if (searchState == null){
				if (searchRequest != null && searchRequest instanceof HierarchicalBrowseRequest){
					searchState = searchStateFactory.createHierarchicalBrowseSearchState(request.getParameterMap());
				} else {
					String resourceTypes = request.getParameter(searchSettings.searchStateParam("RESOURCE_TYPES"));
					if (resourceTypes == null || resourceTypes.contains(searchSettings.resourceTypeFile)){
						searchState = searchStateFactory.createSearchState(request.getParameterMap());
					} else {
						searchState = searchStateFactory.createCollectionBrowseSearchState(request.getParameterMap());
					}
				}
			} else {
				session.removeAttribute("searchState");
			}
		}
		
		//Perform actions on search state
		String actionsParam = request.getParameter(searchSettings.searchStateParams.get("ACTIONS"));
		if (actionsParam != null){
			try {
				searchActionService.executeActions(searchState, actionsParam);
			} catch (InvalidHierarchicalFacetException e){
				LOG.warn("An invalid facet was provided: " + request.getQueryString(), e);
			}
		}
		
		//Validate the search state to make sure that it contains appropriate values and field names
		//briefSearchRequestValidator.validate(searchState);
		
		//Store the search state into the search request
		searchRequest.setSearchState(searchState);
		
		return searchRequest;
	}
	
	protected SearchResultResponse getSearchResults(SearchRequest searchRequest){
		return queryLayer.getSearchResults(searchRequest);
	}

	public SearchActionService getSearchActionService() {
		return searchActionService;
	}

	public void setSearchActionService(SearchActionService searchActionService) {
		this.searchActionService = searchActionService;
	}

	public SolrQueryLayerService getQueryLayer() {
		return queryLayer;
	}

	public void setQueryLayer(SolrQueryLayerService queryLayer) {
		this.queryLayer = queryLayer;
	}

	public void setSearchStateFactory(SearchStateFactory searchStateFactory) {
		this.searchStateFactory = searchStateFactory;
	}
}
