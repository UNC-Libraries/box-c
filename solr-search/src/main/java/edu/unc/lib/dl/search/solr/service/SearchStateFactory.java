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
package edu.unc.lib.dl.search.solr.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.FacetFieldFactory;
import edu.unc.lib.dl.search.solr.model.MultivaluedHierarchicalFacet;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchSettings;

/**
 * Factory which generates SearchState objects.
 * @author bbpennel
 */
public class SearchStateFactory {
	private static final Logger log = LoggerFactory.getLogger(SolrSearchService.class);
	private SearchSettings searchSettings;
	@Autowired
	private FacetFieldFactory facetFieldFactory;
	
	public SearchStateFactory(){
		
	}
	
	/**
	 * Creates and returns a SearchState object representing the default search state
	 * for a blank search.
	 * @return
	 */
	public SearchState createSearchState(){
		SearchState searchState = new SearchState();
		
		searchState.setBaseFacetLimit(searchSettings.facetsPerGroup);
		searchState.setResourceTypes(searchSettings.defaultResourceTypes);
		searchState.setSearchTermOperator(searchSettings.defaultOperator);
		searchState.setRowsPerPage(searchSettings.defaultPerPage);
		searchState.setFacetsToRetrieve(new ArrayList<String>(searchSettings.facetNames));
		searchState.setStartRow(0);
		searchState.setSortType("default");
		searchState.setSortOrder(searchSettings.sortNormal);
		searchState.setAccessTypeFilter(null);
		return searchState;
	}
	
	/**
	 * Creates and returns a SearchState object starting from the default options for a 
	 * collection browse search, and then populating it with the search state. 
	 * from the http request.
	 * @param request
	 * @return SearchState object containing the search state for a collection browse
	 */
	public SearchState createCollectionBrowseSearchState(Map<String,String[]> request){
		SearchState searchState = createSearchState();

		searchState.setRowsPerPage(searchSettings.defaultCollectionsPerPage);
		searchState.setResourceTypes(new ArrayList<String>(searchSettings.defaultCollectionResourceTypes));
		searchState.setFacetsToRetrieve(new ArrayList<String>(searchSettings.collectionBrowseFacetNames));
		
		populateSearchState(searchState, request);
		return searchState;
	}
	
	/**
	 * Creates and returns a SearchState object starting from the default options for a 
	 * normal search, and then populating it with the search state. 
	 * from the http request.
	 * @param request
	 * @return SearchState object containing the search state
	 */	
	public SearchState createSearchState(Map<String,String[]> request){
		SearchState searchState = createSearchState();
		populateSearchState(searchState, request);
		
		return searchState;
	}
	
	/**
	 * Returns a search state object for an advanced search request.
	 * @param request
	 * @return
	 */
	public SearchState createSearchStateAdvancedSearch(Map<String,String[]> request){
		SearchState searchState = createSearchState();
		populateSearchStateAdvancedSearch(searchState, request);
		
		return searchState;
	}
	
	/**
	 * Returns a search state for a result set of only identifiers.
	 * @return
	 */
	public SearchState createIDSearchState(){
		SearchState searchState = new SearchState();
		
		List<String> resultFields = new ArrayList<String>();
		resultFields.add(SearchFieldKeys.ID);
		searchState.setResultFields(resultFields);
		
		searchState.setSearchTermOperator(searchSettings.defaultOperator);
		searchState.setRowsPerPage(searchSettings.defaultPerPage);
		searchState.setFacetsToRetrieve(null);
		searchState.setStartRow(0);
		searchState.setAccessTypeFilter(null);
		return searchState;
	}
	
	/**
	 * Returns a search state for a result set of titles and identifiers.
	 * @return
	 */
	public SearchState createTitleListSearchState(){
		SearchState searchState = createIDSearchState();
		searchState.getResultFields().add(SearchFieldKeys.TITLE);
		return searchState;
	}
	
	/**
	 * Returns a search state for results listing the containers within a hierarchy. 
	 * @return
	 */
	public SearchState createHierarchyListSearchState(){
		SearchState searchState = createIDSearchState();
		searchState.getResultFields().add(SearchFieldKeys.TITLE);
		searchState.getResultFields().add(SearchFieldKeys.ANCESTOR_PATH);
		searchState.getResultFields().add(SearchFieldKeys.ANCESTOR_NAMES);
		searchState.getResultFields().add(SearchFieldKeys.RESOURCE_TYPE);
		
		List<String> containerTypes = new ArrayList<String>();
		containerTypes.add(searchSettings.resourceTypeCollection);
		containerTypes.add(searchSettings.resourceTypeFolder);
		containerTypes.add(searchSettings.resourceTypeAggregate);
		searchState.setResourceTypes(containerTypes);
		
		searchState.setSortType("collection");
		searchState.setSortOrder(searchSettings.sortNormal);
		
		return searchState;
	}
	
