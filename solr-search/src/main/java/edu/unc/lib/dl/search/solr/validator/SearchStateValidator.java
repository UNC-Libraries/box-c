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
package edu.unc.lib.dl.search.solr.validator;

import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchSettings;

/**
 * Validator for SearchState objects
 * @author bbpennel
 */
public class SearchStateValidator {
    @Autowired
    private SearchSettings searchSettings;


    public SearchStateValidator() {
    }

    public void validate(SearchState searchState) {
        //Validate search Fields and types
        Map<String,String> searchFields = searchState.getSearchFields();
        if (searchFields != null) {
            Iterator<String> searchTypeIt = searchFields.keySet().iterator();
            while (searchTypeIt.hasNext()) {
                //Validate the field name, verify that search fields users are looking for actually exist
                String searchType = searchTypeIt.next();
                if (!searchSettings.searchableFields.contains(searchType)) {
                    searchTypeIt.remove();
                }
            }
        }

        //Validate range fields
        Map<String,SearchState.RangePair> rangeFields = searchState.getRangeFields();
        if (rangeFields != null) {
            Iterator<Map.Entry<String, SearchState.RangePair>> rangeFieldIt = rangeFields.entrySet().iterator();
            while (rangeFieldIt.hasNext()) {
                Map.Entry<String, SearchState.RangePair> rangeField = rangeFieldIt.next();
                //If invalid search fields are specified, discard them
                if (searchSettings.rangeSearchableFields.contains(rangeField.getKey())) {
                    if (searchSettings.dateSearchableFields.contains(rangeField.getKey())) {
                        if (rangeField.getValue().getLeftHand() != null) {
                            rangeField.getValue().setLeftHand(rangeField.getValue().getLeftHand()
                                    .replace('/', '-').replaceAll("[^0-9\\-]+",""));
                        }
                        if (rangeField.getValue().getRightHand() != null) {
                            rangeField.getValue().setRightHand(rangeField.getValue().getRightHand()
                                    .replace('/', '-').replaceAll("[^0-9\\-]+",""));
                        }
                    }
                } else {
                    rangeFieldIt.remove();
                }
            }
        }

        //Validate facet fields

        Map<String,Object> facets = searchState.getFacets();
        if (facets != null) {
            Iterator<String> facetIt = facets.keySet().iterator();
            while (facetIt.hasNext()) {
                String facetField = facetIt.next();
                if (!searchSettings.facetNames.contains(facetField)) {
                    facetIt.remove();
                }
            }
        }

        //Validate start row number
        if (searchState.getStartRow() < 0) {
            searchState.setStartRow(0);
        }

        //Validate rows per page
        if (searchState.getRowsPerPage() > searchSettings.maxPerPage) {
            searchState.setRowsPerPage(searchSettings.maxPerPage);
        } else if (searchState.getRowsPerPage() <= 0) {
            searchState.setRowsPerPage(0);
        }

        if (searchState.getResourceTypes() != null) {
            Iterator<String> resourceTypesIt = searchState.getResourceTypes().iterator();
            while (resourceTypesIt.hasNext()) {
                if (!searchSettings.resourceTypes.contains(resourceTypesIt.next())) {
                    resourceTypesIt.remove();
                }
            }
        }

        //Validate sort type
        if (searchState.getSortType() == null || searchState.getSortType().length() == 0 ||
                !searchSettings.sortTypes.containsKey(searchState.getSortType())) {
            //Sort type was invalid, so overwrite it and order with defaults
            searchState.setSortType("default");
            searchState.setSortNormalOrder(true);
        }
    }

    public SearchSettings getSearchSettings() {
        return searchSettings;
    }

    public void setSearchSettings(SearchSettings searchSettings) {
        this.searchSettings = searchSettings;
    }
}
