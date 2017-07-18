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
package edu.unc.lib.dl.search.solr.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;

/**
 * Response object for a search request.  Contains the list of results from the selected
 * page, the list of hierarchical and nonhierarchical facets, and the count of the total
 * number of results the query found.
 * @author bbpennel
 */
public class SearchResultResponse {
    private final Logger LOG = LoggerFactory.getLogger(SearchResultResponse.class);

    private BriefObjectMetadata selectedContainer;
    private List<BriefObjectMetadata> resultList;
    private FacetFieldList facetFields;
    private long resultCount;
    private SearchState searchState;
    private SolrQuery generatedQuery;

    public SearchResultResponse() {
    }

    public List<BriefObjectMetadata> getResultList() {
        return resultList;
    }

    public void setResultList(List<BriefObjectMetadata> resultList) {
        this.resultList = resultList;
    }

    public FacetFieldList getFacetFields() {
        return facetFields;
    }

    public void setFacetFields(FacetFieldList facetFields) {
        this.facetFields = facetFields;
    }

    public long getResultCount() {
        return resultCount;
    }

    public void setResultCount(long resultCount) {
        this.resultCount = resultCount;
    }

    public SearchState getSearchState() {
        return searchState;
    }

    public void setSearchState(SearchState searchState) {
        this.searchState = searchState;
    }

    public SolrQuery getGeneratedQuery() {
        return generatedQuery;
    }

    public void setGeneratedQuery(SolrQuery generatedQuery) {
        this.generatedQuery = generatedQuery;
    }

    public BriefObjectMetadata getSelectedContainer() {
        return selectedContainer;
    }

    public void setSelectedContainer(BriefObjectMetadata selectedContainer) {
        this.selectedContainer = selectedContainer;
    }

    public void extractCrumbDisplayValueFromRepresentative(BriefObjectMetadata representative) {
        Object contentTypeValue = searchState.getFacets().get(SearchFieldKeys.CONTENT_TYPE.name());
        if (contentTypeValue instanceof MultivaluedHierarchicalFacet) {
            LOG.debug("Replacing content type search value "
                    + searchState.getFacets().get(SearchFieldKeys.CONTENT_TYPE.name()));
            MultivaluedHierarchicalFacet repFacet = null;
            // If we're dealing with a rolled up result then hunt through all its items to find the matching content
            // type
            if (representative instanceof GroupedMetadataBean) {
                GroupedMetadataBean groupRep = (GroupedMetadataBean) representative;

                int i = 0;
                do {
                    representative = groupRep.getItems().get(i);

                    if (representative.getContentTypeFacet() != null) {
                        repFacet = representative.getContentTypeFacet().get(0);
                        LOG.debug("Pulling content type from representative {}: {}",
                                representative.getId(), repFacet);
                        if (repFacet.contains(((MultivaluedHierarchicalFacet) contentTypeValue))) {
                            break;
                        } else {
                            repFacet = null;
                        }
                    }
                } while (++i < groupRep.getItems().size());
            } else {
                // If its not a rolled up result, take it easy
                repFacet = representative.getContentTypeFacet().get(0);
            }

            if (repFacet != null) {
                ((MultivaluedHierarchicalFacet) contentTypeValue).setDisplayValues(repFacet);
                searchState.getFacets().put(SearchFieldKeys.CONTENT_TYPE.name(), contentTypeValue);
            }
        }
    }

    public List<String> getIdList() {
        if (this.resultList == null) {
            return null;
        }
        List<String> ids = new ArrayList<String>();
        for (BriefObjectMetadata brief: this.resultList) {
            ids.add(brief.getId());
        }
        return ids;
    }
}
