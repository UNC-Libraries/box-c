package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.filters.QueryFilter;

/**
 * Filter which restricts results to entries which contain populated values for the given search key,
 *
 * @author lfarrell
 */
public class HasPopulatedFieldFilter implements QueryFilter {
    private final SearchFieldKey fieldKey;

    protected HasPopulatedFieldFilter(SearchFieldKey fieldKey) {
        this.fieldKey = fieldKey;
    }

    @Override
    public String toFilterString() {
        return getFieldKey().getSolrField() + ":*";
    }

    @Override
    public SearchFieldKey getFieldKey() {
        return fieldKey;
    }
}
