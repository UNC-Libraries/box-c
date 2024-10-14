package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.filters.QueryFilter;

/**
 * Filter which restricts results to entries which contain populated values for the given key with
 * the specified field search value
 *
 * @author lfarrell
 */
public class HasValueFilter implements QueryFilter {
    private final SearchFieldKey fieldKey;
    private final String fieldValue;

    protected HasValueFilter(SearchFieldKey fieldKey, String fieldValue) {
        this.fieldKey = fieldKey;
        this.fieldValue = fieldValue;
    }

    @Override
    public String toFilterString() {
        return getFieldKey().getSolrField() + ":" + fieldValue;
    }

    @Override
    public SearchFieldKey getFieldKey() {
        return fieldKey;
    }

    public String getFieldValue() {
        return fieldValue;
    }
}
