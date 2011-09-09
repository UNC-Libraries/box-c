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

import java.util.List;
import java.util.ArrayList;

import org.apache.solr.client.solrj.SolrQuery;

import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;

/**
 * Response object for a search request.  Contains the list of results from the selected 
 * page, the list of hierarchical and nonhierarchical facets, and the count of the total
 * number of results the query found.
 * @author bbpennel
 * $Id: SearchResultResponse.java 2766 2011-08-22 15:29:07Z bbpennel $
 * $URL: https://vcs.lib.unc.edu/cdr/cdr-master/trunk/solr-search/src/main/java/edu/unc/lib/dl/search/solr/model/SearchResultResponse.java $
 */
public class SearchResultResponse {
	private List<BriefObjectMetadataBean> resultList;
	private FacetFieldList facetFields;
	private long resultCount;
	private SearchState searchState;
	private SolrQuery generatedQuery;
	
	public SearchResultResponse(){
	}
	
	public List<BriefObjectMetadataBean> getResultList() {
		return resultList;
	}

	public void setResultList(List<BriefObjectMetadataBean> resultList) {
		this.resultList = resultList;
	}

	public FacetFieldList getFacetFields() {
		return facetFields;
	}

	public void setFacetFields(FacetFieldList facetFields) {
		this.facetFields = facetFields;
	}

	public long getResultCount() {
		return resultCount;
	}

	public void setResultCount(long resultCount) {
		this.resultCount = resultCount;
	}

	public SearchState getSearchState() {
		return searchState;
	}

	public void setSearchState(SearchState searchState) {
		this.searchState = searchState;
	}
	
	public SolrQuery getGeneratedQuery() {
		return generatedQuery;
	}

	public void setGeneratedQuery(SolrQuery generatedQuery) {
		this.generatedQuery = generatedQuery;
	}

	public List<String> getIdList(){
		List<String> ids = new ArrayList<String>();
		for (BriefObjectMetadataBean brief: this.resultList){
			ids.add(brief.getId());
		}
		return ids;
	}
}
