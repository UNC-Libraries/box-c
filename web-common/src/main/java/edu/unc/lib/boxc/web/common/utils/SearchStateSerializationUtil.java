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
package edu.unc.lib.boxc.web.common.utils;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.HierarchicalFacet;
import edu.unc.lib.boxc.search.api.requests.SearchState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for serializing SearchState objects
 * @author bbpennel
 */
public class SearchStateSerializationUtil {
    public static final String FACET_VALUE_KEY = "value";
    public static final String FACET_DISPLAY_VALUE_KEY = "displayValue";

    private SearchStateSerializationUtil() {
    }

    /**
     * Generate a map containing all of the search filters present
     * @param searchState
     * @return
     */
    public static Map<String, Object> getFilterParameters(SearchState searchState) {
        var params = new HashMap<String, Object>();
        if (searchState.getSearchFields() != null) {
            searchState.getSearchFields().forEach((name, value) -> {
                var field = SearchFieldKey.valueOf(name);
                params.put(field.getUrlParam(), value);
            });
        }
        if (searchState.getRangeFields() != null) {
            searchState.getRangeFields().forEach((name, value) -> {
                var field = SearchFieldKey.valueOf(name);
                // Need to call toString to ensure that RangePairs are formatted correctly
                params.put(field.getUrlParam(), value.toString());
            });
        }
        if (searchState.getFacets() != null) {
            searchState.getFacets().forEach((name, values) -> {
                if (values.isEmpty()) {
                    return;
                }
                var field = SearchFieldKey.valueOf(name);
                var resultValues = new ArrayList<Map<String, String>>();
                values.forEach(value -> {
                    var entry = new HashMap<String, String>();
                    entry.put(FACET_VALUE_KEY, value.getValue());
                    // Set display value, falling back to the search value if there isn't one
                    var displayValue = value.getDisplayValue();
                    if (displayValue == null) {
                        if (value instanceof HierarchicalFacet) {
                            displayValue = ((HierarchicalFacet) value).getSearchKey();
                        } else {
                            displayValue = value.getSearchValue();
                        }
                    }
                    entry.put(FACET_DISPLAY_VALUE_KEY, displayValue);
                    resultValues.add(entry);
                });
                params.put(field.getUrlParam(), resultValues);
            });
        }
        return params;
    }
}