	/**
	 * Returns a search state representing the default navigation search state for a hierarchical
	 * structure browse request.
	 * @return
	 */
	public SearchState createHierarchicalBrowseSearchState(){
		SearchState searchState = new SearchState();
		
		searchState.setBaseFacetLimit(searchSettings.facetsPerGroup);
		searchState.setResourceTypes(searchSettings.defaultResourceTypes);
		searchState.setSearchTermOperator(searchSettings.defaultOperator);
		searchState.setRowsPerPage(searchSettings.defaultPerPage);
		searchState.setStartRow(0);
		
		searchState.setSortType("collection");
		searchState.setSortOrder(searchSettings.sortNormal);
		
		searchState.setFacetsToRetrieve(new ArrayList<String>(searchSettings.facetNamesStructureBrowse));
		
		return searchState;
	}
	
	/**
	 * Returns a search state representing the navigation search state for a hierarchical structure
	 * browse request with the users previously existing search state overlayed.
	 * @param request
	 * @return
	 */
	public SearchState createHierarchicalBrowseSearchState(Map<String,String[]> request){
		SearchState searchState = createHierarchicalBrowseSearchState();
		
		populateSearchState(searchState, request);
		
		return searchState;
	}
	
	/**
	 * Returns a search state usable for looking up all facet values for the facet field 
	 * specified.  A base value may be given for the facet being queried, for use in 
	 * querying specific tiers in a hierarchical facet.
	 * @param facetField
	 * @param baseValue
	 * @return
	 */
	public SearchState createFacetSearchState(String facetField, String facetSort, int maxResults){
		SearchState searchState = new SearchState();
		
		searchState.setResourceTypes(searchSettings.defaultResourceTypes);
		searchState.setRowsPerPage(0);
		searchState.setStartRow(0);
		searchState.setAccessTypeFilter(null);
		
		ArrayList<String> facetList = new ArrayList<String>();
		facetList.add(facetField);
		searchState.setFacetsToRetrieve(facetList);
		
		if (facetSort != null){
			HashMap<String,String> facetSorts = new HashMap<String,String>();
			facetSorts.put(facetField, facetSort);
			searchState.setFacetSorts(facetSorts);
		}
		
		searchState.setBaseFacetLimit(maxResults);
		
		return searchState;
	}
	
	public SearchState createFacetSearchState(String facetField, int maxResults){
		return createFacetSearchState(facetField, null, maxResults);
	}
	
