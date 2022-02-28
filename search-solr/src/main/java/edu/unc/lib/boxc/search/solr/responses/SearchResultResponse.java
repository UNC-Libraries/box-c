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
package edu.unc.lib.boxc.search.solr.responses;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.FacetFieldList;
import edu.unc.lib.boxc.search.api.facets.SearchFacet;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.GroupedContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.facets.MultivaluedHierarchicalFacet;

/**
 * Response object for a search request.  Contains the list of results from the selected
 * page, the list of hierarchical and nonhierarchical facets, and the count of the total
 * number of results the query found.
 * @author bbpennel
 */
public class SearchResultResponse {
    private final Logger LOG = LoggerFactory.getLogger(SearchResultResponse.class);

    private ContentObjectRecord selectedContainer;
    private List<ContentObjectRecord> resultList;
    private FacetFieldList facetFields;
    private String minimumSearchYear;
    private long resultCount;
    private SearchState searchState;
    private SolrQuery generatedQuery;

    public SearchResultResponse() {
    }

    public List<ContentObjectRecord> getResultList() {
        return resultList;
    }

    public void setResultList(List<ContentObjectRecord> resultList) {
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

    public String getMinimumSearchYear() {
        return minimumSearchYear;
    }

    public void setMinimumSearchYear(String year) {
        this.minimumSearchYear = year;
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

    public ContentObjectRecord getSelectedContainer() {
        return selectedContainer;
    }

    public void setSelectedContainer(ContentObjectRecord selectedContainer) {
        this.selectedContainer = selectedContainer;
    }

    public void extractCrumbDisplayValueFromRepresentative(ContentObjectRecord representative) {
        List<SearchFacet> contentTypeValue = searchState.getFacets().get(SearchFieldKey.CONTENT_TYPE.name());
        if (contentTypeValue instanceof MultivaluedHierarchicalFacet) {
            LOG.debug("Replacing content type search value "
                    + searchState.getFacets().get(SearchFieldKey.CONTENT_TYPE.name()));
            MultivaluedHierarchicalFacet repFacet = null;
            // If we're dealing with a rolled up result then hunt through all its items to find the matching content
            // type
            if (representative instanceof GroupedContentObjectRecord) {
                GroupedContentObjectRecord groupRep = (GroupedContentObjectRecord) representative;

                int i = 0;
                do {
                    representative = groupRep.getItems().get(i);

                    if (representative.getContentTypeFacet() != null) {
                        repFacet = (MultivaluedHierarchicalFacet) representative.getContentTypeFacet().get(0);
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
                repFacet = (MultivaluedHierarchicalFacet) representative.getContentTypeFacet().get(0);
            }

            if (repFacet != null) {
                ((MultivaluedHierarchicalFacet) contentTypeValue).setDisplayValues(repFacet);
                searchState.setFacet(SearchFieldKey.CONTENT_TYPE, contentTypeValue);
            }
        }
    }

    public List<String> getIdList() {
        if (this.resultList == null) {
            return null;
        }
        List<String> ids = new ArrayList<String>();
        for (ContentObjectRecord brief: this.resultList) {
            ids.add(brief.getId());
        }
        return ids;
    }
}
