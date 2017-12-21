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

import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.invalidTerm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AccessGroupConstants;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.AbstractHierarchicalFacet;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.CaseInsensitiveFacet;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.FacetFieldObject;
import edu.unc.lib.dl.search.solr.model.GenericFacet;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseRequest;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse.ResultNode;
import edu.unc.lib.dl.search.solr.model.HierarchicalFacetNode;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.service.ObjectPathFactory;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SolrSettings;
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
    protected ObjectPathFactory pathFactory;

    private static int NEIGHBOR_SEEK_PAGE_SIZE = 500;

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
        List<String> resourceTypes = new ArrayList<>();
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
        searchState.setRowsPerPage(250);
        searchState.setFacetsToRetrieve(null);
        ArrayList<String> resultFields = new ArrayList<>();
        resultFields.add(SearchFieldKeys.ANCESTOR_PATH.name());
        resultFields.add(SearchFieldKeys.TITLE.name());
        resultFields.add(SearchFieldKeys.ID.name());
        searchState.setResultFields(resultFields);

        searchRequest.setSearchState(searchState);
        return getSearchResults(searchRequest);
    }

    public SearchResultResponse getDepartmentList(AccessGroupSet accessGroups, String pid) {
        SearchState searchState;
        Boolean has_pid = (pid != null) ? true : false;

        searchState = searchStateFactory.createFacetSearchState(SearchFieldKeys.DEPARTMENT.name(), "index",
                Integer.MAX_VALUE);

        SearchRequest searchRequest = new SearchRequest(searchState, accessGroups, true);
        searchRequest.setRootPid(pid);
        BriefObjectMetadata selectedContainer = null;

        if (has_pid) {
            selectedContainer = addSelectedContainer(searchRequest.getRootPid(), searchState,
                    false);
        }

        SearchResultResponse results = getSearchResults(searchRequest);

        if (has_pid) {
            results.setSelectedContainer(selectedContainer);
        }

        if (results.getFacetFields() != null && results.getFacetFields().size() > 0) {
            FacetFieldObject deptField = results.getFacetFields().get(0);
            if (deptField != null) {
                CaseInsensitiveFacet.deduplicateCaseInsensitiveValues(deptField);
            }
        }
        return results;
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
    public SearchResultResponse getFacetList(SearchRequest searchRequest) {
        SearchState searchState = (SearchState) searchRequest.getSearchState().clone();

        LOG.debug("Retrieving facet list");
        BriefObjectMetadata selectedContainer = null;
        if (searchRequest.getRootPid() != null) {
            selectedContainer = addSelectedContainer(searchRequest.getRootPid(), searchState,
                    searchRequest.isApplyCutoffs());
        } else {
            CutoffFacet ancestorPath;
            if (!searchState.getFacets().containsKey(SearchFieldKeys.ANCESTOR_PATH.name())) {
                ancestorPath = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH.name(), "2,*");
                ancestorPath.setFacetCutoff(3);
                searchState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), ancestorPath);
            } else {
                ancestorPath = (CutoffFacet) searchState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH.name());
                if (ancestorPath.getFacetCutoff() == null) {
                    ancestorPath.setFacetCutoff(ancestorPath.getHighestTier() + 1);
                }
            }
            if (!searchRequest.isApplyCutoffs()) {
                ancestorPath.setCutoff(null);
            }
        }

        // Turning off rollup because it is really slow
        searchState.setRollup(false);

        SearchRequest facetRequest = new SearchRequest(searchState, true);

        searchState.setRowsPerPage(0);
        searchState.setResourceTypes(null);

        SearchResultResponse resultResponse = getSearchResults(facetRequest);
        resultResponse.setSelectedContainer(selectedContainer);

        // If this facet list contains parent collections, then retrieve display names for them
        if (resultResponse.getFacetFields() != null && (searchState.getFacetsToRetrieve() == null
                || searchState.getFacetsToRetrieve().contains(SearchFieldKeys.PARENT_COLLECTION.name()))) {

            FacetFieldObject parentCollectionFacet = resultResponse.getFacetFields().get(
                    SearchFieldKeys.PARENT_COLLECTION.name());

            if (parentCollectionFacet != null) {
                for (GenericFacet pidFacet : parentCollectionFacet.getValues()) {
                    String parentName = pathFactory.getName(pidFacet.getSearchValue());

                    if (parentName != null) {
                        pidFacet.setFieldName(SearchFieldKeys.ANCESTOR_PATH.name());
                        pidFacet.setDisplayValue(parentName);
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
    public List<BriefObjectMetadataBean> getParentCollectionValues(FacetFieldObject parentCollectionFacet) {
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
            addAccessRestrictions(query);
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
     * Retrieves a list of the closest windowSize neighbors within the parent container of the specified object,
     * using the default sort order. The first windowSize / 2 - 1 neighbors are retrieved to each side
     * of the item, and trimmed so that there are always windowSize - 1 neighbors surrounding the item if possible.
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
            LOG.error("Attempted to get neighboring items without creditentials", e);
            return null;
        }

        // Restrict query to files/aggregates and objects within the same parent
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*:*" + accessRestrictionClause);

        solrQuery.setFacet(true);
        solrQuery.addFilterQuery(solrSettings.getFieldName(SearchFieldKeys.RESOURCE_TYPE.name())
                + ":" + searchSettings.resourceTypeFile + " "
                + solrSettings.getFieldName(SearchFieldKeys.RESOURCE_TYPE.name())
                + ":" + searchSettings.resourceTypeAggregate);

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

        // Sort neighbors using the default sort
        addSort(solrQuery, "default", true);

        // Query for ids in this container in groups of NEIGHBOR_SEEK_PAGE_SIZE until we find the offset of the object
        solrQuery.setRows(NEIGHBOR_SEEK_PAGE_SIZE);
        solrQuery.setFields("id");

        long total = -1;
        int start = 0;
        pageLoop: do {
            try {
                solrQuery.setStart(start);
                QueryResponse queryResponse = this.executeQuery(solrQuery);
                total = queryResponse.getResults().getNumFound();
                for (SolrDocument doc : queryResponse.getResults()) {
                    if (metadata.getId().equals(doc.getFieldValue("id"))) {
                        break pageLoop;
                    }
                    start++;
                }
            } catch (SolrServerException e) {
                LOG.error("Error retrieving Neighboring items: " + e);
                return null;
            }
        } while (start < total);

        // Wasn't found, no neighbors shall be forthcoming
        if (start >= total) {
            return null;
        }

        // Calculate the starting index for the window, so that object is as close to the middle as possible
        long left = start - (windowSize / 2);
        long right = start + (windowSize / 2);

        if (left < 0) {
            right -= left;
            left = 0;
        }

        if (right >= total) {
            left -= (right - total) + 1;
            if (left < 0) {
                left = 0;
            }
        }

        // Query for the windowSize of objects
        solrQuery.setFields(new String[0]);
        solrQuery.setRows(windowSize);
        solrQuery.setStart((int) left);

        try {
            QueryResponse queryResponse = this.executeQuery(solrQuery);
            return queryResponse.getBeans(BriefObjectMetadataBean.class);
        } catch (SolrServerException e) {
            LOG.error("Error retrieving Neighboring items: " + e);
            return null;
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
        searchState.setFacetsToRetrieve(facetsToRetrieve);
        return getFacetList(new SearchRequest(searchState, true));
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
                .append(SolrSettings.sanitize(metadataObject.getPath().getSearchValue()));

        solrQuery.setFacet(true);
        solrQuery.addFilterQuery(query.toString());

        try {
            queryResponse = this.executeQuery(solrQuery);
            return queryResponse.getResults().getNumFound();
        } catch (SolrServerException e) {
            LOG.error("Error retrieving Solr search result request", e);
        }
        return -1;
    }

    /**
     * Populates the child count attributes of all metadata objects in the given search result response by querying for
     * all non-folder objects which have the metadata object's highest ancestor path tier
     * somewhere in its ancestor path.
     *
     * Items in resultList must have their ancestorPaths populated.
     *
     * @param resultList
     * @param accessGroups
     */
    public void getChildrenCounts(List<BriefObjectMetadata> resultList, AccessGroupSet accessGroups) {
        this.getChildrenCounts(resultList, accessGroups, "child", null, null);
    }

    public void getChildrenCounts(List<BriefObjectMetadata> resultList, SearchRequest searchRequest) {
        this.getChildrenCounts(
                resultList, searchRequest.getAccessGroups(), "child", null, this.generateSearch(searchRequest));
    }

    public void getChildrenCounts(List<BriefObjectMetadata> resultList, AccessGroupSet accessGroups, String countName,
            String queryAddendum, SolrQuery baseQuery) {
        long startTime = System.currentTimeMillis();
        if (resultList == null || resultList.size() == 0) {
            return;
        }

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

        solrQuery.add("f." + ancestorPathField + ".facet.limit", Integer.toString(Integer.MAX_VALUE));
        // Sort by value rather than count so that earlier tiers will come first in case the result gets cut off
        solrQuery.setFacetSort("index");

        try {
            startTime = System.currentTimeMillis();
            QueryResponse queryResponse = this.executeQuery(solrQuery);
            LOG.info("Query executed in " + (System.currentTimeMillis() - startTime));
            assignChildrenCounts(queryResponse.getFacetField(ancestorPathField), resultList, countName);
        } catch (SolrServerException e) {
            LOG.error("Error retrieving Solr search result request", e);
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
                    matchIndex = Collections.binarySearch(facetField.getValues(),searchValue,new Comparator<Object>() {
                        @Override
                        public int compare(Object currentFacetValueObject, Object searchValueObject) {
                            if (searchValueObject == null) {
                                throw new NullPointerException();
                            }
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

    /**
     * Retrieves the tree of partially expanded containers from the Collections object up to the rootPID in the request.
     * Only the containers directly in the ancestor path of the starting pid will be expanded
     *
     * @param browseRequest
     * @return
     */
    public HierarchicalBrowseResultResponse getExpandedStructurePath(HierarchicalBrowseRequest browseRequest) {
        HierarchicalBrowseResultResponse browseResponse = null;
        HierarchicalBrowseResultResponse previousResponse = null;

        CutoffFacet path = this.getAncestorPath(browseRequest.getRootPid(), browseRequest.getAccessGroups());
        String currentPID = browseRequest.getRootPid();

        // Retrieve structure results for each tier in the path, in reverse order
        List<HierarchicalFacetNode> nodes = path != null ? path.getFacetNodes() : null;
        int cnt = nodes != null ? nodes.size() : 0;
        while (cnt >= 0) {
            // Get the list of objects for the current tier
            HierarchicalBrowseRequest stepRequest = new HierarchicalBrowseRequest(browseRequest.getSearchState(), 1,
                    browseRequest.getAccessGroups());
            stepRequest.setRootPid(currentPID);
            browseResponse = getHierarchicalBrowseResults(stepRequest);

            if (previousResponse != null) {
                // Store the previous root node as a child in the current tier
                ResultNode previousRoot = previousResponse.getRootNode();
                int childNodeIndex = browseResponse.getChildNodeIndex(previousRoot.getMetadata().getId());
                if (childNodeIndex != -1) {
                    browseResponse.getRootNode().getChildren().set(childNodeIndex, previousRoot);
                } else {
                    LOG.warn("Could not locate child entry {} inside of results for {}",
                            previousRoot.getMetadata().getId(),
                            browseResponse.getRootNode().getMetadata().getId());
                }

            }

            cnt--;
            if (cnt > -1) {
                HierarchicalFacetNode node = nodes.get(cnt);
                currentPID = node.getSearchKey();
                previousResponse = browseResponse;
            }
        }

        return browseResponse;
    }

    /**
     * Retrieves results for populating a hierarchical browse view. Supports all the regular navigation available to
     * searches. Results contain child counts for each item (all items returned are containers),
     * and a map containing the number of nested subcontainers per container.
     * Children counts are retrieved based on facet counts.
     *
     * @param browseRequest
     * @return
     */
    public HierarchicalBrowseResultResponse getHierarchicalBrowseResults(HierarchicalBrowseRequest browseRequest) {
        AccessGroupSet accessGroups = browseRequest.getAccessGroups();
        SearchState browseState = (SearchState) browseRequest.getSearchState().clone();
        HierarchicalBrowseResultResponse browseResults = new HierarchicalBrowseResultResponse();

        CutoffFacet rootPath = null;
        BriefObjectMetadataBean rootNode = null;
        if (browseRequest.getRootPid() != null) {
            rootNode = getObjectById(new SimpleIdRequest(browseRequest.getRootPid(), browseRequest.getAccessGroups()));
            if (rootNode != null) {
                rootPath = rootNode.getPath();
                browseState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), rootPath);
                browseResults.setSelectedContainer(rootNode);
            }
        }
        // Default the ancestor path to the collections object so we always have a root
        if (rootNode == null) {
            rootPath = (CutoffFacet) browseState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH.name());
            if (rootPath == null) {
                rootPath = new CutoffFacet(SearchFieldKeys.ANCESTOR_PATH.name(),
                        "1," + RepositoryPaths.getContentRootPid().getUUID());
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

        SearchState hierarchyState = searchStateFactory.createHierarchyListSearchState();
        // Use the ancestor path facet from the state where we will have set a default value
        hierarchyState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), rootPath);
        hierarchyState.setRowsPerPage(0);

        SearchRequest hierarchyRequest = new SearchRequest(hierarchyState, accessGroups, false);

        SolrQuery baseQuery = this.generateSearch(hierarchyRequest);
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

        if (browseRequest.isRetrieveFacets() && browseRequest.getSearchState().getFacetsToRetrieve() != null) {
            SearchState facetState = (SearchState) browseState.clone();
            facetState.setRowsPerPage(0);
            SearchRequest facetRequest = new SearchRequest(facetState, browseRequest.getAccessGroups(), true);
            SolrQuery facetQuery = this.generateSearch(facetRequest);
            try {
                SearchResultResponse facetResponse = this.executeSearch(facetQuery, facetState, true, false);
                browseResults.setFacetFields(facetResponse.getFacetFields());
            } catch (SolrServerException e) {
                LOG.warn("Failed to retrieve facet results for " + facetQuery.toString(), e);
            }
        }

        // Don't need to manipulate the container list any further unless either the root is a real record or there are
        // subcontainers
        if (!rootIsAStub || results.getResultCount() > 0) {
            // Get the children counts per container
            SearchRequest filteredChildrenRequest =
                    new SearchRequest(browseState, browseRequest.getAccessGroups(), true);
            this.getChildrenCounts(results.getResultList(), accessGroups, "child", null,
                    this.generateSearch(filteredChildrenRequest));

            this.getChildrenCounts(results.getResultList(), accessGroups, "containers",
                    "contentModel:" + SolrSettings.sanitize(ContentModelHelper.Model.CONTAINER.toString()),
                    this.generateSearch(filteredChildrenRequest));

            try {
                // If anything that constituted a search is in the request then trim out possible empty folders
                if (browseState.isPopulatedSearch()) {
                    // Get the list of any direct matches for the current query
                    browseResults.setMatchingContainerPids(this.getDirectContainerMatches(browseState, accessGroups));
                    browseResults.getMatchingContainerPids().add(browseRequest.getRootPid());
                    // Remove all containers that are not direct matches for the user's query and have 0 children
                    browseResults.removeContainersWithoutContents();
                }
            } catch (SolrServerException e) {
                LOG.error("Error while getting children counts for hierarchical browse", e);
                return null;
            }
        }

        // Retrieve normal item search results, which are restricted to a max number per page
        if (browseRequest.isIncludeFiles() && browseState.getRowsPerPage() > 0) {
            browseState.getResourceTypes().add(searchSettings.resourceTypeFile);
            SearchState fileSearchState = new SearchState(browseState);
            List<String> resourceTypes = new ArrayList<>();
            resourceTypes.add(searchSettings.resourceTypeFile);
            fileSearchState.setResourceTypes(resourceTypes);
            CutoffFacet ancestorPath =
                    (CutoffFacet) fileSearchState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH.name());
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
        SearchRequest directMatchRequest = new SearchRequest(directMatchState, accessGroups, false);
        SolrQuery directMatchQuery = this.generateSearch(directMatchRequest);
        QueryResponse directMatchResponse = this.executeQuery(directMatchQuery);
        String idField = solrSettings.getFieldName(SearchFieldKeys.ID.name());
        Set<String> directMatchIds = new HashSet<>(directMatchResponse.getResults().size());
        for (SolrDocument document : directMatchResponse.getResults()) {
            directMatchIds.add((String) document.getFirstValue(idField));
        }
        return directMatchIds;
    }

    public HierarchicalBrowseResultResponse getStructureTier(SearchRequest browseRequest) {
        SearchState fileSearchState = new SearchState(browseRequest.getSearchState());

        CutoffFacet ancestorPath = (CutoffFacet) fileSearchState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH.name());
        if (ancestorPath != null) {
            ancestorPath.setCutoff(ancestorPath.getHighestTier() + 1);
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
     * Matches hierarchical facets in the search state with those in the facet list. If a match is found,
     * then the search state hierarchical facet is overwritten with the result facet
     * in order to give it a display value.
     *
     * @param searchState
     * @param resultResponse
     */
    public void lookupHierarchicalDisplayValues(SearchState searchState, AccessGroupSet accessGroups) {
        if (searchState.getFacets() == null) {
            return;
        }
        Iterator<String> facetIt = searchState.getFacets().keySet().iterator();
        while (facetIt.hasNext()) {
            String facetKey = facetIt.next();
            Object facetValue = searchState.getFacets().get(facetKey);
            if (facetValue instanceof AbstractHierarchicalFacet) {
                FacetFieldObject resultFacet =
                        getHierarchicalFacet((AbstractHierarchicalFacet) facetValue, accessGroups);
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
        String id = pid.getId();
        String[] idParts = id.split("/");
        String datastream = null;
        if (idParts.length > 1) {
            id = idParts[0];
            datastream = idParts[1];
            solrQuery.addField(solrSettings.getFieldName(SearchFieldKeys.ROLE_GROUP.name()));
        }

        query.append(solrSettings.getFieldName(SearchFieldKeys.ID.name()))
                .append(':').append(SolrSettings.sanitize(id));

        try {
            // Add access restrictions to query
            addAccessRestrictions(query, idRequest.getAccessGroups());
        } catch (AccessRestrictionException e) {
            // If the user doesn't have any access groups, they don't have access to anything, return null.
            LOG.error(e.getMessage());
            return false;
        }

        solrQuery.setQuery(query.toString());
        if (datastream == null) {
            solrQuery.setRows(0);
        } else {
            solrQuery.setRows(1);
        }

        solrQuery.addField(solrSettings.getFieldName(SearchFieldKeys.ID.name()));

        LOG.debug("getObjectById query: " + solrQuery.toString());
        try {
            queryResponse = this.executeQuery(solrQuery);
            if (queryResponse.getResults().getNumFound() == 0) {
                return false;
            }
            if (datastream == null) {
                return true;
            }

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

    public boolean hasRole(AccessGroupSet accessGroups, UserRole userRole) {
        StringBuilder query = new StringBuilder();
        String joinedGroups = accessGroups.joinAccessGroups(" OR ", userRole.getPredicate() + "|", true);
        query.append("roleGroup:(").append(joinedGroups).append(')');

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

    public SearchResultResponse performSearch(SearchRequest searchRequest) {
        SearchState searchState = (SearchState) searchRequest.getSearchState().clone();
        SearchState originalState = searchRequest.getSearchState();
        searchRequest.setSearchState(searchState);
        searchState.setFacetsToRetrieve(null);

        // Adjust the number of results to retrieve
        if (searchState.getRowsPerPage() == null || searchState.getRowsPerPage() < 0) {
            searchState.setRowsPerPage(searchSettings.defaultPerPage);
        } else if (searchState.getRowsPerPage() > searchSettings.getMaxPerPage()) {
            searchState.setRowsPerPage(searchSettings.getMaxPerPage());
        }

        Boolean rollup = searchState.getRollup();

        BriefObjectMetadata selectedContainer = null;
        // Get the record for the currently selected container if one is selected.
        if (searchRequest.getRootPid() != null) {
            selectedContainer = addSelectedContainer(searchRequest.getRootPid(), searchState,
                    searchRequest.isApplyCutoffs());
        } else if (rollup == null) {
            LOG.debug("No container and no rollup, defaulting rollup to true");
            searchState.setRollup(true);
        }

        // Retrieve search results
        SearchResultResponse resultResponse = getSearchResults(searchRequest);

        if (resultResponse.getResultCount() == 0 && searchRequest.isApplyCutoffs()
                && searchState.getFacets().containsKey(SearchFieldKeys.ANCESTOR_PATH.name())) {
            ((CutoffFacet) searchState.getFacets().get(SearchFieldKeys.ANCESTOR_PATH.name())).setCutoff(null);
            resultResponse = getSearchResults(searchRequest);
        }

        resultResponse.setSelectedContainer(selectedContainer);

        // Get the children counts for container entries.
        getChildrenCounts(resultResponse.getResultList(), searchRequest.getAccessGroups());

        searchRequest.setSearchState(originalState);
        return resultResponse;
    }

    public void populateBreadcrumbs(SearchRequest searchRequest, SearchResultResponse resultResponse) {
        SearchState searchState = searchRequest.getSearchState();
        if (searchState.getFacets().containsKey(SearchFieldKeys.CONTENT_TYPE.name())) {
            if (resultResponse.getResultCount() == 0 || searchState.getResultFields() == null
                    || !searchState.getResultFields().contains(SearchFieldKeys.CONTENT_TYPE.name())) {
                SearchState contentTypeSearchState = new SearchState();
                contentTypeSearchState.setRowsPerPage(1);
                contentTypeSearchState.getFacets().put(SearchFieldKeys.CONTENT_TYPE.name(),
                        searchState.getFacets().get(SearchFieldKeys.CONTENT_TYPE.name()));
                contentTypeSearchState.setResultFields(Arrays.asList(SearchFieldKeys.CONTENT_TYPE.name()));

                SearchRequest contentTypeRequest =
                        new SearchRequest(contentTypeSearchState, GroupsThreadStore.getGroups());
                SearchResultResponse contentTypeResponse = getSearchResults(contentTypeRequest);
                if (contentTypeResponse.getResultCount() > 0) {
                    resultResponse.extractCrumbDisplayValueFromRepresentative(
                            contentTypeResponse.getResultList().get(0));
                }
            } else {
                resultResponse.extractCrumbDisplayValueFromRepresentative(resultResponse.getResultList().get(0));
            }
        }
    }

    public void setSearchStateFactory(SearchStateFactory searchStateFactory) {
        this.searchStateFactory = searchStateFactory;
    }

    /**
     * Get the number of departments represented in the collection
     *
     * @return the count, or -1 if there was an error retrieving the count
     */

    public int getDepartmentsCount() {

        SolrQuery query;
        QueryResponse response;

        query = new SolrQuery();
        query.setQuery("*:*");
        query.setRows(0);
        query.addFacetField("department");
        query.setFacetLimit(-1);

        try {
            response = this.executeQuery(query);
            return response.getFacetField("department").getValueCount();
        } catch (SolrServerException e) {
            LOG.error("Error retrieving Solr object request: " + e);
        }

        return -1;

    }

    public long getInvalidVocabularyCount(SearchRequest searchRequest) {

        if (searchRequest.getRootPid() != null) {
            addSelectedContainer(searchRequest.getRootPid(), searchRequest.getSearchState(),
                    searchRequest.isApplyCutoffs());
        }

        SolrQuery query = generateSearch(searchRequest);

        query.setQuery(query.getQuery() + " AND " + solrSettings.getFieldName(SearchFieldKeys.RELATIONS.name()) + ":"
                + invalidTerm.getPredicate() + "*");
        query.setRows(0);

        try {
            QueryResponse response = this.executeQuery(query);
            return response.getResults().getNumFound();
        } catch (SolrServerException e) {
            LOG.error("Error retrieving Solr object request: " + e);
        }

        return -1;

    }

    public SearchResultResponse getRelationSet(SearchRequest searchRequest, String relationName) {

        SolrQuery query = generateSearch(searchRequest);

        query.setQuery(query.getQuery() + " AND " + solrSettings.getFieldName(SearchFieldKeys.RELATIONS.name()) + ":"
                + SolrSettings.sanitize(relationName) + "|*");
        query.setRows(1000);

        try {
            return executeSearch(query, searchRequest.getSearchState(), false, false);
        } catch (SolrServerException e) {
            LOG.error("Error retrieving Solr object request: " + e);
        }

        return null;
    }

    /**
     * Get the total number of collections
     *
     * @return the count, or -1 if there was an error retrieving the count
     */

    public long getCollectionsCount() {

        SolrQuery query;
        QueryResponse response;

        query = new SolrQuery();
        query.setQuery("resourceType:Collection");
        query.setRows(0);
        query.setFacetLimit(-1);

        try {
            response = this.executeQuery(query);
            return response.getResults().getNumFound();
        } catch (SolrServerException e) {
            LOG.error("Error retrieving Solr object request: " + e);
        }

        return -1;

    }

    /**
     * Get the number of objects present in the collection for various formats
     *
     * @return a map from format name to count
     */

    public Map<String, Long> getFormatCounts() {

        SolrQuery query;
        QueryResponse response;

        query = new SolrQuery();
        query.setQuery("*:*");
        query.setRows(0);
        query.addFacetField("contentType");
        query.setFacetLimit(-1);

        HashMap<String, Long> counts = new HashMap<>();

        try {
            response = this.executeQuery(query);
            FacetField facetField = response.getFacetField("contentType");

            for (Count count : facetField.getValues()) {

                if (count.getName().startsWith("^text")) {
                    counts.put("text", count.getCount());
                } else if (count.getName().startsWith("^image")) {
                    counts.put("image", count.getCount());
                } else if (count.getName().startsWith("^dataset")) {
                    counts.put("dataset", count.getCount());
                } else if (count.getName().startsWith("^audio")) {
                    counts.put("audio", count.getCount());
                } else if (count.getName().startsWith("^video")) {
                    counts.put("video", count.getCount());
                }

            }

        } catch (SolrServerException e) {
            LOG.error("Error retrieving Solr object request: " + e);
        }

        return counts;

    }

    public static String getWriteRoleFilter(AccessGroupSet groups) {
        StringBuilder roleString = new StringBuilder();

        roleString.append('(');

        for (String group : groups) {
            String saneGroup = SolrSettings.sanitize(group);
            // TODO rewrite to use edu.unc.lib.dl.acl.util.UserRole.getUserRoles(Collection<Permission>)
//            roleString.append(UserRole.processor.getPredicate()).append('|').append(saneGroup).append(' ');
//            roleString.append(UserRole.curator.getPredicate()).append('|').append(saneGroup).append(' ');
//            roleString.append(UserRole.administrator.getPredicate()).append('|').append(saneGroup).append(' ');
        }

        roleString.append(')');

        return roleString.toString();
    }

    public void setPathFactory(ObjectPathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }

}
