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
package edu.unc.lib.boxc.web.common.services;

import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.PrincipalClassifier;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.ObjectPathFactory;
import edu.unc.lib.boxc.search.solr.services.SearchStateFactory;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.unc.lib.boxc.web.common.utils.SearchConstants.MAX_COLLECTIONS_TO_RETRIEVE;

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
        searchState.setResourceTypes(SearchSettings.DEFAULT_COLLECTION_RESOURCE_TYPES);
        searchState.setRowsPerPage(MAX_COLLECTIONS_TO_RETRIEVE);
        searchState.setFacetsToRetrieve(null);
        List<String> resultFields = new ArrayList<>();
        resultFields.add(SearchFieldKey.ANCESTOR_PATH.name());
        resultFields.add(SearchFieldKey.TITLE.name());
        resultFields.add(SearchFieldKey.ID.name());
        searchState.setResultFields(resultFields);

        searchRequest.setSearchState(searchState);
        return getSearchResults(searchRequest);
    }

    /**
     * Determines if the user has adminRole permissions on any items
     *
     * @param accessGroups
     * @return
     */
    public boolean hasAdminViewPermission(AccessGroupSet accessGroups) {
        StringBuilder query = new StringBuilder();
        String joinedGroups = accessGroups.stream()
            .filter(p -> !PrincipalClassifier.isPatronPrincipal(p))
            .map(p -> p.replaceAll("\\:", "\\\\:"))
            .collect(Collectors.joining(" OR "));
        query.append("adminGroup:(").append(joinedGroups).append(')');

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query.toString());
        // Only take into account permissions provided from collections and units
        solrQuery.addFacetQuery(SearchFieldKey.RESOURCE_TYPE.getSolrField()
                + ":(" + ResourceType.Collection.name() + " " + ResourceType.AdminUnit.name() + ")");

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
        } else if (!searchState.getIgnoreMaxRows() && searchState.getRowsPerPage() > searchSettings.getMaxPerPage()) {
            searchState.setRowsPerPage(searchSettings.getMaxPerPage());
        }

        Boolean rollup = searchState.getRollup();

        ContentObjectRecord selectedContainer = null;
        // Get the record for the currently selected container if one is selected.
        if (searchRequest.getRootPid() != null) {
            selectedContainer = addSelectedContainer(searchRequest.getRootPid(), searchState,
                    searchRequest.isApplyCutoffs(), searchRequest.getAccessGroups());
        } else if (rollup == null) {
            LOG.debug("No container and no rollup, defaulting rollup to true");
            searchState.setRollup(true);
        }

        // Retrieve search results
        SearchResultResponse resultResponse = getSearchResults(searchRequest);

        resultResponse.setSelectedContainer(selectedContainer);

        searchRequest.setSearchState(originalState);
        return resultResponse;
    }

    public void setSearchStateFactory(SearchStateFactory searchStateFactory) {
        this.searchStateFactory = searchStateFactory;
    }

    /**
     * Get the number of objects present in the collection for various formats
     *
     * @return a map from format name to count
     */
    public Map<String, Long> getFormatCounts(AccessGroupSet accessGroups) {
        try {
            SolrQuery query = new SolrQuery();

            query.setQuery("*:*");
            addAccessRestrictions(query, accessGroups);
            query.setRows(0);
            query.addFacetField(SearchFieldKey.FILE_FORMAT_CATEGORY.getSolrField());
            query.setFacetLimit(-1);

            QueryResponse response = this.executeQuery(query);
            FacetField facetField = response.getFacetField(SearchFieldKey.FILE_FORMAT_CATEGORY.getSolrField());
            return facetField.getValues().stream().collect(
                    Collectors.toMap(c -> c.getName().toLowerCase(), Count::getCount));
        } catch (SolrServerException | AccessRestrictionException e) {
            LOG.error("Error retrieving format counts", e);
        }

        return new HashMap<String, Long>();
}

    public static String getWriteRoleFilter(AccessGroupSet groups) {
        StringBuilder roleString = new StringBuilder();

        roleString.append('(');

        for (String group : groups) {
            String saneGroup = SolrSettings.sanitize(group);
            roleString.append(UserRole.canManage.getPredicate()).append('|').append(saneGroup).append(' ');
            roleString.append(UserRole.unitOwner.getPredicate()).append('|').append(saneGroup).append(' ');
        }

        roleString.append(')');

        return roleString.toString();
    }

    public void setPathFactory(ObjectPathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }

}
