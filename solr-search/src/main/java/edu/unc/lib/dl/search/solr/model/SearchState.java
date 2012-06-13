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
package edu.unc.lib.dl.search.solr.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Object representing the state of a search query, containing all of the search related 
 * parameters for specifying terms, facets, page/facet sizes, sorts, and filters.
 * 
 * @author bbpennel
 */
public class SearchState implements Serializable, Cloneable  {
	private static final long serialVersionUID = 1L;
	
	private HashMap<String,String> searchFields;
	private HashMap<String,RangePair> rangeFields;
	private HashMap<String,Object> facets; 
	private HashMap<String,Integer> facetLimits;
	private HashMap<String,String> facetSorts;
	private List<String> facetsToRetrieve;
	private int baseFacetLimit;
	private String accessTypeFilter;
	private int startRow;
	private Integer rowsPerPage;
	private String sortType;
	private String sortOrder;
	private List<String> resourceTypes;
	private String searchTermOperator;
	private List<String> resultFields;
	
	public SearchState(){
		searchFields = new HashMap<String,String>();
		rangeFields = new HashMap<String,RangePair>();
		facets = new HashMap<String,Object>();
		facetLimits = new HashMap<String,Integer>();
		facetSorts = new HashMap<String,String>();
		resultFields = null;
		facetsToRetrieve = null;
	}
	
	public SearchState(SearchState searchState){
		if (searchState.getSearchFields() != null){
			this.searchFields = new HashMap<String,String>(searchState.getSearchFields());
		}
		if (searchState.getRangeFields() != null){
			rangeFields = new HashMap<String,RangePair>();
			for (Entry<String,RangePair> item: searchState.getRangeFields().entrySet()){
				rangeFields.put(item.getKey(), new RangePair(item.getValue()));
			}
		}
		if (searchState.getFacets() != null){
			facets = new HashMap<String,Object>();
			for (Entry<String,Object> item: searchState.getFacets().entrySet()){
				if (item.getValue() instanceof edu.unc.lib.dl.search.solr.model.HierarchicalFacet){
					facets.put(item.getKey(), new HierarchicalFacet((HierarchicalFacet)item.getValue()));
				} else if (item.getValue() instanceof edu.unc.lib.dl.search.solr.model.GenericFacet){
					facets.put(item.getKey(), new GenericFacet((GenericFacet)item.getValue()));
				} else {
					facets.put(item.getKey(), (String)item.getValue());
				}
			}
		}
		if (searchState.getFacetLimits() != null){
			this.facetLimits = new HashMap<String,Integer>(searchState.getFacetLimits());
		}
		if (searchState.getFacetSorts() != null){
			this.facetSorts = new HashMap<String,String>(searchState.getFacetSorts());
		}
		if (searchState.getResourceTypes() != null){
			this.resourceTypes = new ArrayList<String>(searchState.getResourceTypes());
		}
		if (searchState.getResultFields() != null){
			this.resultFields = new ArrayList<String>(searchState.getResultFields());
		}
		if (searchState.getFacetsToRetrieve() != null){
			this.facetsToRetrieve = new ArrayList<String>(searchState.getFacetsToRetrieve());
		}
		
		baseFacetLimit = searchState.getBaseFacetLimit();
		accessTypeFilter = searchState.getAccessTypeFilter();
		startRow = searchState.getStartRow();
		rowsPerPage = searchState.getRowsPerPage();
		sortType = searchState.getSortType();
		sortOrder = searchState.getSortOrder();
		searchTermOperator = searchState.getSearchTermOperator();
	}
	
	public HashMap<String, String> getSearchFields() {
		return searchFields;
	}
	
	public void setSearchFields(HashMap<String, String> searchFields) {
		this.searchFields = searchFields;
	}
	
	public HashMap<String, Object> getFacets() {
		return facets;
	}
	
	public void setFacets(HashMap<String, Object> facets) {
		this.facets = facets;
	}
	
	public int getStartRow() {
		return startRow;
	}
	
	public void setStartRow(int startRow) {
		if (startRow < 0){
			this.startRow = 0;
			return;
		}
		this.startRow = startRow;
	}
	
	public Integer getRowsPerPage() {
		return rowsPerPage;
	}
	
	public void setRowsPerPage(Integer rowsPerPage) {
		if (rowsPerPage < 0){
			this.rowsPerPage = 0;
			return;
		}
		this.rowsPerPage = rowsPerPage;
	}
	
