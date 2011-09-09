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
package edu.unc.lib.dl.ui.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.FacetFieldObject;
import edu.unc.lib.dl.search.solr.model.GenericFacet;
import edu.unc.lib.dl.search.solr.model.HierarchicalFacet;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.security.access.AccessGroupSet;
import edu.unc.lib.dl.security.access.AccessRestrictionException;
import edu.unc.lib.dl.ui.model.request.HierarchicalBrowseRequest;
import edu.unc.lib.dl.ui.model.response.HierarchicalBrowseResultResponse;

/**
 * Solr query construction layer.  Constructs search states specific to common tasks before passing
 * them on to lower level classes to retrieve the results.
 * @author bbpennel
 * $Id: SolrQueryLayerService.java 2743 2011-08-12 16:56:19Z bbpennel $
 * $URL: https://vcs.lib.unc.edu/cdr/cdr-master/trunk/access/src/main/java/edu/unc/lib/dl/ui/service/SolrQueryLayerService.java $
 */
public class SolrQueryLayerService extends SolrSearchService {
	private static final Logger LOG = LoggerFactory.getLogger(SolrQueryLayerService.class);
	
	/**
	 * Returns a list of the most recently added items in the collection
	 * @param accessGroups
	 * @return Result response, where items only contain title and id.
	 */
	public SearchResultResponse getNewlyAdded(AccessGroupSet accessGroups){
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setAccessGroups(accessGroups);
		
		SearchState searchState = SearchStateFactory.createTitleListSearchState();
		searchState.setResourceTypes(searchSettings.defaultResourceTypes);
		searchState.setRowsPerPage(searchSettings.defaultListResultsPerPage);
		searchState.setSortType("dateAdded");
		
		searchRequest.setSearchState(searchState);
		return getSearchResults(searchRequest);
	}
	
	/**
	 * Returns a list of collections
	 * @param accessGroups
	 * @return
	 */
	public SearchResultResponse getCollectionList(AccessGroupSet accessGroups){
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setAccessGroups(accessGroups);
		
		SearchState searchState = SearchStateFactory.createSearchState();
		searchState.setResourceTypes(searchSettings.defaultCollectionResourceTypes);
		searchState.setRowsPerPage(50);
		searchState.setFacetsToRetrieve(null);
		ArrayList<String> resultFields = new ArrayList<String>();
		resultFields.add(SearchFieldKeys.ANCESTOR_PATH);
		resultFields.add(SearchFieldKeys.TITLE);
		resultFields.add(SearchFieldKeys.ID);
		searchState.setResultFields(resultFields);
		
		searchRequest.setSearchState(searchState);
		return getSearchResults(searchRequest);
	}
	
	public SearchResultResponse getDepartmentList(AccessGroupSet accessGroups){
		SearchState searchState = SearchStateFactory.createFacetSearchState(SearchFieldKeys.DEPARTMENT, "index", 500);
		
		SearchRequest searchRequest = new SearchRequest(searchState, accessGroups);
		
		return getSearchResults(searchRequest);
	}
	
