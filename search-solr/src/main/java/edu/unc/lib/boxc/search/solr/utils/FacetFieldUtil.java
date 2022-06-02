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
package edu.unc.lib.boxc.search.solr.utils;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.facets.CutoffFacetNode;
import edu.unc.lib.boxc.search.api.facets.HierarchicalFacetNode;
import edu.unc.lib.boxc.search.api.facets.SearchFacet;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import edu.unc.lib.boxc.search.solr.facets.FilterableDisplayValueFacet;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import edu.unc.lib.boxc.search.solr.services.FacetFieldFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author bbpennel
 *
 */
public class FacetFieldUtil {

    private SearchSettings searchSettings;
    private SolrSettings solrSettings;
    @Autowired
    private FacetFieldFactory facetFieldFactory;

    /**
     * Add facet prefixes for facets present in the provided searchState
     * @param searchState
     * @param solrQuery
     */
    public void addFacetPrefixes(SearchState searchState, SolrQuery solrQuery) {
        searchState.getFacets().forEach((fieldName, facets) -> {
            if (facetIsOfType(facets, CutoffFacet.class)) {
                CutoffFacet facet = (CutoffFacet) facets.get(0);
                var field = SearchFieldKey.valueOf(facet.getFieldName());
                if (facet.getFacetCutoff() != null && field != null) {
                    solrQuery.setFacetPrefix(field.getSolrField(), facet.getFacetCutoff() + ",");
                }
            }
        });
    }

    /**
     * Apply facet restrictions to a solr query based on the type of facet provided
     * @param facetObject Facet to add to the query
     * @param applyCutoffs whether cutoff restrictions from CutoffFacets should be applied to the query
     * @param solrQuery
     */
    public void addToSolrQuery(SearchFacet facetObject, boolean applyCutoffs, SolrQuery solrQuery) {
        addToSolrQuery(Arrays.asList(facetObject), applyCutoffs, solrQuery);
    }

    /**
     * Apply facet restrictions to a solr query based on the type of facet provided
     *
     * @param facetObject list of facets to add to query
     * @param applyCutoffs whether cutoff restrictions from CutoffFacets should be applied to the query
     * @param solrQuery
     */
    public void addToSolrQuery(List<SearchFacet> facetObject, boolean applyCutoffs, SolrQuery solrQuery) {
        if (facetIsOfType(facetObject, CutoffFacet.class)) {
            if (applyCutoffs) {
                addFacetValue(facetObject, solrQuery, cutoffFacetApplyCutoffsToFq);
            } else {
                addFacetValue(facetObject, solrQuery, cutoffFacetToFq);
            }
        } else if (facetIsOfType(facetObject, FilterableDisplayValueFacet.class)) {
            addFacetValue(facetObject, solrQuery, filterableDisplayValueFacetToFq);
        } else if (facetIsOfType(facetObject, GenericFacet.class)) {
            addFacetValue(facetObject, solrQuery, genericFacetToFq);
        }
    }

    /**
     * @param facets
     * @param expected
     * @return return true if the facets in the provided list are of the expected type
     */
    public static boolean facetIsOfType(List<SearchFacet> facets, Class<?> expected) {
        if (facets.isEmpty()) {
            return false;
        }
        return expected.isInstance(facets.get(0));
    }

    private void addFacetValue(List<SearchFacet> facets, SolrQuery solrQuery,
            Function<SearchFacet, String> toFqFunct) {
        String fq = facets.stream().map(toFqFunct).collect(Collectors.joining(" OR "));
        solrQuery.addFilterQuery(fq);
    }

    private Function<SearchFacet, String> cutoffFacetToFq = (inFacet) -> {
        return generateCutoffFacetQuery(inFacet, false);
    };

    private Function<SearchFacet, String> cutoffFacetApplyCutoffsToFq = (inFacet) -> {
        return generateCutoffFacetQuery(inFacet, true);
    };

    private String generateCutoffFacetQuery(SearchFacet inFacet, boolean applyCutoffs) {
        CutoffFacet facet = (CutoffFacet) inFacet;
        List<HierarchicalFacetNode> facetNodes = facet.getFacetNodes();
        CutoffFacetNode endNode = (CutoffFacetNode) facetNodes.get(facetNodes.size() - 1);
        String solrFieldName = SearchFieldKey.valueOf(facet.getFieldName()).getSolrField();

        StringBuilder filterQuery = new StringBuilder("(");
        filterQuery.append(solrFieldName).append(":").append(endNode.getTier()).append(",");
        if (!endNode.getSearchKey().equals("*")) {
            filterQuery.append(SolrSettings.sanitize(endNode.getSearchKey()));
        } else {
            filterQuery.append('*');
        }

        if (applyCutoffs && facet.getCutoff() != null) {
            filterQuery.append(" AND !").append(solrFieldName).append(':').append(facet.getCutoff())
                    .append(',').append('*');
        }
        return filterQuery.append(')').toString();
    }

    private Function<SearchFacet, String> filterableDisplayValueFacetToFq = (facet) -> {
        String solrFieldName = SearchFieldKey.valueOf(facet.getFieldName()).getSolrField();
        return solrFieldName + ":(*|" + SolrSettings.sanitize(facet.getSearchValue()) + ")";
    };

    private Function<SearchFacet, String> genericFacetToFq = (facet) -> {
        String solrFieldName = SearchFieldKey.valueOf(facet.getFieldName()).getSolrField();
        return solrFieldName + ":\"" + SolrSettings.sanitize(facet.getSearchValue()) + "\"";
    };

    /**
     * Default pivoting values used for restricting facet list results.
     *
     * @param fieldKey
     * @param solrQuery
     */
    public void addDefaultFacetPivot(String fieldKey, SolrQuery solrQuery) {
        Class<?> facetClass = SearchSettings.getFacetClass(fieldKey);
        this.addDefaultFacetPivot(fieldKey, facetClass, solrQuery);
    }

    public void addDefaultFacetPivot(GenericFacet facet, SolrQuery solrQuery) {
        this.addDefaultFacetPivot(facet.getFieldName(), facet.getClass(), solrQuery);
    }

    public void addDefaultFacetPivot(String fieldKey, Class<?> facetClass, SolrQuery solrQuery) {
        String solrFieldName = SearchFieldKey.valueOf(fieldKey).getSolrField();
        if (CutoffFacet.class.equals(facetClass)) {
            solrQuery.add("f." + solrFieldName + ".facet.prefix", "1,");
        }
    }

    public void setFacetLimit(String fieldKey, Integer facetLimit, SearchState searchState) {
        // Create a new facet object for the facet being limited
        SearchFacet facet = facetFieldFactory.createFacet(fieldKey, null);
        searchState.getFacetLimits().put(facet.getFieldName(), facetLimit);
    }

    public void setSearchSettings(SearchSettings searchSettings) {
        this.searchSettings = searchSettings;
    }

    public void setSolrSettings(SolrSettings solrSettings) {
        this.solrSettings = solrSettings;
    }
}
