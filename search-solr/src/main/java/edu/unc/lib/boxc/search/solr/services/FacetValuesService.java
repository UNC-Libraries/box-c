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

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.exceptions.SolrRuntimeException;
import edu.unc.lib.boxc.search.api.facets.FacetFieldObject;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
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
public class FacetValuesService {
    private final static Set<String> SORT_VALUES = new HashSet<>(Arrays.asList(
            FacetParams.FACET_SORT_COUNT, FacetParams.FACET_SORT_INDEX));

    private SolrSearchService searchService;
    private FacetFieldFactory facetFieldFactory;

    /**
     * List a page of values from the specified facet
     * @param facetFieldKey facet to retrieve values for
     * @param sort sort order for the results, defaults to count.
     * @param start offset into the result set at which to start for paging
     * @param rows number of values to return in the page
     * @param searchRequest base request around which the facet values will be scoped
     * @return FacetFieldObject contains the facet values
     */
    public FacetFieldObject listValues(SearchFieldKey facetFieldKey, String sort, Integer start, Integer rows,
                                       SearchRequest searchRequest) {
        notNull(facetFieldKey, "Must provide a facetFieldKey");
        var facetSolrField = facetFieldKey.getSolrField();

        // Produce base query from provided searchRequest and its state
        var query = searchService.generateSearch(searchRequest);
        query.setRows(0);
        query.addFacetField(facetSolrField);
        query.setFacetMinCount(1);
        if (start != null && start >= 0) {
            query.set(FacetParams.FACET_OFFSET, start.toString());
        }
        if (sort != null && SORT_VALUES.contains(sort)) {
            query.setFacetSort(sort);
        }
        if (rows != null && rows > 0) {
            query.setFacetLimit(rows);
        }

        try {
            var resp = searchService.executeQuery(query);
            return facetFieldFactory.createFacetFieldObject(facetSolrField, resp.getFacetField(facetSolrField));
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
