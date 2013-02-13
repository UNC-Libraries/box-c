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

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.GroupedMetadataBean;
import edu.unc.lib.dl.search.solr.model.MultivaluedHierarchicalFacet;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.ui.model.RecordNavigationState;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.ui.Model;
import javax.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * Controller which interprets the provided search state, from either the last search state in the session or from GET
 * parameters, as well as actions performed on the state, and retrieves search results using it.
 * 
 * @author bbpennel
 */
@Controller
@RequestMapping("/search")
public class SearchActionController extends AbstractSolrSearchController {
	private static final Logger LOG = LoggerFactory.getLogger(SearchActionController.class);

	@RequestMapping(method = RequestMethod.GET)
	public String handleSearchActions(Model model, HttpServletRequest request) {
		LOG.debug("In handle search actions");

		// Request object for the search
		SearchRequest searchRequest = generateSearchRequest(request);
		SearchState searchState = searchRequest.getSearchState();
		SearchState responseState = (SearchState) searchState.clone();

		List<String> facetsToRetrieve = searchState.getFacetsToRetrieve();
		searchState.setFacetsToRetrieve(null);

		// Determine if this is a collection browse request
		boolean isCollectionBrowseRequest = searchState.getResourceTypes() != null
				&& searchState.getResourceTypes().size() == 1
				&& searchState.getResourceTypes().contains(searchSettings.getResourceTypeCollection());

		if (searchState.getRowsPerPage() == null || searchState.getRowsPerPage() == 0) {
			if (isCollectionBrowseRequest) {
				searchState.setRowsPerPage(searchSettings.defaultCollectionsPerPage);
			} else {
				searchState.setRowsPerPage(searchSettings.defaultPerPage);
			}
		}

		Boolean rollup = searchState.getRollup();
		LOG.debug("Rollup is specified as " + rollup);

		// Get the record for the currently selected container if one is selected.
		if (searchState.getFacets().containsKey(SearchFieldKeys.ANCESTOR_PATH.name())) {
			CutoffFacet queryAncestorPath = (CutoffFacet) searchState.getFacets()
					.get(SearchFieldKeys.ANCESTOR_PATH.name());

			BriefObjectMetadataBean selectedContainer = queryLayer.getObjectById(new SimpleIdRequest(queryAncestorPath
					.getSearchKey(), searchRequest.getAccessGroups()));
			model.addAttribute("selectedContainer", selectedContainer);

			if (selectedContainer != null) {
				// Unless its explicitly set in the url, disable rollup if the container is an aggregate or its inside of an
				// aggregate.
				if (rollup == null) {
					rollup = !(!selectedContainer.getRollup().equals(selectedContainer.getId()) || (selectedContainer
							.getRollup().equals(selectedContainer.getId()) && selectedContainer.getResourceType().equals(
							"Aggregate")));
					LOG.debug("Setting the default rollup value to " + rollup);
					searchState.setRollup(rollup);
				}

				// Store the path value from the selected container as the path for breadcrumbs, making sure cutoff values
				// live on
				CutoffFacet selectedPath = selectedContainer.getPath();
				selectedPath.setCutoff(queryAncestorPath.getCutoff());
				selectedPath.setFacetCutoff(queryAncestorPath.getFacetCutoff());
				searchState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), selectedPath);
			}
		} else if (rollup == null) {
			LOG.debug("No container and no rollup, defaulting rollup to true");
			searchState.setRollup(true);
		}

		// Retrieve search results
		SearchResultResponse resultResponse = queryLayer.getSearchResults(searchRequest);

		if (resultResponse != null) {
			// Retrieve the facet result set
			SearchResultResponse resultResponseFacets = queryLayer.getFacetList(searchState,
					searchRequest.getAccessGroups(), facetsToRetrieve, false);

			// If the users query had no results but the facet query did have results, then if a path is set remove its
			// cutoff and rerun
			if (resultResponseFacets.getResultCount() > 0 && resultResponse.getResultCount() == 0
					&& searchState.getFacets() != null
					&& searchState.getFacets().containsKey(SearchFieldKeys.ANCESTOR_PATH.name())) {
				CutoffFacet ancestorPath = ((CutoffFacet) searchState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH.name()));
				if (ancestorPath.getCutoff() != null) {
					ancestorPath.setCutoff(null);
					resultResponse = queryLayer.getSearchResults(searchRequest);
				}
			}

			resultResponse.setFacetFields(resultResponseFacets.getFacetFields());

			// Add the search state to the response.
			resultResponse.setSearchState(searchState);
		}

		// Use a representative content type value if there are any results.
		// This saves a trip to Solr since we already have the full content type facet needed for the facet list inside of
		// the results
		if (searchState.getFacets().containsKey(SearchFieldKeys.CONTENT_TYPE.name())
				&& resultResponse.getResultCount() > 0) {
			extractCrumbDisplayValueFromRepresentative(searchState, resultResponse.getResultList().get(0));
		}

		// Get the children counts for container entries.
		queryLayer.getChildrenCounts(resultResponse.getResultList(), searchRequest.getAccessGroups());

		// Determine if this is a collection browse or search results page and inform the view.
		if (isCollectionBrowseRequest) {
			model.addAttribute("menuId", "browse");
			model.addAttribute("resultType", "collectionBrowse");
			model.addAttribute("pageSubtitle", "Browse Collections");
		} else {
			model.addAttribute("resultType", "searchResults");
			model.addAttribute("pageSubtitle", "Search Results");
		}

		String searchStateUrl = SearchStateUtil.generateStateParameterString(responseState);
		model.addAttribute("searchStateUrl", searchStateUrl);
		model.addAttribute("userAccessGroups", searchRequest.getAccessGroups());
		model.addAttribute("resultResponse", resultResponse);

		LOG.debug("SAC qs: " + request.getQueryString());

		// Setup parameters for full record navigation
		RecordNavigationState recordNavigationState = new RecordNavigationState();
		recordNavigationState.setSearchState(responseState);
		recordNavigationState.setSearchStateUrl(searchStateUrl);

		recordNavigationState.setRecordIdList(resultResponse.getIdList());
		recordNavigationState.setTotalResults(resultResponse.getResultCount());

		request.getSession().setAttribute("recordNavigationState", recordNavigationState);

		LOG.debug("SAC state: " + searchStateUrl);

		return "searchResults";
	}

	private void extractCrumbDisplayValueFromRepresentative(SearchState searchState, BriefObjectMetadata representative) {
		Object contentTypeValue = searchState.getFacets().get(SearchFieldKeys.CONTENT_TYPE.name());
		if (contentTypeValue instanceof MultivaluedHierarchicalFacet) {
			LOG.debug("Replacing content type search value "
					+ searchState.getFacets().get(SearchFieldKeys.CONTENT_TYPE.name()));
			MultivaluedHierarchicalFacet repFacet = null;
			// If we're dealing with a rolled up result then hunt through all its items to find the matching content
			// type
			if (representative instanceof GroupedMetadataBean) {
				GroupedMetadataBean groupRep = (GroupedMetadataBean) representative;

				int i = 0;
				do {
					representative = groupRep.getItems().get(i);

					if (representative.getContentTypeFacet() != null) {
						repFacet = representative.getContentTypeFacet().get(0);
						LOG.debug("Pulling content type from representative " + representative.getId() + ": " + repFacet);
						if (repFacet.contains(((MultivaluedHierarchicalFacet) contentTypeValue))) {
							break;
						} else {
							repFacet = null;
						}
					}
				} while (++i < groupRep.getItems().size());
			} else {
				// If its not a rolled up result, take it easy
				repFacet = representative.getContentTypeFacet().get(0);
			}

			if (repFacet != null) {
				((MultivaluedHierarchicalFacet) contentTypeValue).setDisplayValues(repFacet);
				searchState.getFacets().put(SearchFieldKeys.CONTENT_TYPE.name(), contentTypeValue);
			}
		}
	}
}
