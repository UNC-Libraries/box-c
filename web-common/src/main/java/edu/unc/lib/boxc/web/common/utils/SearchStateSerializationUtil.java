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
                params.put(field.getUrlParam(), value.getParameterValue());
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