	/**
	 * Retrieves the facet list for the search defined by searchState.  The facet results optionally can
	 * ignore hierarchical cutoffs.
	 * @param searchState
	 * @param facetsToRetrieve
	 * @param applyCutoffs
	 * @return
	 */
	public SearchResultResponse getFacetList(SearchState searchState, AccessGroupSet accessGroups, List<String> facetsToRetrieve, boolean applyCutoffs){
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setAccessGroups(accessGroups);
		searchRequest.setSearchState(searchState);
		
		//Backup resource types
		List<String> resourceTypes = searchState.getResourceTypes();
		//Backup the rowsPerPage
		Integer rowsPerPage = searchState.getRowsPerPage();
		
		searchState.setRowsPerPage(0);
		if (facetsToRetrieve != null)
			searchState.setFacetsToRetrieve(facetsToRetrieve);
		searchState.setResourceTypes(null);
		searchRequest.setApplyFacetCutoffs(applyCutoffs);
		
		SearchResultResponse resultResponse = getSearchResults(searchRequest);
		
		if (searchState.getFacetsToRetrieve() == null || searchState.getFacetsToRetrieve().contains(SearchFieldKeys.PARENT_COLLECTION)){
			FacetFieldObject parentCollectionFacet = resultResponse.getFacetFields().get(SearchFieldKeys.PARENT_COLLECTION);
			List<BriefObjectMetadataBean> parentCollectionValues = getParentCollectionValues(resultResponse.getFacetFields().get(SearchFieldKeys.PARENT_COLLECTION), accessGroups);
			int i;
			if (parentCollectionFacet != null){
				for (GenericFacet pidFacet: parentCollectionFacet.getValues()){
					String pid = pidFacet.getSearchValue();
					for (i = 0; i < parentCollectionValues.size() && !pid.equals(parentCollectionValues.get(i).getId()); i++);
					if (i < parentCollectionValues.size()){
						HierarchicalFacet parentPath = parentCollectionValues.get(i).getPath();
						pidFacet.setFieldName(SearchFieldKeys.ANCESTOR_PATH);
						pidFacet.setDisplayValue(parentPath.getDisplayValue());
						pidFacet.setValue(parentPath.getSearchValue());
					}
				}
			}
		}
		
		//Restore rows per page
		searchState.setRowsPerPage(rowsPerPage);
		//Restore resourceTypes
		searchState.setResourceTypes(resourceTypes);
		
		return resultResponse;
	}
	
	public List<BriefObjectMetadataBean> getParentCollectionValues(FacetFieldObject parentCollectionFacet, AccessGroupSet accessGroups){
		if (parentCollectionFacet == null || parentCollectionFacet.getValues() == null 
				|| parentCollectionFacet.getValues().size() == 0){
			return null;
		}
		
		QueryResponse queryResponse = null;
		SolrQuery solrQuery = new SolrQuery();
		StringBuilder query = new StringBuilder();
		boolean first = true;
		
		query.append('(');
		for (GenericFacet pidFacet: parentCollectionFacet.getValues()){
			if (pidFacet.getSearchValue() != null && pidFacet.getSearchValue().length() > 0){
				if (first){
					first = false;
				} else {
					query.append(" OR ");
				}
				query.append(solrSettings.getFieldName(SearchFieldKeys.ID)).append(':')
					.append(solrSettings.sanitize(pidFacet.getSearchValue()));
			}
		}
		query.append(')');
		
		try {
			//Add access restrictions to query
			addAccessRestrictions(query, accessGroups, SearchFieldKeys.RECORD_ACCESS);
		} catch (AccessRestrictionException e){
			//If the user doesn't have any access groups, they don't have access to anything, return null.
			LOG.error("No access groups", e);
			return null;
		}
		
		solrQuery.setQuery(query.toString());
		
		solrQuery.setFacet(true);
		solrQuery.setFields(solrSettings.getFieldName(SearchFieldKeys.ID), 
				solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH), solrSettings.getFieldName(SearchFieldKeys.TITLE));
		
		solrQuery.setRows(parentCollectionFacet.getValues().size());
		
