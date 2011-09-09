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
import edu.unc.lib.dl.security.access.AccessGroupSet;
import edu.unc.lib.dl.search.solr.model.SearchState;

/**
 * Request bean for a brief record search.  Handles basic searches and advanced searches. 
 * @author bbpennel
 * $Id: SearchRequest.java 2766 2011-08-22 15:29:07Z bbpennel $
 * $URL: https://vcs.lib.unc.edu/cdr/cdr-master/trunk/solr-search/src/main/java/edu/unc/lib/dl/search/solr/model/SearchRequest.java $
 */
public class SearchRequest implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private SearchState searchState;
	private boolean applyFacetCutoffs;
	private boolean applyFacetPrefixes;
	private AccessGroupSet accessGroups;
	
	public SearchRequest(){
		searchState = null;
		accessGroups = null;
		applyFacetCutoffs = true;
		applyFacetPrefixes = true;
	}
	
	public SearchRequest(SearchState searchState, AccessGroupSet accessGroups){
		setSearchState(searchState);
		setAccessGroups(accessGroups);
		applyFacetCutoffs = true;
		applyFacetPrefixes = true;
	}
	
	public SearchRequest(SearchState searchState, AccessGroupSet accessGroups, boolean applyFacetCutoffs){
		setSearchState(searchState);
		setAccessGroups(accessGroups);
		this.applyFacetCutoffs = applyFacetCutoffs;
		applyFacetPrefixes = true;
	}

	public SearchState getSearchState() {
		return searchState;
	}

	public void setSearchState(SearchState searchState) {
		this.searchState = searchState;
	}

	public AccessGroupSet getAccessGroups() {
		return accessGroups;
	}

	public void setAccessGroups(AccessGroupSet accessGroups) {
		this.accessGroups = accessGroups;
	}

	public boolean isApplyFacetCutoffs() {
		return applyFacetCutoffs;
	}

	public void setApplyFacetCutoffs(boolean applyFacetCutoffs) {
		this.applyFacetCutoffs = applyFacetCutoffs;
	}

	public boolean isApplyFacetPrefixes() {
		return applyFacetPrefixes;
	}

	public void setApplyFacetPrefixes(boolean applyFacetPrefixes) {
		this.applyFacetPrefixes = applyFacetPrefixes;
	}
	
	
}