	public String getSortType() {
		return sortType;
	}
	
	public void setSortType(String sortType) {
		this.sortType = sortType;
	}

	public String getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}
	
	/**
	 * Retrieves all the search term fragments contained in the selected field.  Fragments are either
	 * single words separated by non-alphanumeric characters, or phrases encapsulated by quotes.
	 * @param fieldType field type of the search term string to retrieve fragments of.
	 * @return An arraylist of strings containing all of the word fragments in the selected search term field.
	 */
	public ArrayList<String> getSearchTermFragments(String fieldType){
		if (this.searchFields == null || fieldType == null)
			return null;
		String value = this.searchFields.get(fieldType);
		if (value == null)
			return null;
		Pattern pattern = Pattern.compile("(\"[^\"]*\"|[^\" ,':]+)"); 
		Matcher matcher = pattern.matcher(value); 
		ArrayList<String> fragments = new ArrayList<String>();
		while (matcher.find()){
			if (matcher.groupCount() == 1){
				fragments.add(matcher.group(1));
			}
		}
		return fragments;
	}

	public HashMap<String, RangePair> getRangeFields() {
		return rangeFields;
	}

	public void setRangeFields(HashMap<String, RangePair> rangeFields) {
		this.rangeFields = rangeFields;
	}
	
	public static class RangePair {
		//A null in either side of the pair indicates no restriction
		private String leftHand;
		private String rightHand;
		
		public RangePair(){
			
		}
		
		public RangePair(String leftHand, String rightHand){
			this.leftHand = leftHand;
			this.rightHand = rightHand;
		}
		
		public RangePair(RangePair rangePair){
			this.leftHand = rangePair.getLeftHand();
			this.rightHand = rangePair.getRightHand();
		}
		
		public String getLeftHand() {
			return leftHand;
		}

		public void setLeftHand(String leftHand) {
			this.leftHand = leftHand;
		}

		public String getRightHand() {
			return rightHand;
		}

		public void setRightHand(String rightHand) {
			this.rightHand = rightHand;
		}
		
		public String toString(){
			if (leftHand == null){
				if (rightHand == null)
					return "";
				return "," + rightHand;
			}
			if (rightHand == null)
				return leftHand + ",";
			return leftHand + "," + rightHand;
		}
	}

	public String getAccessTypeFilter() {
		return accessTypeFilter;
	}

	public void setAccessTypeFilter(String accessTypeFilter) {
		this.accessTypeFilter = accessTypeFilter;
	}

	public List<String> getResourceTypes() {
		return resourceTypes;
	}

	public void setResourceTypes(List<String> resourceTypes) {
		this.resourceTypes = resourceTypes;
	}
	
	public void setResourceTypes(Collection<String> resourceCollection){
		this.resourceTypes = new ArrayList<String>(resourceCollection);
	}

	public String getSearchTermOperator() {
		return searchTermOperator;
	}

	public void setSearchTermOperator(String searchTermOperator) {
		this.searchTermOperator = searchTermOperator;
	}

	public HashMap<String, Integer> getFacetLimits() {
		return facetLimits;
	}

	public void setFacetLimits(HashMap<String, Integer> facetLimits) {
		this.facetLimits = facetLimits;
	}

	public int getBaseFacetLimit() {
		return baseFacetLimit;
	}

	public void setBaseFacetLimit(int baseFacetLimit) {
		this.baseFacetLimit = baseFacetLimit;
	}

	public List<String> getResultFields() {
		return resultFields;
	}

	public void setResultFields(List<String> resultFields) {
		this.resultFields = resultFields;
	}

	public List<String> getFacetsToRetrieve() {
		return facetsToRetrieve;
	}

	public void setFacetsToRetrieve(List<String> facetsToRetrieve) {
		this.facetsToRetrieve = facetsToRetrieve;
	}

	public HashMap<String, String> getFacetSorts() {
		return facetSorts;
	}

	public void setFacetSorts(HashMap<String, String> facetSorts) {
		this.facetSorts = facetSorts;
	}
	
	/**
	 * Returns if any search fields that would indicate search-like behavior have been populated
	 * @return
	 */
	public boolean isPopulatedSearch(){
		return this.getFacets().size() > 0 || this.getRangeFields().size() > 0 
			|| this.getSearchFields().size() > 0 || this.getAccessTypeFilter() != null;
	}

	@Override
	public Object clone() {
		return new SearchState(this);
	}
}
