package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.filters.QueryFilter;

import java.util.List;

/**
 * Filter which restricts results to entries which contain populated values for the given key with
 * the specified field search value
 *
 * @author lfarrell
 */
public class HasValuesFilter implements QueryFilter {
    private final SearchFieldKey fieldKey;
    private final List<String> fieldValue;

    protected HasValuesFilter(SearchFieldKey fieldKey, List<String> fieldValue) {
        this.fieldKey = fieldKey;
        this.fieldValue = fieldValue;
    }

    @Override
    public String toFilterString() {
        StringBuilder filter = new StringBuilder(getFieldKey().getSolrField() + ":" + fieldValue.get(0));

        if (fieldValue.size() == 1) {
            return filter.toString();
        }

        /* Start looping from the second value in the field, since we've already got the first value */
        for (String value : fieldValue.subList(1, fieldValue.size())) {
            filter.append(" OR ").append(getFieldKey().getSolrField()).append(":").append(value);
        }

        return filter.toString();
    }

    @Override
    public SearchFieldKey getFieldKey() {
        return fieldKey;
    }
}
