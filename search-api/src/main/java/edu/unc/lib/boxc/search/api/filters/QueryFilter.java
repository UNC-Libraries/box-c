package edu.unc.lib.boxc.search.api.filters;

import edu.unc.lib.boxc.search.api.SearchFieldKey;

/**
 * A filter used to adjust the results of a query
 *
 * @author bbpennel
 */
public interface QueryFilter {
    /**
     * @return this filter represented as a query string
     */
    public String toFilterString();

    /**
     * @return SearchFieldKey for the field to apply the filter to
     */
    public SearchFieldKey getFieldKey();
}
