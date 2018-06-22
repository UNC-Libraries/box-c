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

import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.ANCESTOR_PATH;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.RESOURCE_TYPE;
import static edu.unc.lib.dl.util.ResourceType.AdminUnit;
import static edu.unc.lib.dl.util.ResourceType.Collection;
import static edu.unc.lib.dl.util.ResourceType.Folder;
import static edu.unc.lib.dl.util.ResourceType.Work;
import static java.util.Arrays.asList;
import static java.util.Collections.binarySearch;

import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.search.solr.exception.SolrRuntimeException;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;

/**
 * Service for calculating the number of child objects within containers.
 *
 * @author bbpennel
 *
 */
public class ChildrenCountService extends AbstractQueryService {
    private static final Logger log = LoggerFactory.getLogger(ChildrenCountService.class);

    public static final String CHILD_COUNT = "child";

    /**
     * Get the count of child objects within the container, at any depth.
     *
     * @param container BriefMetadataObject whose children will be counted.
     * @param principals agent principals
     * @return count of number of child objects
     */
    public long getChildrenCount(BriefObjectMetadata container, AccessGroupSet principals) {
        SolrQuery solrQuery = createBaseQuery(principals);

        StringBuilder filterQuery = new StringBuilder();
        addFilter(filterQuery, ANCESTOR_PATH, container.getPath().getSearchValue());
        solrQuery.addFilterQuery(filterQuery.toString());

        try {
            QueryResponse queryResponse = executeQuery(solrQuery);
            return queryResponse.getResults().getNumFound();
        } catch (SolrServerException e) {
            throw new SolrRuntimeException(e);
        }
    }

    /**
     * Adds a count of the number of children contained by each object in result
     * list.
     *
     * This count is stored in the container's count map under the "child" key.
     *
     * @param containers containers to add child counts to.
     * @param principals agent principals
     */
    public void addChildrenCounts(List<BriefObjectMetadata> containers, AccessGroupSet principals) {
        addChildrenCounts(containers, principals, CHILD_COUNT, null);
    }

    /**
     * Adds a count of the number of children contained by each object in result
     * list.
     *
     * The count will be calculated based on any restrictions from baseQuery if
     * it is provided. The count will be stored in each container's count map
     * under the provided countKey.
     *
     * @param containers containers to add child counts to.
     * @param principals agent principals
     * @param countKey key of the count to store.
     * @param baseQuery Optional. Starting query that will be used for
     *            calculating child counts.
     */
    public void addChildrenCounts(List<BriefObjectMetadata> containers, AccessGroupSet principals,
            String countKey, SolrQuery baseQuery) {

        Assert.notNull(containers);
        if (containers.size() == 0) {
            return;
        }

        log.debug("Adding child counts of type {} to result list", countKey);

        SolrQuery solrQuery;
        if (baseQuery == null) {
            // Create a base query since we didn't receive one
            solrQuery = createBaseQuery(principals);
        } else {
            // Starting from a base query
            solrQuery = baseQuery.getCopy();
            // Make sure we aren't returning any normal results
            solrQuery.setRows(0);
            // Remove all facet fields so we are only getting ancestor path
            String[] facetFields = solrQuery.getFacetFields();
            if (facetFields != null) {
                for (String facetField : facetFields) {
                    solrQuery.removeFacetField(facetField);
                }
            }
        }

        // Algorithm for getting child counts for list of objects currently involves
        // retrieving counts for every object in Solr and then matching
        // the counts back to the metadata object using a binary search. We
        // should reevaluate this as things scale, but was introduced as more
        // efficient than hundreds of queries per request.
        String ancestorPathField = solrField(ANCESTOR_PATH);
        solrQuery.setFacetMinCount(1);
        solrQuery.addFacetField(ancestorPathField);

        solrQuery.add("f." + ancestorPathField + ".facet.limit", Integer.toString(Integer.MAX_VALUE));
        // Sort by value rather than count so that earlier tiers will come first in case the result gets cut off
        solrQuery.setFacetSort("index");

        try {
            QueryResponse queryResponse = this.executeQuery(solrQuery);
            assignChildrenCounts(queryResponse.getFacetField(ancestorPathField), containers, countKey);
        } catch (SolrServerException e) {
            throw new SolrRuntimeException(e);
        }
    }

    private SolrQuery createBaseQuery(AccessGroupSet principals) {
        SolrQuery solrQuery = new SolrQuery();

        // Add access restrictions to query
        StringBuilder query = new StringBuilder("*:*");
        restrictionUtil.add(query, principals);
        solrQuery.setQuery(query.toString());

        solrQuery.setStart(0);
        solrQuery.setRows(0);
        solrQuery.setFacet(true);

        // Restrict counts to stand alone content objects
        solrQuery.addFilterQuery(makeFilter(RESOURCE_TYPE,
                asList(AdminUnit.name(), Collection.name(), Folder.name(), Work.name())));
        return solrQuery;
    }

    /**
     * Assigns children counts to container objects from ancestor path facet results based on matching search values
     *
     * @param facetField
     * @param containerObjects
     * @param countName
     */
    private void assignChildrenCounts(FacetField ancestorsField, List<BriefObjectMetadata> containerObjects,
            String countName) {
        if (ancestorsField.getValues() == null) {
            return;
        }

        // Get the list of counts per pid
        List<Count> counts = ancestorsField.getValues();
        // Determine the most efficient algorithm for searching the counts
        boolean binarySearch = counts.size() > 64;
        // Find the count associated with each object in the list of containers
        for (BriefObjectMetadata container : containerObjects) {

            String searchValue = container.getPath().getSearchValue();

            // Find the facet count for this container, either using a binary or linear search
            Count match;
            if (binarySearch) {
                match = binarySearchForMatchingCount(counts, searchValue);
            } else {
                match = counts.stream()
                    .filter(v -> v.getName().indexOf(searchValue) == 0)
                    .findFirst()
                    .orElse(null);
            }

            // Store the matching count on the container
            if (match != null) {
                container.getCountMap().put(countName, match.getCount());
            } else {
                container.getCountMap().put(countName, Long.valueOf(0));
            }
        }
    }

    private Count binarySearchForMatchingCount(List<Count> counts, String searchValue) {
        int matchIndex = binarySearch(counts, searchValue, (countObj, searchValueObj) -> {
            String searchVal = (String) searchValueObj;
            String countId = ((Count) countObj).getName();
            // If the id starts with the search value, return match.  Else, compare and continue.
            return countId.indexOf(searchVal) == 0 ? 0 : countId.compareTo(searchValue);
        });
        return matchIndex < 0 ? null : counts.get(matchIndex);
    }
}
