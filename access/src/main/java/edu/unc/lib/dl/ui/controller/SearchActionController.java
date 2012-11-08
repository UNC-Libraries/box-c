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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.HierarchicalFacetNode;
import edu.unc.lib.dl.search.solr.model.MultivaluedHierarchicalFacet;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.ui.model.RecordNavigationState;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.HierarchicalFacet;
import edu.unc.lib.dl.ui.validator.DatastreamAccessValidator;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.ui.Model;
import javax.servlet.http.HttpServletRequest;

import java.util.Iterator;
import java.util.List;

/**
 * Controller which interprets the provided search state, from either the last search state in the session or
 * from GET parameters, as well as actions performed on the state, and retrieves search results using it.
 * @author bbpennel
 */
@Controller
@RequestMapping("/search")
public class SearchActionController extends AbstractSolrSearchController {
	private static final Logger LOG = LoggerFactory.getLogger(SearchActionController.class);
	
	@RequestMapping(method = RequestMethod.GET)
	public String handleSearchActions(Model model, HttpServletRequest request){
		LOG.debug("In handle search actions");
		
		//Request object for the search
		SearchRequest searchRequest = generateSearchRequest(request);
		SearchState searchState = searchRequest.getSearchState();
		List<String> facetsToRetrieve = searchState.getFacetsToRetrieve();
		searchState.setFacetsToRetrieve(null);
		
		//Determine if this is a collection browse request
		boolean isCollectionBrowseRequest = searchState.getResourceTypes() != null 
				&& searchState.getResourceTypes().size() == 1 
				&& searchState.getResourceTypes().contains(searchSettings.getResourceTypeCollection());
		
		if (searchState.getRowsPerPage() == null || searchState.getRowsPerPage() == 0){
			if (isCollectionBrowseRequest){
				searchState.setRowsPerPage(searchSettings.defaultCollectionsPerPage);
			} else {
				searchState.setRowsPerPage(searchSettings.defaultPerPage);
			}
		}
		
		//Retrieve search results
		SearchResultResponse resultResponse = queryLayer.getSearchResults(searchRequest);
		
		if (resultResponse != null){
			//Get the display values for hierarchical facets from the search results.
			//queryLayer.lookupHierarchicalDisplayValues(searchState, searchRequest.getAccessGroups());
			
			//Retrieve the facet result set
			SearchResultResponse resultResponseFacets = queryLayer.getFacetList(searchState, searchRequest.getAccessGroups(), facetsToRetrieve, false);
			
			//If the users query had no results but the facet query did have results, then if a path is set remove its cutoff and rerun
			if (resultResponseFacets.getResultCount() > 0 && resultResponse.getResultCount() == 0 
					&& searchState.getFacets() != null && searchState.getFacets().containsKey(SearchFieldKeys.ANCESTOR_PATH)){
				CutoffFacet ancestorPath = ((CutoffFacet)searchState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH));
				if (ancestorPath.getCutoff() != null){
					ancestorPath.setCutoff(null);
					resultResponse = queryLayer.getSearchResults(searchRequest);
				}
			}
			
			resultResponse.setFacetFields(resultResponseFacets.getFacetFields());
			
			//Add the search state to the response.
			resultResponse.setSearchState(searchState);
			
			//Filter the datastreams in the response according to the users permissions
			DatastreamAccessValidator.filterSearchResult(resultResponse, searchRequest.getAccessGroups());
		}
		
		//Get the record for the currently selected container if one is selected.
		if (searchState.getFacets().containsKey(SearchFieldKeys.ANCESTOR_PATH)){
			BriefObjectMetadataBean selectedContainer = queryLayer.getObjectById(new SimpleIdRequest(
					((CutoffFacet)searchState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH)).getSearchKey(),
					searchRequest.getAccessGroups()));
			model.addAttribute("selectedContainer", selectedContainer);
			
			// Store the path value from the selected container as the path for breadcrumbs
			searchState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH, selectedContainer.getPath());
		}
		
		// Use a representative content type value if there are any results.
		if (searchState.getFacets().containsKey(SearchFieldKeys.CONTENT_TYPE) && resultResponse.getResultCount() > 0){
			Object contentTypeValue = searchState.getFacets().get(SearchFieldKeys.CONTENT_TYPE);
			if (contentTypeValue instanceof MultivaluedHierarchicalFacet) {
				LOG.debug("Replacing content type search value " + searchState.getFacets().get(SearchFieldKeys.CONTENT_TYPE));
				BriefObjectMetadataBean representative = resultResponse.getResultList().get(0);
				MultivaluedHierarchicalFacet repFacet = representative.getContentTypeFacet().get(0);
				((MultivaluedHierarchicalFacet)contentTypeValue).setDisplayValues(repFacet);
				
				for (HierarchicalFacetNode node: repFacet.getFacetNodes()) {
					LOG.debug("rep:" + node.getSearchKey() + "|" + node.getDisplayValue());
				}
				
				for (HierarchicalFacetNode node: ((MultivaluedHierarchicalFacet)contentTypeValue).getFacetNodes()) {
					LOG.debug("search:" + node.getSearchKey() + "|" + node.getDisplayValue());
				}
				
				searchState.getFacets().put(SearchFieldKeys.CONTENT_TYPE, contentTypeValue);
			}
			
		}
		
		//Get the children counts for container entries.
		queryLayer.getChildrenCounts(resultResponse.getResultList(), searchRequest.getAccessGroups());
		
		//Determine if this is a collection browse or search results page and inform the view.
		if (isCollectionBrowseRequest){
			model.addAttribute("resultType", "collectionBrowse");
			model.addAttribute("pageSubtitle", "Browse Collections");
		} else {
			model.addAttribute("resultType", "searchResults");
			model.addAttribute("pageSubtitle", "Search Results");
		}
		
		String searchStateUrl = SearchStateUtil.generateStateParameterString(searchState);
		model.addAttribute("searchStateUrl", searchStateUrl);
		model.addAttribute("userAccessGroups", searchRequest.getAccessGroups());
		model.addAttribute("resultResponse", resultResponse);
		
		LOG.debug("SAC qs: " + request.getQueryString());
		
		//Setup parameters for full record navigation
		RecordNavigationState recordNavigationState = new RecordNavigationState();
		recordNavigationState.setSearchState(searchState);
		recordNavigationState.setSearchStateUrl(searchStateUrl);

		recordNavigationState.setRecordIdList(resultResponse.getIdList());
		recordNavigationState.setTotalResults(resultResponse.getResultCount());
		
		request.getSession().setAttribute("recordNavigationState", recordNavigationState);
		
		LOG.debug("SAC state: " + searchStateUrl);
		
		return "searchResults";
	}
}
