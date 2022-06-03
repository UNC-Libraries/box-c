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
package edu.unc.lib.boxc.search.solr.services;

import edu.unc.lib.boxc.search.api.exceptions.SolrRuntimeException;
import edu.unc.lib.boxc.search.api.facets.FacetFieldObject;
import edu.unc.lib.boxc.search.api.requests.FacetValuesRequest;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.params.FacetParams;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.util.Assert.notNull;

/**
 * Service for navigating and returning values of facets
 * @author bbpennel
 */
public class FacetValuesService extends AbstractFacetListService {
    private final static Set<String> SORT_VALUES = new HashSet<>(Arrays.asList(
            FacetParams.FACET_SORT_COUNT, FacetParams.FACET_SORT_INDEX));

    private FacetFieldFactory facetFieldFactory;

    /**
     * List a page of values from the specified facet
     * @param request details of request to retrieve facet values
     * @return FacetFieldObject contains the facet values
     */
    public FacetFieldObject listValues(FacetValuesRequest request) {
        notNull(request.getFacetFieldKey(), "Must provide a facetFieldKey");
        var facetSolrField = request.getFacetFieldKey().getSolrField();

        var searchState = request.getBaseSearchRequest().getSearchState();
        assignResourceTypes(searchState);
        addSelectedContainer(request.getBaseSearchRequest());
        request.getBaseSearchRequest().setApplyCutoffs(false);
        // Remove any filters from the same facet being retrieved, so that all values from the facet are visible
        searchState.getFacets().remove(request.getFacetFieldKey().name());
        // Produce base query from provided searchRequest and its state
        SolrQuery query = searchService.generateSearch(request.getBaseSearchRequest());
        query.setRows(0);
        query.addFacetField(facetSolrField);
        query.setFacetMinCount(1);

        if (request.getStart() != null && request.getStart() >= 0) {
            query.set(FacetParams.FACET_OFFSET, request.getStart().toString());
        }
        if (request.getSort() != null && SORT_VALUES.contains(request.getSort())) {
            query.setFacetSort(request.getSort());
        }
        if (request.getRows() != null && request.getRows() > 0) {
            query.setFacetLimit(request.getRows());
        }

        try {
            var resp = searchService.executeQuery(query);
            return facetFieldFactory.createFacetFieldObject(request.getFacetFieldKey(),
                    resp.getFacetField(facetSolrField));
        } catch (SolrServerException e) {
            throw new SolrRuntimeException(e);
        }
    }

    /**
     * Assert that the provided sort value is valid. Throws IllegalArgumentException if not.
     * @param sort
     */
    public static void assertValidFacetSortValue(String sort) {
        if (sort != null && !SORT_VALUES.contains(sort)) {
            throw new IllegalArgumentException("Invalid facet sort type, values are: "
                    + String.join(", ", SORT_VALUES));
        }
    }

    public void setSearchService(SolrSearchService searchService) {
        this.searchService = searchService;
    }

    public void setFacetFieldFactory(FacetFieldFactory facetFieldFactory) {
        this.facetFieldFactory = facetFieldFactory;
    }
}
