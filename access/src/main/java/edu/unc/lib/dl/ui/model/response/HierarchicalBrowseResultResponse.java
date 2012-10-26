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
package edu.unc.lib.dl.ui.model.response;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.HierarchicalFacet.HierarchicalFacetTier;
import edu.unc.lib.dl.search.solr.model.HierarchicalFacetNode;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.util.SearchSettings;

public class HierarchicalBrowseResultResponse extends SearchResultResponse {
	private Map<String,Long> subcontainerCounts;
	private HashSet<String> matchingContainerPids = null;
	private Long rootCount;
	
	private static SearchSettings searchSettings;
	
	public HierarchicalBrowseResultResponse(){
		super();
		subcontainerCounts = new HashMap<String,Long>();
		matchingContainerPids = new HashSet<String>();
	}
	
	public void setSearchResultResponse(SearchResultResponse response){
		this.setFacetFields(response.getFacetFields());
		this.setResultCount(response.getResultCount());
		this.setGeneratedQuery(response.getGeneratedQuery());
		this.setResultList(response.getResultList());
		this.setSearchState(response.getSearchState());
	}
	
	public Map<String, Long> getSubcontainerCounts() {
		return subcontainerCounts;
	}

	public void setSubcontainerCounts(Map<String, Long> subcontainerCounts) {
		this.subcontainerCounts = subcontainerCounts;
	}

	public void populateSubcontainerCounts(List<FacetField> facetFields){
		subcontainerCounts = new HashMap<String,Long>();
		for (FacetField facetField: facetFields){
			if (facetField.getValues() != null){
				for (FacetField.Count facetValue: facetField.getValues()){
					int index = facetValue.getName().indexOf(",");
					index = facetValue.getName().indexOf(",", index+1);
					if (index != -1)
						subcontainerCounts.put(facetValue.getName().substring(0, index), facetValue.getCount());
				}
			}
		}
	}
	
	public void removeContainersWithoutContents(){
		ListIterator<BriefObjectMetadataBean> resultIt = this.getResultList().listIterator(this.getResultList().size());
		while (resultIt.hasPrevious()){
			BriefObjectMetadataBean briefObject = resultIt.previous();
			if (briefObject.getChildCount() == 0 && searchSettings.isResourceTypeContainer(briefObject.getResourceType())){
				if (this.matchingContainerPids != null && this.matchingContainerPids.contains(briefObject.getId())){
					//The container was directly found by the users query, so leave it as is.
				} else {
					resultIt.remove();
					//If an item is being filtered out, then decrement the counts for it and all its ancestors in subcontainer counts
					if (briefObject.getAncestorPathFacet() != null && briefObject.getAncestorPathFacet().getFacetNodes() != null){
						for (HierarchicalFacetNode facetTier: briefObject.getAncestorPathFacet().getFacetNodes()){
							String tierIdentifier = facetTier.getSearchKey();
							Long count = this.subcontainerCounts.get(tierIdentifier);
							if (count != null)
								this.subcontainerCounts.put(tierIdentifier, count - 1);
						}
					}
				}
			}
		}
	}
	
	public void populateMatchingContainerPids(SolrDocumentList containerList, String fieldName){
		this.matchingContainerPids = new HashSet<String>();
		for (SolrDocument container: containerList){
			this.matchingContainerPids.add((String)container.getFirstValue(fieldName));
			
		}
	}
	
	/**
	 * Appends item results to the end of the list and adds them as children of the root.
	 * @param itemResults
	 */
	public void populateItemResults(List<BriefObjectMetadataBean> itemResults){
		if (this.getResultList().size() > 0 && this.getResultList().get(0).getPath() != null){
			for (HierarchicalFacetNode rootTier: this.getResultList().get(0).getPath().getFacetNodes()){
				Long count = this.subcontainerCounts.get(rootTier.getSearchKey());
				if (count == null){
					this.subcontainerCounts.put(rootTier.getSearchKey(), (long)itemResults.size());
				} else {
					this.subcontainerCounts.put(rootTier.getSearchKey(), count + itemResults.size());
				}
			}
		}
		for (BriefObjectMetadataBean itemResult: itemResults){
			this.getResultList().add(itemResult);
		}
	}

	public Long getRootCount() {
		return rootCount;
	}

	public void setRootCount(Long rootCount) {
		this.rootCount = rootCount;
	}

	public void setSearchSettings(SearchSettings searchSettings) {
		HierarchicalBrowseResultResponse.searchSettings = searchSettings;
	}

	public HashSet<String> getMatchingContainerPids() {
		return matchingContainerPids;
	}

	public void setMatchingContainerPids(HashSet<String> matchingContainerPids) {
		this.matchingContainerPids = matchingContainerPids;
	}
}
