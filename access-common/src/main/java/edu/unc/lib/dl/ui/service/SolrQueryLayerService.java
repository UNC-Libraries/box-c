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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.search.solr.model.AbstractHierarchicalFacet;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.CaseInsensitiveFacet;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.CutoffFacetNode;
import edu.unc.lib.dl.search.solr.model.FacetFieldObject;
import edu.unc.lib.dl.search.solr.model.GenericFacet;
import edu.unc.lib.dl.search.solr.model.IdListRequest;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

import edu.unc.lib.dl.acl.util.AccessGroupConstants;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseRequest;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;
import edu.unc.lib.dl.ui.util.AccessUtil;
import edu.unc.lib.dl.util.ContentModelHelper;

/**
 * Solr query construction layer. Constructs search states specific to common tasks before passing them on to lower
 * level classes to retrieve the results.
 * 
 * @author bbpennel
 */
public class SolrQueryLayerService extends SolrSearchService {
	private static final Logger LOG = LoggerFactory.getLogger(SolrQueryLayerService.class);
	protected SearchStateFactory searchStateFactory;
	protected PID collectionsPid;

	/**
	 * Returns a list of the most recently added items in the collection
	 * 
	 * @param accessGroups
	 * @return Result response, where items only contain title and id.
	 */
	public SearchResultResponse getNewlyAdded(AccessGroupSet accessGroups) {
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setAccessGroups(accessGroups);

		SearchState searchState = searchStateFactory.createTitleListSearchState();
		List<String> resourceTypes = new ArrayList<String>();
		resourceTypes.add(searchSettings.resourceTypeCollection);
		searchState.setResourceTypes(resourceTypes);
		searchState.setRowsPerPage(searchSettings.defaultListResultsPerPage);
		searchState.setSortType("dateAdded");

		searchRequest.setSearchState(searchState);
		return getSearchResults(searchRequest);
	}

	/**
	 * Returns a list of collections
	 * 
	 * @param accessGroups
	 * @return
	 */
	public SearchResultResponse getCollectionList(AccessGroupSet accessGroups) {
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setAccessGroups(accessGroups);

		SearchState searchState = searchStateFactory.createSearchState();
		searchState.setResourceTypes(searchSettings.defaultCollectionResourceTypes);
		searchState.setRowsPerPage(50);
		searchState.setFacetsToRetrieve(null);
		ArrayList<String> resultFields = new ArrayList<String>();
		resultFields.add(SearchFieldKeys.ANCESTOR_PATH.name());
		resultFields.add(SearchFieldKeys.TITLE.name());
		resultFields.add(SearchFieldKeys.ID.name());
		searchState.setResultFields(resultFields);

		searchRequest.setSearchState(searchState);
		return getSearchResults(searchRequest);
	}

	public FacetFieldObject getDepartmentList(AccessGroupSet accessGroups) {
		SearchState searchState = searchStateFactory.createFacetSearchState(SearchFieldKeys.DEPARTMENT.name(), "index",
				Integer.MAX_VALUE);

		SearchRequest searchRequest = new SearchRequest(searchState, accessGroups);

		SearchResultResponse results = getSearchResults(searchRequest);

		if (results.getFacetFields() != null && results.getFacetFields().size() > 0) {
			FacetFieldObject deptField = results.getFacetFields().get(0);
			if (deptField != null) {
				CaseInsensitiveFacet.deduplicateCaseInsensitiveValues(deptField);
			}
			return deptField;
		}
		return null;
	}

