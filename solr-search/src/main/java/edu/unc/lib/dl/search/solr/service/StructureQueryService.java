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
import edu.unc.lib.dl.search.solr.exception.SolrRuntimeException;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseRequest;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;
import edu.unc.lib.dl.search.solr.model.HierarchicalFacetNode;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
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
        HierarchicalBrowseResultResponse browseResponse = new HierarchicalBrowseResultResponse();

        AccessGroupSet principals = browseRequest.getAccessGroups();

        // Start hierarchy tree at root of the repository
        BriefObjectMetadata contentRootMd = getContentRootMetadata(principals);
        browseResponse.getResultList().add(contentRootMd);

        // Get the path of the root object being requested for iteration
        CutoffFacet path = getObjectPath(browseRequest.getRootPid(), principals);
        List<HierarchicalFacetNode> pathFacetNodes = path.getFacetNodes();
        if (pathFacetNodes == null) {
            return browseResponse;
        }

        // Path facet used to find immediate children of ancestor containers, built up one tier at a time
        CutoffFacet stepPath = new CutoffFacet(ANCESTOR_PATH.name(), asList(), 0);
        // Retrieve immediate children of all objects in the hierarchy leading up to the target container
        for (HierarchicalFacetNode stepFacetNode : pathFacetNodes) {
            // Add the next tier's identifier to the search facet
            stepPath.addNode(stepFacetNode);

            SearchState stepState = new SearchState(browseRequest.getSearchState());
            // Get the list of objects for the current tier
            HierarchicalBrowseRequest stepRequest = new HierarchicalBrowseRequest(stepState, 1,
                    principals);
            HierarchicalBrowseResultResponse tierResponse = retrieveTier(stepRequest, stepPath);

            // Add results for this tier to the combined result list
            browseResponse.populateItemResults(tierResponse.getResultList());
        }

        assignCounts(browseRequest.getSearchState(), principals, browseResponse.getResultList());
        filterToDirectMatches(browseRequest, browseResponse);
        browseResponse.generateResultTree();
        retrieveFacets(browseRequest, browseResponse);

        return browseResponse;
    }

    /**
     * Retrieves structure of the provided root, including immediate children of the object.
     *
     * Results are populated with counts of number of subcontainers.
     * If no root is provided, the root of the content tree is selected.
     *
     * @param browseRequest
     * @return
     */
    public HierarchicalBrowseResultResponse getHierarchicalBrowseResults(HierarchicalBrowseRequest browseRequest) {
        AccessGroupSet principals = browseRequest.getAccessGroups();

        // Retrieve the root node for this request and use it for the structure search
        BriefObjectMetadata rootMd = getRootMetadata(browseRequest.getRootPid(), principals);
        CutoffFacet rootPath = rootMd.getPath();

        HierarchicalBrowseResultResponse browseResponse = new HierarchicalBrowseResultResponse();
        browseResponse.setSelectedContainer(rootMd);
        browseResponse.getResultList().add(rootMd);

        HierarchicalBrowseResultResponse childResults = retrieveTier(browseRequest, rootPath);
        browseResponse.populateItemResults(childResults.getResultList());

        assignCounts(browseRequest.getSearchState(), principals, browseResponse.getResultList());
        filterToDirectMatches(browseRequest, browseResponse);
        browseResponse.generateResultTree();
        retrieveFacets(browseRequest, browseResponse);

        return browseResponse;
    }

    /*
     * Gets metadata for the requested root object. Defaults to the root of the
     * content tree if no rootId is provided.
     */
    private BriefObjectMetadata getRootMetadata(String rootId, AccessGroupSet principals) {
        BriefObjectMetadata rootNode = null;
        if (rootId != null) {
            rootNode = searchService.getObjectById(new SimpleIdRequest(rootId, principals));
        }
        // Default the root to the collections object so we always have a root
        if (rootNode == null) {
            rootNode = getContentRootMetadata(principals);
        }

        return rootNode;
    }

    private BriefObjectMetadata getContentRootMetadata(AccessGroupSet principals) {
        return searchService.getObjectById(new SimpleIdRequest(
                getContentRootPid().getId(), principals));
    }

    /**
     * Retrieves a single tier of results as a HierarchicalBrowseResultResponse.
     *
     * @param browseRequest
     * @param tierPath path facet identifying the container whose children will be retrieved.
     * @return
     */
    private HierarchicalBrowseResultResponse retrieveTier(HierarchicalBrowseRequest browseRequest,
            CutoffFacet tierPath) {
        SearchState browseState = (SearchState) browseRequest.getSearchState().clone();
        AccessGroupSet principals = browseRequest.getAccessGroups();
        HierarchicalBrowseResultResponse browseResults = new HierarchicalBrowseResultResponse();

        SearchState hierarchyState = searchStateFactory.createHierarchyListSearchState();
        // Use the ancestor path facet from the state where we will have set a default value
        hierarchyState.getFacets().put(ANCESTOR_PATH.name(), tierPath);
        hierarchyState.setRowsPerPage(0);

        SearchRequest hierarchyRequest = new SearchRequest(hierarchyState, principals, false);

        SolrQuery baseQuery = searchService.generateSearch(hierarchyRequest);
        // Get the set of all applicable containers
        SolrQuery hierarchyQuery = baseQuery.getCopy();
        hierarchyQuery.setRows(new Integer(searchSettings.getProperty("search.results.maxBrowsePerPage")));

        // Reusable query segment for limiting the results to the immediate tier results
        StringBuilder cutoffQuery = new StringBuilder();
        cutoffQuery.append('!').append(solrField(ANCESTOR_PATH)).append(":");
        cutoffQuery.append(tierPath.getHighestTier() + 1);
        cutoffQuery.append(searchSettings.facetSubfieldDelimiter).append('*');
        hierarchyQuery.addFilterQuery(cutoffQuery.toString());

        SearchResultResponse results;
        try {
            results = searchService.executeSearch(hierarchyQuery, hierarchyState, false, false);
            browseResults.setSearchResultResponse(results);

            // If requested, retrieve works and files restricted to a max number per page
            if (browseRequest.isIncludeFiles() && browseState.getRowsPerPage() > 0) {
                SearchState fileSearchState = new SearchState(browseState);
                fileSearchState.setFacetsToRetrieve(null);
                fileSearchState.setResourceTypes(asList(Work.name(), ResourceType.File.name()));
                SearchRequest fileSearchRequest = new SearchRequest(fileSearchState, principals, false);
                SolrQuery fileSearchQuery = searchService.generateSearch(fileSearchRequest);

                SearchResultResponse fileResults = searchService.executeSearch(
                        fileSearchQuery, hierarchyState, false, false);

                browseResults.getResultList().addAll(fileResults.getResultList());
            }
        } catch (SolrServerException e) {
            throw new SolrRuntimeException("Error while getting container results for hierarchical query: "
                    + hierarchyQuery, e);
        }

        return browseResults;
    }

    /*
     * Retrieves facet results for the browse request if requested.
     */
    private void retrieveFacets(HierarchicalBrowseRequest browseRequest,
            HierarchicalBrowseResultResponse browseResults) {
        if (browseRequest.isRetrieveFacets() && browseRequest.getSearchState().getFacetsToRetrieve() != null) {
            log.debug("Retrieving facets for structure browse");
            SearchState facetState = (SearchState) browseRequest.getSearchState().clone();
            facetState.setRowsPerPage(0);
            SearchRequest facetRequest = new SearchRequest(facetState, browseRequest.getAccessGroups(), true);
            SolrQuery facetQuery = searchService.generateSearch(facetRequest);
            try {
                SearchResultResponse facetResponse = searchService.executeSearch(facetQuery, facetState, true, false);
                browseResults.setFacetFields(facetResponse.getFacetFields());
            } catch (SolrServerException e) {
                throw new SolrRuntimeException("Failed to retrieve facet results for" + facetQuery.toString(), e);
            }
        }
    }

    /*
     * Calculates and assigns child and container counts to each object in the result list
     */
    private void assignCounts(SearchState browseState, AccessGroupSet principals, List<BriefObjectMetadata> results) {
        // Get the children counts per container
        SearchRequest filteredChildrenRequest =
                new SearchRequest(browseState, principals, true);
        SolrQuery filteredChildrenQuery = searchService.generateSearch(filteredChildrenRequest);
        SolrQuery filteredContainerQuery = filteredChildrenQuery.getCopy();

        // Get count of of children objects
        filteredChildrenQuery.addFilterQuery(makeFilter(RESOURCE_TYPE,
                asList(AdminUnit.name(), Collection.name(), Folder.name(), Work.name())));
        childrenCountService.addChildrenCounts(results, principals,
                CHILD_COUNT, filteredChildrenQuery);

        // Get count of container child objects only
        filteredContainerQuery.addFilterQuery(makeFilter(RESOURCE_TYPE,
            asList(AdminUnit.name(), Collection.name(), Folder.name())));
        childrenCountService.addChildrenCounts(results, principals,
                CONTAINERS_COUNT, filteredContainerQuery);
    }

    /*
     * If the request contains search limits, reduces the structure result to
     * only return results which directly matched those limits.
     */
    private void filterToDirectMatches(HierarchicalBrowseRequest browseRequest,
            HierarchicalBrowseResultResponse browseResults) {
        if (!browseRequest.getSearchState().isPopulatedSearch()) {
            return;
        }
        try {
            SearchState browseState = browseRequest.getSearchState();
            browseResults.setMatchingContainerPids(getDirectContainerMatches(browseState,
                    browseRequest.getAccessGroups()));
            browseResults.getMatchingContainerPids().add(browseRequest.getRootPid());
            // Remove all containers that are not direct matches for the user's query and have 0 children
            browseResults.removeContainersWithoutContents();
        } catch (SolrServerException e) {
            throw new SolrRuntimeException("Error while getting children counts for hierarchical browse", e);
        }
    }

    /**
     * Returns a set of object IDs for containers that directly matched the restrictions from the base query.
     */
    private Set<String> getDirectContainerMatches(SearchState baseState, AccessGroupSet accessGroups)
            throws SolrServerException {

        SearchState directMatchState = (SearchState) baseState.clone();
        directMatchState.setResourceTypes(null);
        directMatchState.setResultFields(asList(SearchFieldKeys.ID.name()));
        directMatchState.setRowsPerPage(searchSettings.getMaxBrowsePerPage());

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

    /*
     * Returns the path facet of the object identified by pid.
     */
    private CutoffFacet getObjectPath(String pid, AccessGroupSet accessGroups) {
        List<String> resultFields = asList(SearchFieldKeys.ID.name(), ANCESTOR_PATH.name());
        SimpleIdRequest idRequest = new SimpleIdRequest(pid, resultFields, accessGroups);

        try {
            BriefObjectMetadataBean rootNode = searchService.getObjectById(idRequest);
            return rootNode.getPath();
        } catch (Exception e) {
            throw new SolrRuntimeException("Error while retrieving Solr entry for " + pid, e);
        }
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
