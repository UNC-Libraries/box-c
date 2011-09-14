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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.unc.lib.dl.search.solr.model.HierarchicalFacet;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchSettings;

/**
 * Service class which parses and performs any number of actions on a provided SearchState object.
 * @author bbpennel
 */
@Component
public class SearchActionService {
	private final Logger LOG = LoggerFactory.getLogger(SolrSearchService.class);
	@Autowired
	private SearchSettings searchSettings;
	
	public SearchActionService(){
		
	}
	
	/**
	 * Main action execution loop.  Parses the action string and performs all actions specified  
	 * within it over the provided search state.
	 * @param searchState
	 * @param actionsString
	 * @return
	 */
	public SearchState executeActions(SearchState searchState, String actionsString){
		if (actionsString != null && actionsString.length() > 0){
			ArrayList<ActionPair> actionList = parseActions(actionsString);
			for (ActionPair action: actionList){
				LOG.debug("Executing: " + action);
				if (action.actionName.equals(searchSettings.actionName("SET_FACET"))){
					setFacet(searchState, action.parameters);
					setStartRow(searchState, 0);
				} else if (action.actionName.equals(searchSettings.actionName("REMOVE_FACET"))){
					removeField(action.parameters, searchState.getFacets());
					setStartRow(searchState, 0);
				} else if (action.actionName.equals(searchSettings.actionName("SET_SEARCH_FIELD"))){
					setField(action.parameters, searchState.getSearchFields());
					setStartRow(searchState, 0);
				} else if (action.actionName.equals(searchSettings.actionName("ADD_SEARCH_FIELD"))){
					addField(action.parameters, searchState.getSearchFields());
					setStartRow(searchState, 0);
				} else if (action.actionName.equals(searchSettings.actionName("REMOVE_SEARCH_FIELD"))){
					removeField(action.parameters, searchState.getSearchFields());
					setStartRow(searchState, 0);
				} else if (action.actionName.equals(searchSettings.actionName("SET_RANGE_FIELD"))){
					setRangeField(searchState, action.parameters);
					setStartRow(searchState, 0);
				} else if (action.actionName.equals(searchSettings.actionName("REMOVE_RANGE_FIELD"))){
					removeField(action.parameters, searchState.getRangeFields());
					setStartRow(searchState, 0);
				} else if (action.actionName.equals(searchSettings.actionName("SET_FACET_LIMIT"))){
					setFacetLimit(searchState, action.parameters);
					setStartRow(searchState, 0);
				} else if (action.actionName.equals(searchSettings.actionName("REMOVE_FACET_LIMIT"))){
					removeField(action.parameters, searchState.getFacetLimits());
					setStartRow(searchState, 0);
				} else if (action.actionName.equals(searchSettings.actionName("SET_FACET_SELECT"))){
					setFacetSelect(searchState, action.parameters);
				} else if (action.actionName.equals(searchSettings.actionName("REMOVE_FACET_SELECT"))){
					removeFacetSelect(searchState);
				} else if (action.actionName.equals(searchSettings.actionName("NEXT_PAGE"))){
					nextPage(searchState);
				} else if (action.actionName.equals(searchSettings.actionName("PREVIOUS_PAGE"))){
					previousPage(searchState);
				} else if (action.actionName.equals(searchSettings.actionName("SET_START_ROW"))){
					setStartRow(searchState, action.parameters);
				} else if (action.actionName.equals(searchSettings.actionName("SET_ROWS_PER_PAGE"))){
					setRow(searchState, action.parameters);
					setStartRow(searchState, 0);
				} else if (action.actionName.equals(searchSettings.actionName("REMOVE_ROWS_PER_PAGE"))){
					removeRow(searchState);
					setStartRow(searchState, 0);
				} else if (action.actionName.equals(searchSettings.actionName("SET_SORT"))){
					setSort(searchState, action.parameters);
					setStartRow(searchState, 0);
				} else if (action.actionName.equals(searchSettings.actionName("SET_ACCESS_FILTER"))){
					setAccessFilter(searchState, action.parameters);
					setStartRow(searchState, 0);
				} else if (action.actionName.equals(searchSettings.actionName("REMOVE_ACCESS_FILTER"))){
					removeAccessFilter(searchState);
					setStartRow(searchState, 0);
				} else if (action.actionName.equals(searchSettings.actionName("SET_RESOURCE_TYPE"))){
					setResourceType(searchState, action.parameters);
					setStartRow(searchState, 0);
				} else if (action.actionName.equals(searchSettings.actionName("REMOVE_RESOURCE_TYPE"))){
					removeResourceType(searchState, action.parameters);
					setStartRow(searchState, 0);
				} else if (action.actionName.equals(searchSettings.actionName("RESET_NAVIGATION"))){
					resetNavigation(searchState, action.parameters);
				}
			}
		}
		
		return searchState;
	}
	
