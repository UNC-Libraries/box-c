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

import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.RESOURCE_TYPE;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.TITLE_LC;
import static edu.unc.lib.dl.search.solr.util.SolrSettings.sanitize;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.search.solr.exception.SolrRuntimeException;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.util.FacetFieldUtil;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.util.ResourceType;

/**
 * Service that retrieves objects neighboring a selected object.
 *
 * @author bbpennel
 *
 */
public class NeighborQueryService extends AbstractQueryService {

    protected FacetFieldUtil facetFieldUtil;

    /**
     * Retrieves a list of the closest windowSize neighbors within the parent container of the specified object,
     * using title sort order. The first (windowSize - 1)/ 2 neighbors are retrieved to each side
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
            AccessGroupSet principals) {

        // Get the common access restriction clause (starts with "AND ...")
        StringBuilder accessRestrictionClause = new StringBuilder();
        restrictionUtil.add(accessRestrictionClause, principals);

        // Restrict query to files/aggregates and objects within the same parent
        SolrQuery solrQuery = new SolrQuery();

        solrQuery.setFacet(true);

        solrQuery.addFilterQuery(solrField(RESOURCE_TYPE)
                + ":(" + ResourceType.File.name() + " OR " + ResourceType.Work.name() + ")");

        CutoffFacet ancestorPath = metadata.getAncestorPathFacet();
        if (ancestorPath != null) {
            // We want only objects at the same level of the hierarchy
            ancestorPath.setCutoff(ancestorPath.getHighestTier() + 1);

            facetFieldUtil.addToSolrQuery(ancestorPath, solrQuery);
        }

        // Query for a window of results to either side of the target
        solrQuery.setRows(windowSize - 1);
        solrQuery.setFields(new String[0]);

        // Clone base query for reuse in getting succeeding neighbors
        SolrQuery succeedingQuery = solrQuery.getCopy();
        SolrQuery precedingQuery = solrQuery;

        // Limit results to those with titles alphabetically before the target,
        // OR if the title is the same, to ids before the target.
        String pTitlesClause;
        // Only limit to preceding titles if there is a title on the target, [* TO ""] has special meaning in solr
        if (!isBlank(metadata.getTitle())) {
            pTitlesClause = "(%1$s:{* TO \"%2$s\"} OR ";
        } else {
            pTitlesClause = "(";
        }
        String pItemsQuery = format(
                pTitlesClause + "(%1$s:\"%2$s\" AND %3$s:{* TO \"%4$s\"})) AND !%3$s:\"%4$s\"",
                solrField(TITLE_LC), sanitize(metadata.getTitle().toLowerCase()),
                solrField(SearchFieldKeys.ID), metadata.getId());
        // Get set of preceding neighbors
        precedingQuery.setQuery(pItemsQuery + accessRestrictionClause);

        // Sort neighbors using reverse title sort in order to get items closest to target
        addSort(precedingQuery, "title", false);

        List<BriefObjectMetadataBean> precedingNeighbors;
        try {
            QueryResponse queryResponse = executeQuery(precedingQuery);
            precedingNeighbors = queryResponse.getBeans(BriefObjectMetadataBean.class);
            // Reverse order of preceding items from the reverse title sort
            precedingNeighbors = Lists.reverse(precedingNeighbors);
        } catch (SolrServerException e) {
            throw new SolrRuntimeException("Error retrieving Neighboring items: {}" + solrQuery, e);
        }

        // Limit results to those with titles alphabetically after the target,
        // OR if the title is the same, to ids after the target.
        String sItemsQuery = format(
                "(%1$s:{\"%2$s\" TO *} OR (%1$s:\"%2$s\" AND %3$s:{\"%4$s\" TO *})) AND !%3$s:\"%4$s\"",
                solrField(TITLE_LC), sanitize(metadata.getTitle().toLowerCase()),
                solrField(SearchFieldKeys.ID), metadata.getId());
        // Get set of succeeding neighbors
        succeedingQuery.setQuery(sItemsQuery + accessRestrictionClause);

        // Sort neighbors using the title sort
        addSort(succeedingQuery, "title", true);

        List<BriefObjectMetadataBean> succeedingNeighbors;
        try {
            QueryResponse queryResponse = this.executeQuery(succeedingQuery);
            succeedingNeighbors = queryResponse.getBeans(BriefObjectMetadataBean.class);
        } catch (SolrServerException e) {
            throw new SolrRuntimeException("Error retrieving Neighboring items: {}" + solrQuery, e);
        }

        // Construct a result from appropriate numbers of preceding records, the target,
        // and succeeding reports
        List<BriefObjectMetadataBean> results = new ArrayList<>();

        // Expected number of objects to either side of target if sufficient available
        int precedingHalfWindow = (int) Math.ceil((windowSize - 1) / 2.0);
        // succeeding window size rounds down if uneven number of neighbors needed
        int succeedingHalfWindow = (int) Math.floor((windowSize - 1) / 2.0);

        int lenP = precedingNeighbors.size();
        int lenS = succeedingNeighbors.size();
        // Number of objects more or less than ideal window size
        int extraP = lenP - precedingHalfWindow;
        int extraS = lenS - succeedingHalfWindow;

        // Add preceding neighbors
        if (extraP <= 0) {
            // Number of preceding neighbors does not exceed half the window, use all
            results.addAll(precedingNeighbors);
        } else {
            // More preceding neighbors than needed, calculate start of subset to include
            // If there are fewer succeeding neighbors than needed, expand preceding subset
            int pStart = lenP - precedingHalfWindow + (extraS < 0 ? extraS : 0);
            if (pStart < 0) {
                pStart = 0;
            }
            results.addAll(precedingNeighbors.subList(pStart, lenP));
        }

        // Add the target item into the results
        results.add(metadata);

        // Add succeeding neighbors
        if (extraS <= 0) {
            // Number of succeeding neighbors does not exceed half the window, use all
            results.addAll(succeedingNeighbors);
        } else {
            // Possibly more succeeding neighbors than needed, determine subset to include
            // If there was fewer preceding neighbors than needed, expand succeeding subset.
            int sEnd = succeedingHalfWindow - (extraP < 0 ? extraP : 0);
            if (sEnd > lenS) {
                sEnd = lenS;
            }
            results.addAll(succeedingNeighbors.subList(0, sEnd));
        }

        return results;
    }

    /**
     * @param facetFieldUtil the facetFieldUtil to set
     */
    public void setFacetFieldUtil(FacetFieldUtil facetFieldUtil) {
        this.facetFieldUtil = facetFieldUtil;
    }
}