		try {
			queryResponse = this.executeQuery(solrQuery);
			return queryResponse.getBeans(BriefObjectMetadataBean.class);
		} catch (SolrServerException e) {
			LOG.error("Failed to execute query " + solrQuery.toString(), e);
		}
		return null;
	}
	
	/**
	 * Retrieves a list of the nearest windowSize neighbors within the nearest parent collection or folder around the item metadata, 
	 * based on the order field of the item.  The first windowSize/2 neighbors are retrieved to each side of the item.  If there 
	 * are fewer than windowSize/2 items to a side, then the opposite side of the window is expanded by the difference so that the 
	 * total number of records will equal windowSize if there are enough total records in the parent container.
	 * @param metadata Record which the window pivots around.
	 * @param windowSize max number of items in the window.  This includes the pivot, so odd numbers are recommended.
	 * @param accessGroups Access groups of the user making this request.
	 * @return
	 */
	public List<BriefObjectMetadataBean> getNeighboringItems(BriefObjectMetadataBean metadata, int windowSize, AccessGroupSet accessGroups){
		int splitSize = windowSize/2;
		int rows = splitSize;
		List<BriefObjectMetadataBean> leftList = null;
		List<BriefObjectMetadataBean> rightList = null;
		
		Long pivotOrder = metadata.getDisplayOrder();
		if (pivotOrder == null)
			pivotOrder = (long)0;
		
		StringBuilder accessQuery = new StringBuilder();
		try {
			//Add access restrictions to query
			addAccessRestrictions(accessQuery, accessGroups, SearchFieldKeys.RECORD_ACCESS);
		} catch (AccessRestrictionException e){
			//If the user doesn't have any access groups, they don't have access to anything, return null.
			LOG.error(e.getMessage());
			return null;
		}
		
		QueryResponse queryResponse = null;
		SolrQuery solrQuery = new SolrQuery();
		StringBuilder query = new StringBuilder();
		
		//Get the first half of the window to the left of the pivot record
		query.append(solrSettings.getFieldName(SearchFieldKeys.DISPLAY_ORDER)).append(":[* TO ");
		if (pivotOrder == 0)
			query.append(0);
		else query.append(pivotOrder - 1);
		query.append(']');
		
		query.append(accessQuery);
		solrQuery.setQuery(query.toString());
		
		solrQuery.setFacet(true);
		solrQuery.addFilterQuery(solrSettings.getFieldName(SearchFieldKeys.RESOURCE_TYPE) + ":File");
		HierarchicalFacet ancestorPath = null;
		if (metadata.getResourceType().equals(searchSettings.resourceTypeFile)){
			ancestorPath = metadata.getAncestorPath();
		} else {
			ancestorPath = metadata.getPath();
		}
		if (ancestorPath != null && ancestorPath.getFacetTiers() != null){
			for (HierarchicalFacet.HierarchicalFacetTier facetTier: ancestorPath.getFacetTiers()){
				solrQuery.addFilterQuery(solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH)+ ":" 
						+ facetTier.getTier() + searchSettings.facetSubfieldDelimiter 
						+ solrSettings.sanitize(facetTier.getSearchValue()) + searchSettings.facetSubfieldDelimiter + "*");
			}
		}
		
		solrQuery.setStart(0);
		solrQuery.setRows(rows);
		
		solrQuery.setSortField(solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_NAMES), SolrQuery.ORDER.asc);
		solrQuery.addSortField(solrSettings.getFieldName(SearchFieldKeys.DISPLAY_ORDER), SolrQuery.ORDER.desc);
		
		//Execute left hand query
		try {
			queryResponse = this.executeQuery(solrQuery);
			leftList = queryResponse.getBeans(BriefObjectMetadataBean.class);
		} catch (SolrServerException e) {
			LOG.error("Error retrieving Neighboring items: " + e);
			return null;
		}
		
		//Expand the right side window size by the difference between the split size and the left window result count.
		if (leftList.size() < splitSize){
			rows = splitSize * 2 - leftList.size();
		}
		
		query = new StringBuilder();
		//Get the right half of the window where display order is greater than the pivot
		query.append(solrSettings.getFieldName(SearchFieldKeys.DISPLAY_ORDER)).append(":[").append(pivotOrder+1).append(" TO *]");
		query.append(accessQuery);
		solrQuery.setQuery(query.toString());
		
		solrQuery.setRows(rows);
		
		solrQuery.setSortField(solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_NAMES), SolrQuery.ORDER.asc);
		solrQuery.addSortField(solrSettings.getFieldName(SearchFieldKeys.DISPLAY_ORDER), SolrQuery.ORDER.asc);
		
		//Execute right hand query
		try {
			queryResponse = this.executeQuery(solrQuery);
			rightList = queryResponse.getBeans(BriefObjectMetadataBean.class);
		} catch (SolrServerException e) {
			LOG.error("Error retrieving Neighboring items: " + e);
			return null;
		}
		
		//If there are no more items to the left or the left and right windows plus pivot equals the window size, then we're done.
		if (leftList.size() < splitSize || rightList.size() + leftList.size() + 1 == windowSize){
			Collections.reverse(leftList);
			leftList.add(metadata);
			leftList.addAll(rightList);
			return leftList;
		}
		
		//Less than split size from the right side but not touching the left side, so we need to expand the left side
		query.append(solrSettings.getFieldName(SearchFieldKeys.DISPLAY_ORDER)).append(":[* TO ").append(leftList.get(leftList.size() - 1).getDisplayOrder()-1).append(']');
		query.append(accessQuery);
		solrQuery.setQuery(query.toString());
		
		solrQuery.setRows(splitSize * 2 - leftList.size());
		
		solrQuery.setSortField(solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_NAMES), SolrQuery.ORDER.asc);
		solrQuery.addSortField(solrSettings.getFieldName(SearchFieldKeys.DISPLAY_ORDER), SolrQuery.ORDER.desc);
		
		
		//Execute query and add the results to the end of the left list
		try {
			queryResponse = this.executeQuery(solrQuery);
			leftList.addAll(queryResponse.getBeans(BriefObjectMetadataBean.class));
		} catch (SolrServerException e) {
			LOG.error("Error retrieving Neighboring items: " + e);
			return null;
		}
		
		Collections.reverse(leftList);
		leftList.add(metadata);
		leftList.addAll(rightList);
		return leftList;
	}
	
	/**
	 * Returns the number of children plus a facet list for the parent defined by ancestorPath.
	 * @param ancestorPath
	 * @param accessGroups
	 * @return
	 */
	public SearchResultResponse getFullRecordSupplementalData(HierarchicalFacet ancestorPath, AccessGroupSet accessGroups){
		SearchState searchState = SearchStateFactory.createSearchState();
		searchState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH, ancestorPath);
		searchState.setRowsPerPage(0);
		return getFacetList(searchState, accessGroups, new ArrayList<String>(searchSettings.collectionBrowseFacetNames), false);
	}
	
	/**
	 * Populates the child count attributes of all metadata objects in the given search result response 
	 * by querying for all non-folder objects which have the metadata object's highest ancestor path
	 * tier somewhere in its ancestor path.
	 * @param resultResponse
	 * @param accessGroups
	 */
	public void getChildrenCounts(List<BriefObjectMetadataBean> resultList, AccessGroupSet accessGroups){
		QueryResponse queryResponse = null;
		SolrQuery solrQuery = new SolrQuery();
		StringBuilder query = new StringBuilder();
		
		try {
			//Add access restrictions to query
			addAccessRestrictions(query, accessGroups, SearchFieldKeys.RECORD_ACCESS);
		} catch (AccessRestrictionException e){
			//If the user doesn't have any access groups, they don't have access to anything, return null.
			LOG.error(e.getMessage());
			return;
		}
		
		solrQuery.setStart(0);
		solrQuery.setRows(0);
		
		long maxTier = 0;
		boolean first = true; 

		List<BriefObjectMetadataBean> containerObjects = new ArrayList<BriefObjectMetadataBean>();
		
		for (BriefObjectMetadataBean metadataObject: resultList){
			if (metadataObject.getPath() != null && (metadataObject.getResourceType().equals(searchSettings.resourceTypeCollection) 
					|| metadataObject.getResourceType().equals(searchSettings.resourceTypeFolder))){
				if (first){
					first = false;
					query.append("(");
				} else {
					query.append(" OR ");
				}
				query.append(solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH)).append(':')
						.append(solrSettings.sanitize(metadataObject.getPath().getSearchValue())).append(",*");
				containerObjects.add(metadataObject);
				long highestTier = metadataObject.getPath().getHighestTier();
				if (maxTier < highestTier)
					maxTier = highestTier;
			}
		}
		
		if (!first)
			query.append(")");
		
		//Add query
		if (query.length() > 0){
			solrQuery.setQuery(query.toString());
		} else {
			return;
		}
		
		StringBuilder filterQuery = new StringBuilder();
		filterQuery.append("!").append(solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH))
				.append(':').append(searchSettings.resourceTypeFile);
		
		solrQuery.addFilterQuery(filterQuery.toString());
		
		try {
			queryResponse = this.executeQuery(solrQuery);
			
			solrQuery.removeFilterQuery(filterQuery.toString());
			
			solrQuery.setFacet(true);
			solrQuery.setFacetMinCount(1);
			solrQuery.addFacetField(solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH));
			
			//Set the limit on number of facets to return very high since its not possible to predict the # values without a second query
			solrQuery.add("f." + solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH) + ".facet.limit", 
					String.valueOf(queryResponse.getResults().getNumFound()));
			
			//Don't return any facets past the max tier in the contain set, but don't filter to this since that'd effect counts
			StringBuilder facetQuery = new StringBuilder();
			facetQuery.append('!').append(solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH)).append(':') 
					.append(maxTier+1).append(searchSettings.facetSubfieldDelimiter).append('*');
			solrQuery.addFacetQuery(facetQuery.toString());
			
			queryResponse = this.executeQuery(solrQuery);
			assignChildrenCounts(queryResponse, containerObjects);
		} catch (SolrServerException e) {
			LOG.error("Error retrieving Solr search result request: " + e);
		}
	}
	
	protected void assignChildrenCounts(QueryResponse queryResponse, List<BriefObjectMetadataBean> containerObjects){
		for (FacetField facetField: queryResponse.getFacetFields()){
			if (facetField.getValues() != null){
				for (FacetField.Count facetValue: facetField.getValues()){
					for (int i=0; i < containerObjects.size(); i++){
						BriefObjectMetadataBean container = containerObjects.get(i);
						if (facetValue.getName().indexOf(container.getPath().getSearchValue()) == 0){
							container.setChildCount(facetValue.getCount());
							break;
						}	
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves results for populating a hierarchical browse view.  Supports all the regular navigation
	 * available to searches.  Results contain child counts for each item (all items returns are containers),
	 * and a map containing the number of nested subcontainers per container.  Children counts are retrieved
	 * based on facet counts.
	 * @param browseRequest
	 * @return
	 */
	public HierarchicalBrowseResultResponse getHierarchicalBrowseResults(HierarchicalBrowseRequest browseRequest){
		AccessGroupSet accessGroups = browseRequest.getAccessGroups();
		
		boolean noRootNode = !browseRequest.getSearchState().getFacets().containsKey(SearchFieldKeys.ANCESTOR_PATH);
		
		HierarchicalBrowseResultResponse browseResults = new HierarchicalBrowseResultResponse();
		
		SearchState hierarchyState = SearchStateFactory.createHierarchyListSearchState();
		if (!noRootNode){
			hierarchyState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH, 
					browseRequest.getSearchState().getFacets().get(SearchFieldKeys.ANCESTOR_PATH));
		}
		
		hierarchyState.setRowsPerPage(0);
		
		SearchRequest hierarchyRequest = new SearchRequest(hierarchyState, accessGroups);
		
		//Get the total number of containers within the scope of the parent.
		SearchResultResponse results = this.getSearchResults(hierarchyRequest, true);
		long containerCount = results.getResultCount();
		
		//////////////////////////////////////////////////////////////
		//Get the set of all applicable containers
		SolrQuery hierarchyQuery = results.getGeneratedQuery();
		hierarchyQuery.setRows((int)containerCount);
		
		//Add ancestorPath cutoff to query
		StringBuilder cutoffQuery = new StringBuilder();
		cutoffQuery.append('!').append(solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH)).append(":");
		if (hierarchyState.getFacets().containsKey(SearchFieldKeys.ANCESTOR_PATH)){
			cutoffQuery.append(((HierarchicalFacet)hierarchyState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH)).getHighestTier() + browseRequest.getRetrievalDepth());
		} else {
			cutoffQuery.append(browseRequest.getRetrievalDepth());
		}
		cutoffQuery.append(searchSettings.facetSubfieldDelimiter).append('*');
		hierarchyQuery.addFilterQuery(cutoffQuery.toString());

		try {
			results = this.executeSearch(hierarchyQuery, hierarchyState, false, false);
			browseResults.setSearchResultResponse(results);
		} catch (SolrServerException e) {
			LOG.error("Error while getting container results for hierarchical browse results", e);
			return null;
		}
		
		////////////////////////////////////////////////////////
		//Get the root node for this search
		if (!noRootNode){
			BriefObjectMetadataBean rootNode = getObjectById(new SimpleIdRequest(
					((HierarchicalFacet)browseRequest.getSearchState().getFacets().get(SearchFieldKeys.ANCESTOR_PATH)).getSearchKey(),
					browseRequest.getAccessGroups()));
			browseResults.getResultList().add(0, rootNode);
		}
		
		////////////////////////////////////////////////////////
		//Get the children counts per container	
		//Clear the resource types list for reuse purposes
		SearchState searchState = new SearchState(browseRequest.getSearchState());
		SearchRequest searchRequest = new SearchRequest(searchState, accessGroups);
		
		if (searchState.getResultFields() != null){
			if (!searchState.getResultFields().contains(SearchFieldKeys.ANCESTOR_PATH))
				searchState.getResultFields().add(SearchFieldKeys.ANCESTOR_PATH);
			if (!searchState.getResultFields().contains(SearchFieldKeys.ANCESTOR_PATH))
				searchState.getResultFields().add(SearchFieldKeys.RESOURCE_TYPE);
		}
		
		//No search results
		searchState.setRowsPerPage(0);
		
		List<String> facetsToRetrieve = new ArrayList<String>();
		facetsToRetrieve.add(SearchFieldKeys.ANCESTOR_PATH);
		searchState.setFacetsToRetrieve(facetsToRetrieve);
		
		//Get all available facet values
		HashMap<String,Integer> facetLimits = new HashMap<String,Integer>();
		facetLimits.put(SearchFieldKeys.ANCESTOR_PATH, new Integer(-1));
		searchState.setFacetLimits(facetLimits);
		
		//Disable facet prefixing since we want child counts without depth restrictions
		searchRequest.setApplyFacetPrefixes(false);
		searchRequest.setApplyFacetCutoffs(false);
		
		//Store resource types then set to null for overriding later
		String resourceTypeFilter = this.getResourceTypeFilter(searchState.getResourceTypes());
		searchState.setResourceTypes(null);
		
		//Get the children count search query so far.
		SolrQuery childQuery = this.generateSearch(searchRequest, true);
		
		//Set the resource type filter query and restore it to the search state
		if (resourceTypeFilter != null)
			childQuery.addFilterQuery(resourceTypeFilter);
		
		//Enable faceting for ancestorPath
		childQuery.setFacet(true);
		childQuery.setFacetMinCount(1);
		
		//Get the children counts per contain
		QueryResponse queryResponse;
		try {
			queryResponse = this.executeQuery(childQuery);
			//Populate children result counts
			assignChildrenCounts(queryResponse, browseResults.getResultList());
			browseResults.setRootCount(queryResponse.getResults().getNumFound());
			
			///////////////////////////////////////////////////////
			//Get the sub-container count per container
			hierarchyQuery.setRows(0);
			
			hierarchyQuery.setFacet(true);
			hierarchyQuery.setFacetMinCount(1);
			hierarchyQuery.addFacetField(solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH));
			
			hierarchyQuery.setFacetLimit(-1);

			queryResponse = this.executeQuery(hierarchyQuery);
			browseResults.populateSubcontainerCounts(queryResponse.getFacetFields());
			
			boolean containsPathFacet = searchState.getFacets().containsKey(SearchFieldKeys.ANCESTOR_PATH);
			//If the user has specified any state fields that constitute a search, then clear out all items with 0 results
			if ((searchState.getFacets().size() > 1 && containsPathFacet) 
					|| (searchState.getFacets().size() > 0 && !containsPathFacet) 
					|| searchState.getRangeFields().size() > 0 
					|| searchState.getSearchFields().size() > 0 || searchState.getAccessTypeFilter() != null){
				//Look up containers that match the user query so that they can be retained in the result set
				childQuery.setRows((int)containerCount);
				childQuery.removeFacetField(solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH));
				childQuery.addFilterQuery(cutoffQuery.toString());
				childQuery.setFields(solrSettings.getFieldName(SearchFieldKeys.ID));
				if (resourceTypeFilter != null)
					childQuery.removeFilterQuery(resourceTypeFilter);
				childQuery.addFilterQuery(getResourceTypeFilter(hierarchyState.getResourceTypes()));
				
				queryResponse = this.executeQuery(childQuery);
				browseResults.populateMatchingContainerPids(queryResponse.getResults(), solrSettings.getFieldName(SearchFieldKeys.ID));
				
				browseResults.removeContainersWithoutContents();
			}
			
			if (!noRootNode && browseRequest.getSearchState().getRowsPerPage() > 0 
					&& (browseRequest.getSearchState().getResourceTypes() == null 
					|| browseRequest.getSearchState().getResourceTypes().contains(searchSettings.resourceTypeFile))){
				SearchState fileSearchState = new SearchState(browseRequest.getSearchState());
				List<String> resourceTypes = new ArrayList<String>();
				resourceTypes.add(searchSettings.resourceTypeFile);
				fileSearchState.setResourceTypes(resourceTypes);
				Object ancestorPath = fileSearchState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH);
				if (ancestorPath instanceof HierarchicalFacet){
					((HierarchicalFacet)ancestorPath).setCutoffTier(((HierarchicalFacet)ancestorPath).getHighestTier() + 1);
				}
				fileSearchState.setFacetsToRetrieve(null);
				SearchRequest fileSearchRequest = new SearchRequest(fileSearchState, browseRequest.getAccessGroups());
				SearchResultResponse fileResults = this.getSearchResults(fileSearchRequest);
				browseResults.populateItemResults(fileResults.getResultList());
			}
			
		} catch (SolrServerException e) {
			LOG.error("Error while getting children counts for hierarchical browse", e);
			return null;
		}

		return browseResults; 
	}
	
	public SearchResultResponse getHierarchicalBrowseItemResult(HierarchicalBrowseRequest browseRequest){
		SearchState fileSearchState = new SearchState(browseRequest.getSearchState());
		List<String> resourceTypes = new ArrayList<String>();
		resourceTypes.add(searchSettings.resourceTypeFile);
		fileSearchState.setResourceTypes(resourceTypes);
		Object ancestorPath = fileSearchState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH);
		if (ancestorPath instanceof HierarchicalFacet){
			((HierarchicalFacet)ancestorPath).setCutoffTier(((HierarchicalFacet)ancestorPath).getHighestTier() + 1);
		}
		fileSearchState.setFacetsToRetrieve(null);
		SearchRequest fileSearchRequest = new SearchRequest(fileSearchState, browseRequest.getAccessGroups());
		SearchResultResponse fileResults = this.getSearchResults(fileSearchRequest);
		return fileResults;
	}
	
	/**
	 * Matches hierarchical facets in the search state with those in the facet list.  If
	 * a match is found, then the search state hierarchical facet is overwritten with the result
	 * facet in order to give it a display value.
	 * @param searchState
	 * @param resultResponse
	 */
	public void lookupHierarchicalDisplayValues(SearchState searchState, AccessGroupSet accessGroups){
		if (searchState.getFacets() == null)
			return;
		Iterator<String> facetIt = searchState.getFacets().keySet().iterator();
		while (facetIt.hasNext()){
			String facetKey = facetIt.next();
			if (searchSettings.hierarchicalFacets.contains(facetKey)){
				Object facetValue = searchState.getFacets().get(facetKey);
				if (facetValue instanceof HierarchicalFacet){
					HierarchicalFacet resultFacet = getHierarchicalFacet(facetKey, ((HierarchicalFacet)facetValue).getSearchValue(), accessGroups);
					if (resultFacet != null){
						if (((HierarchicalFacet)facetValue).getCutoffTier() != null)
							resultFacet.setCutoffTier(((HierarchicalFacet)facetValue).getCutoffTier());
						searchState.getFacets().put(facetKey, resultFacet);
					}
				}
			}
		}
	}
}