	private ArrayList<ActionPair> parseActions(String actionsString){
		ArrayList<ActionPair> actionList = new ArrayList<ActionPair>();
		String actions[] = actionsString.split("\\|");
		for (String action: actions){
			String actionComponents[] = action.split(":", 2);
			if (actionComponents.length > 0){
				String actionName = actionComponents[0];
				ArrayList<String> parameters = null;
				if (actionComponents.length > 1){
					String actionParametersString = actionComponents[1];
					Pattern pattern = Pattern.compile("(\"[^\"]*\"|[^,]+)"); 
					Matcher matcher = pattern.matcher(actionParametersString); 
					parameters = new ArrayList<String>();
					while (matcher.find()){
						if (matcher.groupCount() == 1){
							String match = matcher.group(1);
							parameters.add(match.replaceAll("%2[cC]", ","));
						}
					}
				}
				actionList.add(new ActionPair(actionName, parameters));
			}
		}
		return actionList;
	}
	
	private void setRangeField(SearchState searchState, ArrayList<String> parameters){
		if (parameters.size() != 3)
			return;
		searchState.getRangeFields().put(searchSettings.searchFieldKey(parameters.get(0)), 
				new SearchState.RangePair(parameters.get(1), parameters.get(2)));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void setField(ArrayList<String> parameters, Map collection){
		if (parameters.size() != 2)
			return;
		collection.put(searchSettings.searchFieldKey(parameters.get(0)), parameters.get(1));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addField(ArrayList<String> parameters, Map collection){
		if (parameters.size() != 2)
			return;
		String value = (String)collection.get(searchSettings.searchFieldKey(parameters.get(0)));
		if (value == null){
			value = parameters.get(1);
		} else {
			value += " " + parameters.get(1);
		}
		collection.put(searchSettings.searchFieldKey(parameters.get(0)), value);
	}
	
	@SuppressWarnings("rawtypes")
	private void removeField(ArrayList<String> parameters, Map collection){
		if (parameters.size() != 1)
			return;
		collection.remove(searchSettings.searchFieldKey(parameters.get(0)));
	}
	
	private void setFacet(SearchState searchState, ArrayList<String> parameters){
		if (parameters.size() < 2)
			return;
		String key = searchSettings.searchFieldKey(parameters.get(0));
		String value = parameters.get(1);
		if (value.indexOf('"') == 0){
			value = value.substring(1);
		}
		if (value.lastIndexOf('"') == value.length()-1){
			value = value.substring(0, value.length()-1);
		}
		if (searchSettings.hierarchicalFacets.contains(key)){
			String[] parameterSubfields = value.split(",");
			HierarchicalFacet hierFacet = new HierarchicalFacet(key, parameterSubfields[0] + "," + parameterSubfields[1]);
			if (parameterSubfields.length == 3){
				try {
					hierFacet.setCutoffTier(new Integer(parameterSubfields[2]));
				} catch (NumberFormatException ignored){
					//Cutoff value was not an integer, so ignore it.
				}
			}
			searchState.getFacets().put(key, hierFacet);
		} else {
			searchState.getFacets().put(key, value);
		}
	}
	
	private void setFacetLimit(SearchState searchState, ArrayList<String> parameters){
		if (parameters.size() != 2)
			return;
		try {
			searchState.getFacetLimits().put(searchSettings.searchFieldKey(parameters.get(0)), 
					Integer.parseInt(parameters.get(1)));
		} catch (NumberFormatException e){
			LOG.error("Failed to perform set facet limit action: " + parameters.get(1));
		}
	}
	
	private void setFacetSelect(SearchState searchState, ArrayList<String> parameters){
		searchState.setFacetsToRetrieve(parameters);
	}
	
	private void removeFacetSelect(SearchState searchState){
		searchState.setFacetsToRetrieve(null);
	}
	
	private void nextPage(SearchState searchState){
		searchState.setStartRow(searchState.getStartRow() + searchState.getRowsPerPage());
	}
	
	private void previousPage(SearchState searchState){
		searchState.setStartRow(searchState.getStartRow() - searchState.getRowsPerPage());
	}
	
	private void setStartRow(SearchState searchState, ArrayList<String> parameters){
		if (parameters.size() != 1)
			return;
		setStartRow(searchState, Integer.parseInt(parameters.get(0)));
	}
	
	private void setStartRow(SearchState searchState, int startRow){
		searchState.setStartRow(startRow);
	}
	
	private void setRow(SearchState searchState, ArrayList<String> parameters){
		if (parameters.size() != 1)
			return;
		setRow(searchState, Integer.parseInt(parameters.get(0)));
	}
	
	private void setRow(SearchState searchState, int row){
		searchState.setRowsPerPage(row);
	}
	
	private void removeRow(SearchState searchState){
		searchState.setRowsPerPage(null);
	}
	
	private void setSort(SearchState searchState, ArrayList<String> parameters){
		if (parameters.size() != 2)
			return;
		searchState.setSortType(parameters.get(0));
		searchState.setSortOrder(parameters.get(1));
	}
	
	private void setAccessFilter(SearchState searchState, ArrayList<String> parameters){
		if (parameters.size() < 1)
			return;
		searchState.setAccessTypeFilter(searchSettings.searchFieldKey(parameters.get(0)));
	}
	
	private void removeAccessFilter(SearchState searchState){
		searchState.setAccessTypeFilter(null);
	}
	
	private void setResourceType(SearchState searchState, ArrayList<String> parameters){
		if (parameters.size() < 1)
			return;
		searchState.setResourceTypes(parameters);
	}
	
	private void removeResourceType(SearchState searchState, ArrayList<String> parameters){
		if (parameters.size() < 1)
			return;
		searchState.getResourceTypes().removeAll(parameters);
	}
	
	private void resetNavigation(SearchState searchState, ArrayList<String> parameters){
		if (parameters.size() < 1)
			return;
		String mode = parameters.get(0); 
		if (mode.equals("search")){
			searchState.setFacetsToRetrieve(new ArrayList<String>(searchSettings.getFacetNames()));
			searchState.setRowsPerPage(searchSettings.defaultPerPage);
			searchState.setResourceTypes(null);
		} else if (mode.equals("collections")){
			searchState.setFacetsToRetrieve(new ArrayList<String>(searchSettings.getCollectionBrowseFacetNames()));
			searchState.setRowsPerPage(searchSettings.defaultCollectionsPerPage);
			ArrayList<String> resourceTypes = new ArrayList<String>();
			resourceTypes.add(searchSettings.getResourceTypeCollection());
			searchState.setResourceTypes(resourceTypes);
		} else if (mode.equals("structure")){
			searchState.setFacetsToRetrieve(new ArrayList<String>(searchSettings.getFacetNamesStructureBrowse()));
			searchState.setRowsPerPage(searchSettings.defaultPerPage);
			searchState.setResourceTypes(null);
		}
		searchState.setStartRow(0);
	}
	
	public SearchSettings getSearchSettings() {
		return searchSettings;
	}

	public void setSearchSettings(SearchSettings searchSettings) {
		this.searchSettings = searchSettings;
	}

	public class ActionPair {
		public String actionName;
		public ArrayList<String> parameters;
		
		public ActionPair(String actionName, ArrayList<String> parameters) {
			this.actionName = actionName;
			this.parameters = parameters;
		}
		
		public String toString(){
			String output = actionName;
			if (parameters != null){
				for (String parameter: parameters){
					output += " " + parameter;
				}
			}
			return output;
		}
	}
}
