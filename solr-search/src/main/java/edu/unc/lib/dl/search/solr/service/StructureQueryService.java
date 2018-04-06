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

import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.getContentRootPid;
import static edu.unc.lib.dl.search.solr.service.ChildrenCountService.CHILD_COUNT;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.ANCESTOR_PATH;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.RESOURCE_TYPE;
import static edu.unc.lib.dl.util.ResourceType.AdminUnit;
import static edu.unc.lib.dl.util.ResourceType.Collection;
import static edu.unc.lib.dl.util.ResourceType.Folder;
import static edu.unc.lib.dl.util.ResourceType.Work;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseRequest;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse.ResultNode;
import edu.unc.lib.dl.search.solr.model.HierarchicalFacetNode;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ResourceType;

/**
 * Query service for producing structural views of search results.
 *
 * @author bbpennel
 *
 */
public class StructureQueryService extends AbstractQueryService {
    private static final Logger log = LoggerFactory.getLogger(StructureQueryService.class);

    public static final String CONTAINERS_COUNT = "containers";

    private ChildrenCountService childrenCountService;

    private SolrSearchService searchService;

    private SearchStateFactory searchStateFactory;

    /**
     * Retrieves the structure tree from the root of the repository to the root
     * of the browse request. Each level of the tree retrieved will be expanded
     * with its immediate children.
     *
     * @param browseRequest
     * @return
     */
    public HierarchicalBrowseResultResponse getExpandedStructurePath(HierarchicalBrowseRequest browseRequest) {
        HierarchicalBrowseResultResponse browseResponse = null;
        HierarchicalBrowseResultResponse previousResponse = null;

        CutoffFacet path = getAncestorPath(browseRequest.getRootPid(), browseRequest.getAccessGroups());
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
                    log.warn("Could not locate child entry {} inside of results for {}",
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
     * Retrieves structure starting at the provided root, including immediate children of the object.
     *
     * Results are populated with counts of number of subcontainers.
     * If no root is provided, the root of the content tree is selected.
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
            rootNode = searchService.getObjectById(new SimpleIdRequest(browseRequest.getRootPid(),
                    browseRequest.getAccessGroups()));
            if (rootNode != null) {
                rootPath = rootNode.getPath();
                browseState.getFacets().put(ANCESTOR_PATH.name(), rootPath);
                browseResults.setSelectedContainer(rootNode);
            }
        }
        // Default the ancestor path to the collections object so we always have a root
        if (rootNode == null) {
            rootPath = (CutoffFacet) browseState.getFacets().get(ANCESTOR_PATH.name());
            if (rootPath == null) {
                rootPath = new CutoffFacet(ANCESTOR_PATH.name(),
                        "1," + getContentRootPid().getId());
                browseState.getFacets().put(ANCESTOR_PATH.name(), rootPath);
            }

            rootNode = searchService.getObjectById(new SimpleIdRequest(rootPath.getSearchKey(),
                    browseRequest.getAccessGroups()));
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
        hierarchyState.getFacets().put(ANCESTOR_PATH.name(), rootPath);
        hierarchyState.setRowsPerPage(0);

        SearchRequest hierarchyRequest = new SearchRequest(hierarchyState, accessGroups, false);

        SolrQuery baseQuery = searchService.generateSearch(hierarchyRequest);
        // Get the set of all applicable containers
        SolrQuery hierarchyQuery = baseQuery.getCopy();
        hierarchyQuery.setRows(new Integer(searchSettings.getProperty("search.results.maxBrowsePerPage")));

        // Reusable query segment for limiting the results to just the depth asked for
        StringBuilder cutoffQuery = new StringBuilder();
        cutoffQuery.append('!').append(solrField(ANCESTOR_PATH)).append(":");
        cutoffQuery.append(rootPath.getHighestTier() + browseRequest.getRetrievalDepth());
        cutoffQuery.append(searchSettings.facetSubfieldDelimiter).append('*');
        hierarchyQuery.addFilterQuery(cutoffQuery.toString());

        SearchResultResponse results;
        try {
            results = searchService.executeSearch(hierarchyQuery, hierarchyState, false, false);
            browseResults.setSearchResultResponse(results);
        } catch (SolrServerException e) {
            log.error("Error while getting container results for hierarchical browse results", e);
            return null;
        }
        // Add the root node into the result set
        browseResults.getResultList().add(0, rootNode);

        if (browseRequest.isRetrieveFacets() && browseRequest.getSearchState().getFacetsToRetrieve() != null) {
            SearchState facetState = (SearchState) browseState.clone();
            facetState.setRowsPerPage(0);
            SearchRequest facetRequest = new SearchRequest(facetState, browseRequest.getAccessGroups(), true);
            SolrQuery facetQuery = searchService.generateSearch(facetRequest);
            try {
                SearchResultResponse facetResponse = searchService.executeSearch(facetQuery, facetState, true, false);
                browseResults.setFacetFields(facetResponse.getFacetFields());
            } catch (SolrServerException e) {
                log.warn("Failed to retrieve facet results for " + facetQuery.toString(), e);
            }
        }

        // Don't need to manipulate the container list any further unless either the root is a real record or there are
        // subcontainers
        if (!rootIsAStub || results.getResultCount() > 0) {
            // Get the children counts per container
            SearchRequest filteredChildrenRequest =
                    new SearchRequest(browseState, browseRequest.getAccessGroups(), true);
            SolrQuery filteredChildrenQuery = searchService.generateSearch(filteredChildrenRequest);
            SolrQuery filteredContainerQuery = filteredChildrenQuery.getCopy();

            // Get count of of children objects
            filteredChildrenQuery.addFilterQuery(makeFilter(RESOURCE_TYPE,
                    asList(AdminUnit.name(), Collection.name(), Folder.name(), Work.name())));
            childrenCountService.addChildrenCounts(results.getResultList(), accessGroups,
                    CHILD_COUNT, filteredChildrenQuery);

            // Get count of container child objects only
            filteredContainerQuery.addFilterQuery(makeFilter(RESOURCE_TYPE,
                asList(AdminUnit.name(), Collection.name(), Folder.name())));
            childrenCountService.addChildrenCounts(results.getResultList(), accessGroups,
                    CONTAINERS_COUNT, filteredContainerQuery);

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
                log.error("Error while getting children counts for hierarchical browse", e);
                return null;
            }
        }

        // Retrieve normal item search results, which are restricted to a max number per page
        if (browseRequest.isIncludeFiles() && browseState.getRowsPerPage() > 0) {
            SearchState fileSearchState = new SearchState(browseState);
            fileSearchState.setResourceTypes(asList(Work.name(), ResourceType.File.name()));
            CutoffFacet ancestorPath =
                    (CutoffFacet) fileSearchState.getFacets().get(ANCESTOR_PATH.name());
            ancestorPath.setCutoff(rootPath.getHighestTier() + 1);
            fileSearchState.setFacetsToRetrieve(null);
            SearchRequest fileSearchRequest = new SearchRequest(fileSearchState, browseRequest.getAccessGroups());
            SearchResultResponse fileResults = searchService.getSearchResults(fileSearchRequest);
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
        SolrQuery directMatchQuery = searchService.generateSearch(directMatchRequest);
        QueryResponse directMatchResponse = this.executeQuery(directMatchQuery);
        String idField = solrField(SearchFieldKeys.ID);
        Set<String> directMatchIds = new HashSet<>(directMatchResponse.getResults().size());
        for (SolrDocument document : directMatchResponse.getResults()) {
            directMatchIds.add((String) document.getFirstValue(idField));
        }
        return directMatchIds;
    }

    public HierarchicalBrowseResultResponse getStructureTier(SearchRequest browseRequest) {
        SearchState searchState = new SearchState(browseRequest.getSearchState());

        CutoffFacet ancestorPath = (CutoffFacet) searchState.getFacets().get(ANCESTOR_PATH.name());
        if (ancestorPath != null) {
            ancestorPath.setCutoff(ancestorPath.getHighestTier() + 1);
        } else {
            ancestorPath = new CutoffFacet(ANCESTOR_PATH.name(), "1,*");
            searchState.getFacets().put(ANCESTOR_PATH.name(), ancestorPath);
        }

        searchState.setFacetsToRetrieve(null);
        SearchRequest fileSearchRequest = new SearchRequest(searchState, browseRequest.getAccessGroups());
        SearchResultResponse fileResults = searchService.getSearchResults(fileSearchRequest);

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
     * Gets the ancestor path facet for the provided pid, given the access groups provided.
     *
     * @param pid
     * @param accessGroups
     * @return
     */
    private CutoffFacet getAncestorPath(String pid, AccessGroupSet accessGroups) {
        List<String> resultFields = new ArrayList<>();
        resultFields.add(ANCESTOR_PATH.name());

        SimpleIdRequest idRequest = new SimpleIdRequest(pid, resultFields, accessGroups);

        BriefObjectMetadataBean rootNode = null;
        try {
            rootNode = searchService.getObjectById(idRequest);
        } catch (Exception e) {
            log.error("Error while retrieving Solr entry for ", e);
        }
        if (rootNode == null) {
            return null;
        }
        return rootNode.getAncestorPathFacet();
    }

    /**
     * @param childrenCountService the childrenCountService to set
     */
    public void setChildrenCountService(ChildrenCountService childrenCountService) {
        this.childrenCountService = childrenCountService;
    }

    /**
     * @param searchService the searchService to set
     */
    public void setSearchService(SolrSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * @param searchStateFactory the searchStateFactory to set
     */
    public void setSearchStateFactory(SearchStateFactory searchStateFactory) {
        this.searchStateFactory = searchStateFactory;
    }
}