	private String getParameter(Map<String,String[]> request, String key){
		String[] value = request.get(key);
		if (value != null)
			return value[0];
		return null;
	}
	
	
	/**
	 * Populates the attributes of the given SearchState object with search state 
	 * parameters retrieved from the request mapping.
	 * @param searchState SearchState object to populate
	 * @param request
	 * @return SearchState object containing all the parameters representing the current
	 * search state in the request.
	 */
	private void populateSearchState(SearchState searchState, Map<String,String[]> request){
		//Retrieve search fields
		String parameter = getParameter(request, searchSettings.searchStateParam("SEARCH_FIELDS"));
		HashMap<String,String> searchFields = new HashMap<String,String>();
		if (parameter != null){
			String parameterArray[] = parameter.split("\\|");
			for (String parameterPair: parameterArray){
				log.debug(parameterPair);
				String parameterPairArray[] = parameterPair.split(":", 2);
				log.debug(parameterPairArray.length + "searchTermPairArray" + parameterPairArray);
				//if a field label is specified, store the search term under it.
				if (parameterPairArray.length > 1){
					searchFields.put(searchSettings.searchFieldKey(parameterPairArray[0]), parameterPairArray[1]);
				}
			}
			searchState.setSearchFields(searchFields);
		}

		//retrieve range fields
		parameter = getParameter(request, searchSettings.searchStateParam("RANGE_FIELDS"));
		HashMap<String,SearchState.RangePair> rangeFields = new HashMap<String,SearchState.RangePair>();
		if (parameter != null){
			String parameterArray[] = parameter.split("\\|");
			for (String parameterPair: parameterArray){
				try {
					String parameterPairArray[] = parameterPair.split(":", 2);
					String rangeEndpoints[] = parameterPairArray[1].split(",");
					rangeFields.put(searchSettings.searchFieldKey(parameterPairArray[0]), new SearchState.RangePair(rangeEndpoints[0], rangeEndpoints[1]));
				} catch (ArrayIndexOutOfBoundsException e){
					//An invalid range was specified, throw away the term pair
				}
			}
			searchState.setRangeFields(rangeFields);
		}
		
		//retrieve facet fields
		parameter = getParameter(request, searchSettings.searchStateParam("FACET_FIELDS"));
		HashMap<String,Object> facets = new HashMap<String,Object>();
		if (parameter != null){
			String parameterArray[] = parameter.split("\\|");
			for (String parameterPair: parameterArray){
				String parameterPairArray[] = parameterPair.split(":", 2);
				//if a field label is specified, store the facet under it.
				if (parameterPairArray.length > 1){
					String key = searchSettings.searchFieldKey(parameterPairArray[0]);
					facets.put(key, this.facetFieldFactory.createFacet(key, parameterPairArray[1]));
				}
			}
			searchState.setFacets(facets);
		}
		
		//retrieve facet limits
		parameter = getParameter(request, searchSettings.searchStateParam("FACET_LIMIT_FIELDS"));
		if (parameter != null){
			HashMap<String,Integer> facetLimits = new HashMap<String,Integer>();
			String parameterArray[] = parameter.split("\\|");
			for (String parameterPair: parameterArray){
				String parameterPairArray[] = parameterPair.split(":", 2);
				if (parameterPairArray.length > 1){
					String fieldKey = searchSettings.searchFieldKey(parameterPairArray[0]);
					//if a field label is specified, store the facet under it.
					if (searchSettings.facetNames.contains(fieldKey)){
						try {
							facetLimits.put(fieldKey, Integer.parseInt(parameterPairArray[1]));
						} catch (Exception e){
							log.error("Failed to add facet limit: " + parameterPairArray[1]);
						}
					}
				}
			}
			searchState.setFacetLimits(facetLimits);
		}
		
		//Set the base facet limit if one is provided
		parameter = getParameter(request, searchSettings.searchStateParam("BASE_FACET_LIMIT"));
		if (parameter != null){
			try {
				searchState.setBaseFacetLimit(Integer.parseInt(parameter));
			} catch (Exception e){
				log.error("Failed to parse base facet limit: " + parameter);
			}
		}
		
		//accessTypeFilter
		parameter = getParameter(request, searchSettings.searchStateParam("ACCESS_FILTER_TYPE"));
		if (parameter != null){
			searchState.setAccessTypeFilter(searchSettings.searchFieldKey(parameter));
		}
		
		//Determine resource types selected
		parameter = getParameter(request, searchSettings.searchStateParam("RESOURCE_TYPES"));
		ArrayList<String> resourceTypes = new ArrayList<String>();
		if (parameter == null){
			//If resource types aren't specified, load the defaults.
			resourceTypes.addAll(searchSettings.defaultResourceTypes);
		} else {
			String resourceArray[] = parameter.split(",");
			for (String resourceType: resourceArray){
				resourceTypes.add(resourceType);
			}
		}
		searchState.setResourceTypes(resourceTypes);
		
		//Get search term operator
		parameter = getParameter(request, searchSettings.searchStateParam("SEARCH_TERM_OPERATOR"));
		if (parameter == null){
			//If no operator set, use the default.
			searchState.setSearchTermOperator(searchSettings.defaultOperator);
		} else {
			searchState.setSearchTermOperator(parameter);
		}
		
		//Get Start row
		int startRow = 0;
		try {
			startRow = Integer.parseInt(getParameter(request, searchSettings.searchStateParam("START_ROW")));
		} catch (Exception e){
		}
		searchState.setStartRow(startRow);
		
		//Get number of rows per page
		int rowsPerPage = 0;
		try {
			rowsPerPage = Integer.parseInt(getParameter(request, searchSettings.searchStateParam("ROWS_PER_PAGE")));
		} catch (Exception e){
			//If not specified, then get the appropriate default value based on search content types.
			if (!resourceTypes.contains("File") && resourceTypes.contains("Collection")){
				rowsPerPage = searchSettings.defaultCollectionsPerPage;
			} else {
				rowsPerPage = searchSettings.defaultPerPage;
			}
		}
		searchState.setRowsPerPage(rowsPerPage);
		
		//Set sort
		parameter = getParameter(request, searchSettings.searchStateParam("SORT_TYPE"));
		if (parameter != null){
			searchState.setSortType(parameter);
			//Set sort order
			parameter = getParameter(request, searchSettings.searchStateParam("SORT_ORDER"));
			if (parameter != null){
				searchState.setSortOrder(parameter);
			}
		}
		
		//facetsToRetrieve
		parameter = getParameter(request, searchSettings.searchStateParam("FACET_FIELDS_TO_RETRIEVE"));
		ArrayList<String> facetsToRetrieve = new ArrayList<String>();
		if (parameter != null){
			String facetArray[] = parameter.split(",");
			for (String facet: facetArray){
				facetsToRetrieve.add(searchSettings.searchFieldKey(facet));
			}
			searchState.setFacetsToRetrieve(facetsToRetrieve);
		}
		
	}
	
