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

import static edu.unc.lib.boxc.model.api.ResourceType.AdminUnit;
import static edu.unc.lib.boxc.model.api.ResourceType.Collection;
import static edu.unc.lib.boxc.model.api.ResourceType.File;
import static edu.unc.lib.boxc.model.api.ResourceType.Folder;
import static edu.unc.lib.boxc.model.api.ResourceType.Work;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.ANCESTOR_PATH;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.RESOURCE_TYPE;
import static java.util.Arrays.asList;

import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import edu.unc.lib.boxc.model.api.ResourceType;
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
    public static final List<String> DEFAULT_CHILD_TYPES =
            asList(AdminUnit.name(), Collection.name(), Folder.name(), Work.name());
    private static final List<String> WORK_CHILD_TYPES =
            asList(File.name());

    /**
     * Get the count of child objects within the container, at any depth.
     *
     * @param container BriefMetadataObject whose children will be counted.
     * @param principals agent principals
     * @return count of number of child objects
     */
    public long getChildrenCount(BriefObjectMetadata container, AccessGroupSet principals) {
        SolrQuery solrQuery = createBaseQuery(principals, container, null);

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

        Assert.notNull(containers, "Must provide a list of containers");
        if (containers.size() == 0) {
            return;
        }

        log.debug("Adding child counts of type {} to result list", countKey);

        // Prepare a common base query for each container
        SolrQuery commonQuery = null;
        if (baseQuery != null) {
            // Starting from a base query
            commonQuery = baseQuery.getCopy();
            // Remove all facet fields so we are only getting ancestor path
            String[] facetFields = commonQuery.getFacetFields();
            if (facetFields != null) {
                for (String facetField : facetFields) {
                    commonQuery.removeFacetField(facetField);
                }
            }
        }

        // Calculate the child count for each provided container one at a time
        for (BriefObjectMetadata container : containers) {
            // Skip counts for any file objects
            if (ResourceType.File.name().equals(container.getResourceType())) {
                continue;
            }
            SolrQuery solrQuery = createBaseQuery(principals, container, commonQuery);

            try {
                QueryResponse queryResponse = executeQuery(solrQuery);
                container.getCountMap().put(countKey, queryResponse.getResults().getNumFound());
            } catch (SolrServerException e) {
                throw new SolrRuntimeException(e);
            }
        }
    }

    private SolrQuery createBaseQuery(AccessGroupSet principals, BriefObjectMetadata container, SolrQuery baseQuery) {
        SolrQuery solrQuery;
        if (baseQuery == null) {
            solrQuery = new SolrQuery();

            // Add access restrictions to query
            StringBuilder query = new StringBuilder("*:*");
            restrictionUtil.add(solrQuery, principals);
            solrQuery.setQuery(query.toString());

            if (!Work.equals(container.getResourceType())) {
                // Restrict counts to stand alone content objects
                solrQuery.addFilterQuery(makeFilter(RESOURCE_TYPE, DEFAULT_CHILD_TYPES));
            } else {
                solrQuery.addFilterQuery(makeFilter(RESOURCE_TYPE, WORK_CHILD_TYPES));
            }
        } else {
            solrQuery = baseQuery.getCopy();
        }

        solrQuery.setStart(0);
        solrQuery.setRows(0);
        solrQuery.setFacet(true);

        StringBuilder filterQuery = new StringBuilder();
        addFilter(filterQuery, ANCESTOR_PATH, container.getPath().getSearchValue());
        solrQuery.addFilterQuery(filterQuery.toString());

        return solrQuery;
    }
}
