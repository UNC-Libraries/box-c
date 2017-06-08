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
package edu.unc.lib.dl.search.solr.util;

import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.search.solr.model.CaseInsensitiveFacet;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.CutoffFacetNode;
import edu.unc.lib.dl.search.solr.model.FacetFieldFactory;
import edu.unc.lib.dl.search.solr.model.GenericFacet;
import edu.unc.lib.dl.search.solr.model.HierarchicalFacetNode;
import edu.unc.lib.dl.search.solr.model.MultivaluedHierarchicalFacet;
import edu.unc.lib.dl.search.solr.model.SearchState;

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
     * Apply facet restrictions to a solr query based on the type of facet provided
     *
     * @param facetObject
     * @param solrQuery
     */
    public void addToSolrQuery(Object facetObject, SolrQuery solrQuery) {
        if (facetObject instanceof CutoffFacet) {
            this.addCutoffFacetValue((CutoffFacet) facetObject, solrQuery);
        } else if (facetObject instanceof MultivaluedHierarchicalFacet) {
            this.addMultivaluedFacetValue((MultivaluedHierarchicalFacet) facetObject, solrQuery);
        } else if (facetObject instanceof CaseInsensitiveFacet) {
            this.addCaseInsensitiveFacetValue((CaseInsensitiveFacet) facetObject, solrQuery);
        } else if (facetObject instanceof GenericFacet) {
            this.addGenericFacetValue((GenericFacet) facetObject, solrQuery);
        }
    }

    private void addCutoffFacetValue(CutoffFacet facet, SolrQuery solrQuery) {
        List<HierarchicalFacetNode> facetNodes = facet.getFacetNodes();
        CutoffFacetNode endNode = (CutoffFacetNode) facetNodes.get(facetNodes.size() - 1);
        String solrFieldName = solrSettings.getFieldName(facet.getFieldName());

        StringBuilder filterQuery = new StringBuilder();
        filterQuery.append(solrFieldName).append(":").append(endNode.getTier()).append(",");
        if (!endNode.getSearchKey().equals("*")) {
            filterQuery.append(SolrSettings.sanitize(endNode.getSearchKey()));
        } else {
            filterQuery.append('*');
        }
        solrQuery.addFilterQuery(filterQuery.toString());

        if (facet.getCutoff() != null) {
            filterQuery = new StringBuilder();
            filterQuery.append('!').append(solrFieldName).append(':').append(facet.getCutoff()).append(',').append('*');
            solrQuery.addFilterQuery(filterQuery.toString());
        }

        if (facet.getFacetCutoff() != null) {
            solrQuery.setFacetPrefix(solrFieldName, facet.getFacetCutoff() + ",");
        }
    }

    private void addMultivaluedFacetValue(MultivaluedHierarchicalFacet facet, SolrQuery solrQuery) {
        StringBuilder filterQuery = new StringBuilder();
        String solrFieldName = solrSettings.getFieldName(facet.getFieldName());

        filterQuery.append(solrFieldName).append(":").append(
                SolrSettings.sanitize(facet.getSearchValue())).append(",*");
        solrQuery.addFilterQuery(filterQuery.toString());

        solrQuery.add("f." + solrFieldName + ".facet.prefix", facet.getPivotValue());
    }

    private void addGenericFacetValue(GenericFacet facet, SolrQuery solrQuery) {
        solrQuery.addFilterQuery(solrSettings.getFieldName(facet.getFieldName()) + ":\""
                + SolrSettings.sanitize(facet.getSearchValue()) + "\"");
    }

    private void addCaseInsensitiveFacetValue(CaseInsensitiveFacet facet, SolrQuery solrQuery) {
        solrQuery.addFilterQuery(solrSettings.getFieldName(facet.getSearchName()) + ":\""
                + SolrSettings.sanitize(facet.getSearchValue()) + "\"");
    }

    /**
     * Default pivoting values used for restricting facet list results.
     *
     * @param fieldKey
     * @param solrQuery
     */
    public void addDefaultFacetPivot(String fieldKey, SolrQuery solrQuery) {
        Class<?> facetClass = searchSettings.getFacetClasses().get(fieldKey);
        this.addDefaultFacetPivot(fieldKey, facetClass, solrQuery);
    }

    public void addDefaultFacetPivot(GenericFacet facet, SolrQuery solrQuery) {
        this.addDefaultFacetPivot(facet.getFieldName(), facet.getClass(), solrQuery);
    }

    public void addDefaultFacetPivot(String fieldKey, Class<?> facetClass, SolrQuery solrQuery) {
        String solrFieldName = solrSettings.getFieldName(fieldKey);
        if (CutoffFacet.class.equals(facetClass)) {
            solrQuery.add("f." + solrFieldName + ".facet.prefix", "1,");
        } else if (MultivaluedHierarchicalFacet.class.equals(facetClass)) {
            solrQuery.add("f." + solrFieldName + ".facet.prefix", "^");
        }
    }

    public void setFacetLimit(String fieldKey, Integer facetLimit, SearchState searchState) {
        // Create a new facet object for the facet being limited
        GenericFacet facet = facetFieldFactory.createFacet(fieldKey, null);
        searchState.getFacetLimits().put(facet.getFieldName(), facetLimit);
    }

    public void setSearchSettings(SearchSettings searchSettings) {
        this.searchSettings = searchSettings;
    }

    public void setSolrSettings(SolrSettings solrSettings) {
        this.solrSettings = solrSettings;
    }
}