	/**
	 * Retrieves the facet list for the search defined by searchState. The facet results optionally can ignore
	 * hierarchical cutoffs.
	 * 
	 * @param searchState
	 * @param facetsToRetrieve
	 * @param applyCutoffs
	 * @return
	 */
	public SearchResultResponse getFacetList(SearchState baseState, AccessGroupSet accessGroups,
			List<String> facetsToRetrieve, boolean applyCutoffs) {
		SearchState searchState = (SearchState) baseState.clone();

		CutoffFacet ancestorPath;
		LOG.debug("Retrieving facet list");
		if (!searchState.getFacets().containsKey(SearchFieldKeys.ANCESTOR_PATH.name())) {
			ancestorPath = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH.name(), "2,*");
			ancestorPath.setFacetCutoff(3);
			searchState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), ancestorPath);
		} else {
			ancestorPath = (CutoffFacet) searchState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH.name());
			if (ancestorPath.getFacetCutoff() == null)
				ancestorPath.setFacetCutoff(ancestorPath.getHighestTier() + 1);
		}

		if (!applyCutoffs) {
			ancestorPath.setCutoff(null);
		}

		// Turning off rollup because it is really slow
		searchState.setRollup(false);

		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setAccessGroups(accessGroups);
		searchRequest.setSearchState(searchState);

		searchState.setRowsPerPage(0);
		if (facetsToRetrieve != null)
			searchState.setFacetsToRetrieve(facetsToRetrieve);
		searchState.setResourceTypes(null);

		SearchResultResponse resultResponse = getSearchResults(searchRequest);

		// If this facet list contains parent collections, then get further metadata about them
		if (resultResponse.getFacetFields() != null
				&& (searchState.getFacetsToRetrieve() == null || searchState.getFacetsToRetrieve().contains(
						SearchFieldKeys.PARENT_COLLECTION.name()))) {
			FacetFieldObject parentCollectionFacet = resultResponse.getFacetFields().get(
					SearchFieldKeys.PARENT_COLLECTION.name());
			List<BriefObjectMetadataBean> parentCollectionValues = getParentCollectionValues(resultResponse
					.getFacetFields().get(SearchFieldKeys.PARENT_COLLECTION.name()), accessGroups);
			int i;
			// If the parent collection facet yielded further metadata, then edit the original facet value to contain the
			// additional metadata
			if (parentCollectionFacet != null && parentCollectionValues != null) {
				for (GenericFacet pidFacet : parentCollectionFacet.getValues()) {
					String pid = pidFacet.getSearchValue();
					for (i = 0; i < parentCollectionValues.size() && !pid.equals(parentCollectionValues.get(i).getId()); i++)
						;
					if (i < parentCollectionValues.size()) {
						CutoffFacet parentPath = parentCollectionValues.get(i).getPath();
						pidFacet.setFieldName(SearchFieldKeys.ANCESTOR_PATH.name());
						pidFacet.setDisplayValue(parentPath.getDisplayValue());
						pidFacet.setValue(parentPath.getSearchValue());
					}
				}
			}
		}

		return resultResponse;
	}

	/**
	 * Retrieves metadata fields for the parent collection pids contained by the supplied facet object.
	 * 
	 * @param parentCollectionFacet
	 *           Facet object containing parent collection ids to lookup
	 * @param accessGroups
	 * @return
	 */
	public List<BriefObjectMetadataBean> getParentCollectionValues(FacetFieldObject parentCollectionFacet,
			AccessGroupSet accessGroups) {
		if (parentCollectionFacet == null || parentCollectionFacet.getValues() == null
				|| parentCollectionFacet.getValues().size() == 0) {
			return null;
		}

		QueryResponse queryResponse = null;
		SolrQuery solrQuery = new SolrQuery();
		StringBuilder query = new StringBuilder();
		boolean first = true;

		query.append('(');
		for (GenericFacet pidFacet : parentCollectionFacet.getValues()) {
			if (pidFacet.getSearchValue() != null && pidFacet.getSearchValue().length() > 0) {
				if (first) {
					first = false;
				} else {
					query.append(" OR ");
				}
				query.append(solrSettings.getFieldName(SearchFieldKeys.ID.name())).append(':')
						.append(SolrSettings.sanitize(pidFacet.getSearchValue()));
			}
		}
		query.append(')');

		// If no pids were added to the query, then there's nothing to look up
		if (first) {
			return null;
		}

		try {
			// Add access restrictions to query
			addAccessRestrictions(query, accessGroups);
		} catch (AccessRestrictionException e) {
			// If the user doesn't have any access groups, they don't have access to anything, return null.
			LOG.error("No access groups", e);
			return null;
		}

		solrQuery.setQuery(query.toString());

		solrQuery.setFacet(true);
		solrQuery.setFields(solrSettings.getFieldName(SearchFieldKeys.ID.name()),
				solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH.name()),
				solrSettings.getFieldName(SearchFieldKeys.TITLE.name()));

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
	 * Retrieves a list of the nearest windowSize neighbors within the nearest parent collection or folder around the
	 * item metadata, based on the order field of the item. The first windowSize - 1 neighbors are retrieved to each side
	 * of the item, and trimmed so that there are always windowSize - 1 neighbors surrounding the item if possible. If no
	 * order field is available, a list of arbitrary windowSize neighbors is returned.
	 * 
	 * @param metadata
	 *           Record which the window pivots around.
	 * @param windowSize
	 *           max number of items in the window. This includes the pivot, so odd numbers are recommended.
	 * @param accessGroups
	 *           Access groups of the user making this request.
	 * @return
	 */
	public List<BriefObjectMetadataBean> getNeighboringItems(BriefObjectMetadataBean metadata, int windowSize,
			AccessGroupSet accessGroups) {

		// Get the common access restriction clause (starts with "AND ...")

		StringBuilder accessRestrictionClause = new StringBuilder();

		try {
			addAccessRestrictions(accessRestrictionClause, accessGroups);
		} catch (AccessRestrictionException e) {
			// If the user doesn't have any access groups, they don't have access to anything, return null.
			LOG.error(e.getMessage());
			return null;
		}

		// Prepare the common query object, including a filter for resource type and the
		// facet which selects only the item's siblings.

		SolrQuery solrQuery = new SolrQuery();

		solrQuery.setFacet(true);
		solrQuery.addFilterQuery(solrSettings.getFieldName(SearchFieldKeys.RESOURCE_TYPE.name()) + ":File "
				+ solrSettings.getFieldName(SearchFieldKeys.RESOURCE_TYPE.name()) + ":Aggregate");

		CutoffFacet ancestorPath = null;

		if (metadata.getResourceType().equals(searchSettings.resourceTypeFile)
				|| metadata.getResourceType().equals(searchSettings.resourceTypeAggregate)) {
			ancestorPath = metadata.getAncestorPathFacet();
		} else {
			ancestorPath = metadata.getPath();
		}

		if (ancestorPath != null) {
			// We want only objects at the same level of the hierarchy
			ancestorPath.setCutoff(ancestorPath.getHighestTier() + 1);

			facetFieldUtil.addToSolrQuery(ancestorPath, solrQuery);
		}

		// If this item has no display order, get arbitrary items surrounding it.

		Long pivotOrder = metadata.getDisplayOrder();

		if (pivotOrder == null) {

			LOG.debug("No display order, just querying for " + windowSize + " siblings");

			StringBuilder query = new StringBuilder();

			List<BriefObjectMetadataBean> list = null;

			query.append("*:*");
			query.append(accessRestrictionClause);
			solrQuery.setQuery(query.toString());

			solrQuery.setStart(0);
			solrQuery.setRows(windowSize);

			solrQuery.setSortField(solrSettings.getFieldName(SearchFieldKeys.DISPLAY_ORDER.name()), SolrQuery.ORDER.desc);

			try {
				QueryResponse queryResponse = this.executeQuery(solrQuery);
				list = queryResponse.getBeans(BriefObjectMetadataBean.class);
			} catch (SolrServerException e) {
				LOG.error("Error retrieving Neighboring items: " + e);
				return null;
			}

			return list;

			// Otherwise, query for items surrounding this item.

		} else {

			LOG.debug("Display order is " + pivotOrder);

			// Find the right and left lists

			StringBuilder query;

			List<BriefObjectMetadataBean> leftList = null;
			List<BriefObjectMetadataBean> rightList = null;

			solrQuery.setStart(0);
			solrQuery.setRows(windowSize - 1);

			// Right list

			query = new StringBuilder();

			query.append(solrSettings.getFieldName(SearchFieldKeys.DISPLAY_ORDER.name())).append(":[")
					.append(pivotOrder + 1).append(" TO *]");
			query.append(accessRestrictionClause);
			solrQuery.setQuery(query.toString());

			solrQuery.setSortField(solrSettings.getFieldName(SearchFieldKeys.DISPLAY_ORDER.name()), SolrQuery.ORDER.asc);

			try {
				QueryResponse queryResponse = this.executeQuery(solrQuery);
				rightList = queryResponse.getBeans(BriefObjectMetadataBean.class);
			} catch (SolrServerException e) {
				LOG.error("Error retrieving Neighboring items: " + e);
				return null;
			}

			LOG.debug("Got " + rightList.size() + " items for right list");

			// Left list

			// (Note that display order stuff is reversed.)

			query = new StringBuilder();

			query.append(solrSettings.getFieldName(SearchFieldKeys.DISPLAY_ORDER.name())).append(":[* TO ")
					.append(pivotOrder - 1).append("]");
			query.append(accessRestrictionClause);
			solrQuery.setQuery(query.toString());

			solrQuery.setSortField(solrSettings.getFieldName(SearchFieldKeys.DISPLAY_ORDER.name()), SolrQuery.ORDER.desc);

			try {
				QueryResponse queryResponse = this.executeQuery(solrQuery);
				leftList = queryResponse.getBeans(BriefObjectMetadataBean.class);
			} catch (SolrServerException e) {
				LOG.error("Error retrieving Neighboring items: " + e);
				return null;
			}

			LOG.debug("Got " + leftList.size() + " items for left list");

			// Trim the lists

			int halfWindow = windowSize / 2;

			// If we have enough in both lists, trim both to be
			// halfWindow long.

			if (leftList.size() >= halfWindow && rightList.size() >= halfWindow) {

				LOG.debug("Trimming both lists");

				leftList.subList(halfWindow, leftList.size()).clear();
				rightList.subList(halfWindow, rightList.size()).clear();

				// If we don't have enough in the left list and we have extra in the right list,
				// try to pick up the slack by trimming fewer items from the right list.

			} else if (leftList.size() < halfWindow && rightList.size() > halfWindow) {

				LOG.debug("Picking up slack from right list");

				// How much extra do we need from the right list?

				int extra = halfWindow - leftList.size();

				// Only "take" the extra (ie, clear less of the right list) if we have it available.

				if (halfWindow + extra < rightList.size())
					rightList.subList(halfWindow + extra, rightList.size()).clear();

			} else if (rightList.size() < halfWindow && leftList.size() > halfWindow) {

				LOG.debug("Picking up slack from left list");

				int extra = halfWindow - rightList.size();

				if (halfWindow + extra < leftList.size())
					leftList.subList(halfWindow + extra, leftList.size()).clear();

			}

			// (Otherwise, we do no trimming, since both lists are smaller or the same size
			// as the window.)

			// Assemble the result.

			Collections.reverse(leftList);
			leftList.add(metadata);
			leftList.addAll(rightList);

			return leftList;

		}

	}

	/**
	 * Returns the number of children plus a facet list for the parent defined by ancestorPath.
	 * 
	 * @param ancestorPath
	 * @param accessGroups
	 * @return
	 */
	public SearchResultResponse getFullRecordSupplementalData(CutoffFacet ancestorPath, AccessGroupSet accessGroups,
			List<String> facetsToRetrieve) {
		SearchState searchState = searchStateFactory.createSearchState();
		searchState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), ancestorPath);
		searchState.setRowsPerPage(0);
		return getFacetList(searchState, accessGroups, facetsToRetrieve, false);
	}

	public long getChildrenCount(BriefObjectMetadataBean metadataObject, AccessGroupSet accessGroups) {
		QueryResponse queryResponse = null;
		SolrQuery solrQuery = new SolrQuery();
		StringBuilder query = new StringBuilder("*:* ");

		try {
			// Add access restrictions to query
			addAccessRestrictions(query, accessGroups);
		} catch (AccessRestrictionException e) {
			// If the user doesn't have any access groups, they don't have access to anything, return null.
			LOG.error(e.getMessage());
			return -1;
		}

		solrQuery.setStart(0);
		solrQuery.setRows(0);

		solrQuery.setQuery(query.toString());

		query = new StringBuilder();
		query.append(solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH.name())).append(':')
				.append(SolrSettings.sanitize(metadataObject.getPath().getSearchValue())).append(",*");

		solrQuery.setFacet(true);
		solrQuery.addFilterQuery(query.toString());

		try {
			queryResponse = this.executeQuery(solrQuery);
			return queryResponse.getResults().getNumFound();
		} catch (SolrServerException e) {
			LOG.error("Error retrieving Solr search result request: " + e);
		}
		return -1;
	}

	/**
	 * Populates the child count attributes of all metadata objects in the given search result response by querying for
	 * all non-folder objects which have the metadata object's highest ancestor path tier somewhere in its ancestor path.
	 * 
	 * @param resultResponse
	 * @param accessGroups
	 */
	public void getChildrenCounts(List<BriefObjectMetadata> resultList, AccessGroupSet accessGroups) {
		this.getChildrenCounts(resultList, accessGroups, "child", null, null);
	}
	
	public void getChildrenCounts(List<BriefObjectMetadata> resultList, AccessGroupSet accessGroups, String countName,
			String queryAddendum, SolrQuery baseQuery) {
		long startTime = System.currentTimeMillis();
		if (resultList == null || resultList.size() == 0)
			return;

		String ancestorPathField = solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH.name());
		SolrQuery solrQuery;
		if (baseQuery == null) {
			// Create a base query since we didn't receive one
			solrQuery = new SolrQuery();
			StringBuilder query = new StringBuilder("*:*");
			try {
				// Add access restrictions to query
				addAccessRestrictions(query, accessGroups);
			} catch (AccessRestrictionException e) {
				// If the user doesn't have any access groups, they don't have access to anything, return null.
				LOG.error(e.getMessage());
				return;
			}

			solrQuery.setStart(0);
			solrQuery.setRows(0);

			solrQuery.setQuery(query.toString());
		} else {
			// Starting from a base query
			solrQuery = baseQuery.getCopy();
			// Make sure we aren't returning any normal results
			solrQuery.setRows(0);
			// Remove all facet fields so we are only getting ancestor path
			if (solrQuery.getFacetFields() != null) {
				for (String facetField : solrQuery.getFacetFields()) {
					solrQuery.removeFacetField(facetField);
				}
			}
		}

		if (queryAddendum != null) {
			solrQuery.setQuery(solrQuery.getQuery() + " AND " + queryAddendum);
		}
		
		solrQuery.setFacet(true);
		solrQuery.setFacetMinCount(1);
		solrQuery.addFacetField(ancestorPathField);

		Integer countPageSize;
		try {
			countPageSize = new Integer(searchSettings.getProperty("search.facet.countPageSize"));
		} catch (NumberFormatException e) {
			countPageSize = 20;
		}
		
		solrQuery.add("f." + ancestorPathField + ".facet.limit", Integer.toString(Integer.MAX_VALUE));
		// Sort by value rather than count so that earlier tiers will come first in case the result gets cut off
		solrQuery.setFacetSort("index");
		
		java.util.Map<Integer, StringBuilder> tierQueryMap = new java.util.HashMap<Integer, StringBuilder>();
		java.util.Map<Integer, List<BriefObjectMetadata>> containerMap = new java.util.HashMap<Integer, List<BriefObjectMetadata>>();
		
		// Pare the list of ids we are searching for and assigning counts to down to just containers
		for (BriefObjectMetadata metadataObject : resultList) {
			if (metadataObject.getPath() != null && metadataObject.getContentModel() != null
					&& metadataObject.getContentModel().contains(ContentModelHelper.Model.CONTAINER.toString())) {
				CutoffFacetNode highestTier = metadataObject.getPath().getHighestTierNode();
				StringBuilder tierQuery = tierQueryMap.get(highestTier.getTier());
				List<BriefObjectMetadata> containerObjects = containerMap.get(highestTier.getTier());
				if (tierQuery == null) {
					tierQuery = new StringBuilder();
					tierQueryMap.put(highestTier.getTier(), tierQuery);
					
					containerObjects = new ArrayList<BriefObjectMetadata>();
					containerMap.put(highestTier.getTier(), containerObjects);
				}
				
				if (tierQuery.length() == 0) {
					tierQuery.append(ancestorPathField).append(":(");
				} else {
					tierQuery.append(" OR ");
				}
				
				tierQuery.append(SolrSettings.sanitize(highestTier.getSearchValue())).append(",*");
				containerObjects.add(metadataObject);
				
				// If there are a lot of results, then do a partial lookup
				if (containerObjects.size() >= countPageSize) {
					tierQuery.append(")");
					this.executeChildrenCounts(tierQuery, containerObjects, solrQuery, countName, highestTier.getTier());
					LOG.info("Partial query done at " + System.currentTimeMillis() + " (" + (System.currentTimeMillis() - startTime) + ")");
					containerMap.remove(highestTier.getTier());
					tierQueryMap.remove(highestTier.getTier());
				}
			}
		}

		Iterator<java.util.Map.Entry<Integer,StringBuilder>> queryIt = tierQueryMap.entrySet().iterator();
		while (queryIt.hasNext()) {
			java.util.Map.Entry<Integer,StringBuilder> tierQueryEntry = queryIt.next();
			tierQueryEntry.getValue().append(')');
			this.executeChildrenCounts(tierQueryEntry.getValue(), containerMap.get(tierQueryEntry.getKey()), solrQuery, countName, tierQueryEntry.getKey());
		}
		LOG.info("Child count query done at " + System.currentTimeMillis() + " (" + (System.currentTimeMillis() - startTime) + ")");
	}
	
	private void executeChildrenCounts(StringBuilder query, List<BriefObjectMetadata> containerObjects, SolrQuery solrQuery, String countName, Integer tier) {
		String ancestorPathField = solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH.name());
		// Remove all ancestor path related filter queries or filter queries from previous count executions, so the counts won't be cut off
		if (solrQuery.getFilterQueries() != null) {
			for (String filterQuery : solrQuery.getFilterQueries()) {
				if (filterQuery.contains(ancestorPathField)) {
					solrQuery.removeFilterQuery(filterQuery);
				}
			}
		}
		if (tier != null) {
			solrQuery.remove("f." + ancestorPathField + ".facet.prefix");
			solrQuery.add("f." + ancestorPathField + ".facet.prefix", tier + ",");
		}
		solrQuery.addFilterQuery(query.toString());
		try {
			long startTime = System.currentTimeMillis();
			QueryResponse queryResponse = this.executeQuery(solrQuery);
			LOG.info("Query executed in " + (System.currentTimeMillis() - startTime));
			assignChildrenCounts(queryResponse.getFacetField(ancestorPathField), containerObjects, countName);
		} catch (SolrServerException e) {
			LOG.error("Error retrieving Solr search result request: " + e);
		}
	}

	/**
	 * Assigns children counts to container objects from ancestor path facet results based on matching search values
	 * 
	 * @param facetField
	 * @param containerObjects
	 * @param countName
	 */
	protected void assignChildrenCounts(FacetField facetField, List<BriefObjectMetadata> containerObjects,
			String countName) {
		if (facetField.getValues() != null) {
			boolean binarySearch = facetField.getValues().size() > 64;
			for (BriefObjectMetadata container : containerObjects) {
				// Find the facet count for this container, either using a binary or linear search
				String searchValue = container.getPath().getSearchValue();
				int matchIndex = -1;
				if (binarySearch) {
					matchIndex = Collections.binarySearch(facetField.getValues(), searchValue, new Comparator<Object>() {
						@Override
						public int compare(Object currentFacetValueObject, Object searchValueObject) {
							if (searchValueObject == null)
								throw new NullPointerException();
							String searchValue = (String) searchValueObject;
							Count facetValue = (Count) currentFacetValueObject;
							return facetValue.getName().indexOf(searchValue) == 0 ? 0 : facetValue.getName().compareTo(
									searchValue);
						}
					});
				} else {
					for (int i = 0; i < facetField.getValues().size(); i++) {
						Count facetValue = facetField.getValues().get(i);
						if (facetValue.getName().indexOf(searchValue) == 0) {
							matchIndex = i;
							break;
						}
					}
				}
				if (matchIndex > -1) {
					container.getCountMap().put(countName, facetField.getValues().get(matchIndex).getCount());
				}
			}
		}
	}

	public HierarchicalBrowseResultResponse getStructureToParentCollection(HierarchicalBrowseRequest browseRequest) {
		HierarchicalBrowseResultResponse requestResponse = getHierarchicalBrowseResults(browseRequest);
		BriefObjectMetadata requestRoot = requestResponse.getRootNode().getMetadata();
		
		// Tree does not need to be expanded further if the root is a collection, it has no parent collection, or it is just below the collection root
		if (requestRoot.getAncestorPath() == null || requestRoot.getAncestorPath().size() <= 1 || requestRoot.getParentCollection() == null || searchSettings.resourceTypeCollection.equals(requestRoot.getResourceType()))
			return requestResponse;
		
		// Retrieve on tier of structure for the parent collection.
		HierarchicalBrowseRequest collectionRequest = new HierarchicalBrowseRequest(browseRequest.getSearchState(), 1, browseRequest.getAccessGroups());
		collectionRequest.setRootPid(requestRoot.getParentCollection());
		HierarchicalBrowseResultResponse collectionResponse = getHierarchicalBrowseResults(collectionRequest);
		
		int parentCollectionTier = requestRoot.getParentCollectionObject().getTier();
		int collectionToRequestDistance = requestRoot.getAncestorPath().size() - parentCollectionTier;
		// The requested root is a child of the collection root, so insert the request tree as a child
		if (collectionToRequestDistance == 0) {
			int childNodeIndex = collectionResponse.getChildNodeIndex(requestRoot.getPid().getPid());
			collectionResponse.getRootNode().getChildren().set(childNodeIndex, requestResponse.getRootNode());
			return collectionResponse;
		}
		
		//MERGE THESE?  Make sure that distance of 0, 1 and greater all work
		if (collectionToRequestDistance == 1) {
			int childNodeIndex = collectionResponse.getChildNodeIndex(requestRoot.getAncestorPathFacet().getSearchKey());
			collectionResponse.getRootNode().getChildren().set(childNodeIndex, requestResponse.getRootNode());
			return collectionResponse;
		}
		
		List<String> idList = new ArrayList<String>();
		// If the distance between the collection root and request root is bigger than 1, then fill in records for all the intermediaries
		for (int i = parentCollectionTier + 1; i < requestRoot.getAncestorPath().size(); i++) {
			idList.add(requestRoot.getAncestorPathFacet().getFacetNodes().get(i).getSearchKey());
		}
		List<BriefObjectMetadata> bridgeMetadata = this.getObjectsById(new IdListRequest(idList, null, browseRequest.getAccessGroups()));
		this.getChildrenCounts(bridgeMetadata, browseRequest.getAccessGroups());
		
		// Guarantee sort order of the bridge items by structure depth
		Collections.sort(bridgeMetadata, new Comparator<BriefObjectMetadata>() {
			@Override
			public int compare(BriefObjectMetadata arg0, BriefObjectMetadata arg1) {
				return arg0.getAncestorPath().size() - arg1.getAncestorPath().size();
			}
		});
		
		// Build tree-line containing all the bridge folders
		HierarchicalBrowseResultResponse.ResultNode bridgeRoot = new HierarchicalBrowseResultResponse.ResultNode(bridgeMetadata.get(0));
		HierarchicalBrowseResultResponse.ResultNode currentNode = bridgeRoot;
		for (int i = 1; i < bridgeMetadata.size(); i++)
			currentNode = currentNode.addChild(bridgeMetadata.get(i));
		
		// Join the bridge to the original request tree
		currentNode.getChildren().add(requestResponse.getRootNode());
		
		// Insert the bridge into the collection tree
		int bridgeRootParentIndex = collectionResponse.getChildNodeIndex(bridgeRoot.getMetadata().getAncestorPathFacet().getSearchKey());
		collectionResponse.getRootNode().getChildren().get(bridgeRootParentIndex).getChildren().add(bridgeRoot);
		
		return collectionResponse;
	}

	/**
	 * Retrieves results for populating a hierarchical browse view. Supports all the regular navigation available to
	 * searches. Results contain child counts for each item (all items returned are containers), and a map containing the
	 * number of nested subcontainers per container. Children counts are retrieved based on facet counts.
	 * 
	 * @param browseRequest
	 * @return
	 */
	public HierarchicalBrowseResultResponse getHierarchicalBrowseResults(HierarchicalBrowseRequest browseRequest) {
		AccessGroupSet accessGroups = browseRequest.getAccessGroups();
		SearchState browseState = (SearchState) browseRequest.getSearchState().clone();

		CutoffFacet rootPath;
		BriefObjectMetadataBean rootNode;
		if (browseRequest.getRootPid() != null) {
			rootNode = getObjectById(new SimpleIdRequest(browseRequest.getRootPid(), browseRequest.getAccessGroups()));
			rootPath = rootNode.getPath();
			browseState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), rootPath);
		} else {
			// Default the ancestor path to the collections object so we always have a root
			rootPath = (CutoffFacet) browseState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH.name());
			if (rootPath == null) {
				rootPath = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH.name(), "1," + this.collectionsPid.getPid());
				browseState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), rootPath);
			}

			rootNode = getObjectById(new SimpleIdRequest(rootPath.getSearchKey(), browseRequest.getAccessGroups()));
		}
		boolean rootIsAStub = rootNode == null;
		if (rootIsAStub) {
			// Parent is not found, but children are, so make a stub for the parent.
			rootNode = new BriefObjectMetadataBean();
			rootNode.setId(rootPath.getSearchKey());
			rootNode.setAncestorPathFacet(rootPath);
		}

		HierarchicalBrowseResultResponse browseResults = new HierarchicalBrowseResultResponse();
		SearchState hierarchyState = searchStateFactory.createHierarchyListSearchState();
		// Use the ancestor path facet from the state where we will have set a default value
		hierarchyState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), rootPath);

		hierarchyState.setRowsPerPage(0);

		SearchRequest hierarchyRequest = new SearchRequest(hierarchyState, accessGroups);

		SolrQuery baseQuery = this.generateSearch(hierarchyRequest, false);
		// Get the set of all applicable containers
		SolrQuery hierarchyQuery = baseQuery.getCopy();
		hierarchyQuery.setRows(new Integer(searchSettings.getProperty("search.results.maxBrowsePerPage")));

		// Reusable query segment for limiting the results to just the depth asked for
		StringBuilder cutoffQuery = new StringBuilder();
		cutoffQuery.append('!').append(solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH.name())).append(":");
		cutoffQuery.append(rootPath.getHighestTier() + browseRequest.getRetrievalDepth());
		cutoffQuery.append(searchSettings.facetSubfieldDelimiter).append('*');
		hierarchyQuery.addFilterQuery(cutoffQuery.toString());

		SearchResultResponse results;
		try {
			results = this.executeSearch(hierarchyQuery, hierarchyState, false, false);
			browseResults.setSearchResultResponse(results);
		} catch (SolrServerException e) {
			LOG.error("Error while getting container results for hierarchical browse results", e);
			return null;
		}
		// Add the root node into the result set
		browseResults.getResultList().add(0, rootNode);

		// Don't need to manipulate the container list any further unless either the root is a real record or there are
		// subcontainers
		if (!rootIsAStub || results.getResultCount() > 0) {
			// Get the children counts per container
			SearchRequest filteredChildrenRequest = new SearchRequest(browseState, browseRequest.getAccessGroups());
			this.getChildrenCounts(results.getResultList(), accessGroups, "child", null,
					this.generateSearch(filteredChildrenRequest, true));

			try {
				// If anything that constituted a search is in the request then trim out possible empty folders
				if (browseState.getFacets().size() > 1 || browseState.getRangeFields().size() > 0
						|| browseState.getSearchFields().size() > 0 || browseState.getAccessTypeFilter() != null) {
					// Get the list of any direct matches for the current query
					browseResults.setMatchingContainerPids(this.getDirectContainerMatches(browseState, accessGroups));
					// Remove all containers that are not direct matches for the user's query and have 0 children
					browseResults.removeContainersWithoutContents();
				}
			} catch (SolrServerException e) {
				LOG.error("Error while getting children counts for hierarchical browse", e);
				return null;
			}
		}

		// Retrieve normal item search results, which are restricted to a max number per page
		if (browseState.getRowsPerPage() > 0
				&& (browseState.getResourceTypes() == null || browseState.getResourceTypes().contains(
						searchSettings.resourceTypeFile))) {
			SearchState fileSearchState = new SearchState(browseState);
			List<String> resourceTypes = new ArrayList<String>();
			resourceTypes.add(searchSettings.resourceTypeFile);
			fileSearchState.setResourceTypes(resourceTypes);
			CutoffFacet ancestorPath = (CutoffFacet) fileSearchState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH.name());
			ancestorPath.setCutoff(rootPath.getHighestTier() + 1);
			fileSearchState.setFacetsToRetrieve(null);
			SearchRequest fileSearchRequest = new SearchRequest(fileSearchState, browseRequest.getAccessGroups());
			SearchResultResponse fileResults = this.getSearchResults(fileSearchRequest);
			browseResults.populateItemResults(fileResults.getResultList());
		}

		browseResults.generateResultTree();

		return browseResults;
	}

	/**
	 * Returns a set of object IDs for containers that directly matched the restrictions from the base query.
	 * 
	 * @param baseState
	 * @param accessGroups
	 * @return
	 * @throws SolrServerException
	 */
	private Set<String> getDirectContainerMatches(SearchState baseState, AccessGroupSet accessGroups)
			throws SolrServerException {
		SearchState directMatchState = (SearchState) baseState.clone();
		directMatchState.setResourceTypes(null);
		directMatchState.setResultFields(Arrays.asList(SearchFieldKeys.ID.name()));
		directMatchState.getFacets().put(SearchFieldKeys.CONTENT_MODEL.name(),
				ContentModelHelper.Model.CONTAINER.toString());
		directMatchState.setRowsPerPage(new Integer(searchSettings.getProperty("search.results.maxBrowsePerPage")));
		SearchRequest directMatchRequest = new SearchRequest(directMatchState, accessGroups);
		SolrQuery directMatchQuery = this.generateSearch(directMatchRequest, false);
		QueryResponse directMatchResponse = this.executeQuery(directMatchQuery);
		String idField = solrSettings.getFieldName(SearchFieldKeys.ID.name());
		Set<String> directMatchIds = new HashSet<String>(directMatchResponse.getResults().size());
		for (SolrDocument document : directMatchResponse.getResults()) {
			directMatchIds.add((String) document.getFirstValue(idField));
		}
		return directMatchIds;
	}

	public HierarchicalBrowseResultResponse getStructureTier(SearchRequest browseRequest) {
		SearchState fileSearchState = new SearchState(browseRequest.getSearchState());

		CutoffFacet ancestorPath = (CutoffFacet) fileSearchState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH.name());
		if (ancestorPath != null) {
			((CutoffFacet) ancestorPath).setCutoff(((CutoffFacet) ancestorPath).getHighestTier() + 1);
		} else {
			ancestorPath = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH.name(), "1,*");
			fileSearchState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), ancestorPath);
		}

		fileSearchState.setFacetsToRetrieve(null);
		SearchRequest fileSearchRequest = new SearchRequest(fileSearchState, browseRequest.getAccessGroups());
		SearchResultResponse fileResults = this.getSearchResults(fileSearchRequest);

		HierarchicalBrowseResultResponse response = new HierarchicalBrowseResultResponse();
		response.setResultList(fileResults.getResultList());

		// Add in a stub root node to top the tree
		BriefObjectMetadataBean rootNode = new BriefObjectMetadataBean();
		rootNode.setId(ancestorPath.getSearchKey());
		rootNode.setAncestorPathFacet(ancestorPath);
		response.getResultList().add(0, rootNode);

		response.generateResultTree();
		return response;
	}

	/**
	 * Matches hierarchical facets in the search state with those in the facet list. If a match is found, then the search
	 * state hierarchical facet is overwritten with the result facet in order to give it a display value.
	 * 
	 * @param searchState
	 * @param resultResponse
	 */
	public void lookupHierarchicalDisplayValues(SearchState searchState, AccessGroupSet accessGroups) {
		if (searchState.getFacets() == null)
			return;
		Iterator<String> facetIt = searchState.getFacets().keySet().iterator();
		while (facetIt.hasNext()) {
			String facetKey = facetIt.next();
			Object facetValue = searchState.getFacets().get(facetKey);
			if (facetValue instanceof AbstractHierarchicalFacet) {
				FacetFieldObject resultFacet = getHierarchicalFacet((AbstractHierarchicalFacet) facetValue, accessGroups);
				if (resultFacet != null) {
					GenericFacet facet = resultFacet.getValues().get(resultFacet.getValues().size() - 1);
					searchState.getFacets().put(facetKey, facet);
					if (facetValue instanceof CutoffFacet) {
						((CutoffFacet) facet).setCutoff(((CutoffFacet) facetValue).getCutoff());
					}
				}
			}
		}
	}

	/**
	 * Checks if an item is accessible given the specified access restrictions
	 * 
	 * @param idRequest
	 * @param accessType
	 * @return
	 */
	public boolean isAccessible(SimpleIdRequest idRequest) {
		QueryResponse queryResponse = null;
		SolrQuery solrQuery = new SolrQuery();
		StringBuilder query = new StringBuilder();

		PID pid = new PID(idRequest.getId());
		String id = pid.getPid();
		String[] idParts = id.split("/");
		String datastream = null;
		if (idParts.length > 1) {
			id = idParts[0];
			datastream = idParts[1];
			solrQuery.addField(solrSettings.getFieldName(SearchFieldKeys.ROLE_GROUP.name()));
		}

		query.append(solrSettings.getFieldName(SearchFieldKeys.ID.name())).append(':').append(SolrSettings.sanitize(id));

		try {
			// Add access restrictions to query
			addAccessRestrictions(query, idRequest.getAccessGroups());
		} catch (AccessRestrictionException e) {
			// If the user doesn't have any access groups, they don't have access to anything, return null.
			LOG.error(e.getMessage());
			return false;
		}

		solrQuery.setQuery(query.toString());
		if (datastream == null)
			solrQuery.setRows(0);
		else
			solrQuery.setRows(1);

		solrQuery.addField(solrSettings.getFieldName(SearchFieldKeys.ID.name()));

		LOG.debug("getObjectById query: " + solrQuery.toString());
		try {
			queryResponse = this.executeQuery(solrQuery);
			if (queryResponse.getResults().getNumFound() == 0)
				return false;
			if (datastream == null)
				return true;

			List<BriefObjectMetadataBean> results = queryResponse.getBeans(BriefObjectMetadataBean.class);
			BriefObjectMetadataBean metadata = results.get(0);

			return AccessUtil.permitDatastreamAccess(idRequest.getAccessGroups(), datastream, metadata);
		} catch (SolrServerException e) {
			LOG.error("Error retrieving Solr object request: " + e);
		}

		return false;
	}

	/**
	 * Determines if the user has adminRole permissions on any items
	 * 
	 * @param accessGroups
	 * @return
	 */
	public boolean hasAdminViewPermission(AccessGroupSet accessGroups) {
		if (accessGroups.contains(AccessGroupConstants.ADMIN_GROUP)) {
			return true;
		}
		StringBuilder query = new StringBuilder();
		String joinedGroups = accessGroups.joinAccessGroups(" OR ", null, true);
		query.append("adminGroup:(").append(joinedGroups).append(')');

		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setQuery(query.toString());
		solrQuery.setRows(0);

		try {
			QueryResponse queryResponse = this.executeQuery(solrQuery);
			return queryResponse.getResults().getNumFound() > 0;
		} catch (SolrServerException e) {
			LOG.error("Error retrieving Solr object request: " + e);
		}
		return false;
	}

	public void setSearchStateFactory(SearchStateFactory searchStateFactory) {
		this.searchStateFactory = searchStateFactory;
	}

	public void setCollectionsPid(PID collectionsPid) {
		this.collectionsPid = collectionsPid;
	}
}