	/**
	 * Populates a search state according to parameters expected from an advanced search request.
	 * @param searchState
	 * @param request
	 */
	private void populateSearchStateAdvancedSearch(SearchState searchState, Map<String,String[]> request){
		String parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.DEFAULT_INDEX));
		if (parameter != null && parameter.length() > 0){
			searchState.getSearchFields().put(SearchFieldKeys.DEFAULT_INDEX, parameter);
		}
		
		parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.SUBJECT_INDEX));
		if (parameter != null && parameter.length() > 0){
			searchState.getSearchFields().put(SearchFieldKeys.SUBJECT_INDEX, parameter);
		}
		
		parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.CONTRIBUTOR_INDEX));
		if (parameter != null && parameter.length() > 0){
			searchState.getSearchFields().put(SearchFieldKeys.CONTRIBUTOR_INDEX, parameter);
		}
		
		parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.TITLE_INDEX));
		if (parameter != null && parameter.length() > 0){
			searchState.getSearchFields().put(SearchFieldKeys.TITLE_INDEX, parameter);
		}
		
		parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.ANCESTOR_PATH));
		if (parameter != null && parameter.length() > 0){
			CutoffFacet hierFacet = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH, parameter);
			searchState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH, hierFacet);
		}
		
		parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.CONTENT_TYPE));
		if (parameter != null && parameter.length() > 0){
			MultivaluedHierarchicalFacet hierFacet = new MultivaluedHierarchicalFacet(SearchFieldKeys.CONTENT_TYPE, parameter);
			searchState.getFacets().put(SearchFieldKeys.CONTENT_TYPE, hierFacet);
		}
		
		parameter = getParameter(request, searchSettings.searchStateParams.get("ACCESS_FILTER_TYPE"));
		if (parameter != null && parameter.length() > 0){
			searchState.setAccessTypeFilter(searchSettings.searchFieldKey(parameter));
		}
		
		//Store date added.
		SearchState.RangePair dateAdded = new SearchState.RangePair();
		parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.DATE_ADDED) + "Start");
		if (parameter != null && parameter.length() > 0){
			dateAdded.setLeftHand(parameter);
		}
		
		parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.DATE_ADDED) + "End");
		if (parameter != null && parameter.length() > 0){
			dateAdded.setRightHand(parameter);
		}
		
		if (dateAdded.getLeftHand() != null || dateAdded.getRightHand() != null){
			searchState.getRangeFields().put(SearchFieldKeys.DATE_ADDED, dateAdded);
		}
		
		//Store date added.
		SearchState.RangePair dateCreated = new SearchState.RangePair();
		parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.DATE_CREATED) + "Start");
		if (parameter != null && parameter.length() > 0){
			dateCreated.setLeftHand(parameter);
		}
		
		parameter = getParameter(request, searchSettings.searchFieldParam(SearchFieldKeys.DATE_CREATED) + "End");
		if (parameter != null && parameter.length() > 0){
			dateCreated.setRightHand(parameter);
		}
		
		if (dateCreated.getLeftHand() != null || dateCreated.getRightHand() != null){
			searchState.getRangeFields().put(SearchFieldKeys.DATE_CREATED, dateCreated);
		}
	}

	public SearchSettings getSearchSettings() {
		return searchSettings;
	}

	@Autowired
	public void setSearchSettings(SearchSettings searchSettings) {
		this.searchSettings = searchSettings;
	}

	public void setFacetFieldFactory(FacetFieldFactory facetFieldFactory) {
		this.facetFieldFactory = facetFieldFactory;
	}
}
