package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.filters.QueryFilter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter which restricts results to entries which contain populated values for the given key with
 * the specified field search value
 *
 * @author lfarrell
 */
public class HasValuesFilter implements QueryFilter {
    private final SearchFieldKey fieldKey;
    private final List<String> fieldValues;

    protected HasValuesFilter(SearchFieldKey fieldKey, List<String> fieldValues) {
        this.fieldKey = fieldKey;
        this.fieldValues = fieldValues;
    }

    @Override
    public String toFilterString() {
        return fieldValues.stream().map(v -> getFieldKey().getSolrField() + ":" + v)
                .collect(Collectors.joining(" OR "));
    }

    @Override
    public SearchFieldKey getFieldKey() {
        return fieldKey;
    }
}
