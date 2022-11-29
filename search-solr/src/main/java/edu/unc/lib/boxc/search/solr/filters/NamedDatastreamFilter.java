package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.filters.QueryFilter;

/**
 * Filter which restricts results to entries which contain datastreams of the specified type
 *
 * @author bbpennel
 */
public class NamedDatastreamFilter implements QueryFilter {
    private DatastreamType datastreamType;
    private SearchFieldKey fieldKey;

    protected NamedDatastreamFilter(SearchFieldKey fieldKey, DatastreamType datastreamType) {
        this.datastreamType = datastreamType;
        this.fieldKey = fieldKey;
    }

    @Override
    public String toFilterString() {
        return getFieldKey().getSolrField() + ":" + datastreamType.getId() + "|*";
    }

    public DatastreamType getDatastreamType() {
        return datastreamType;
    }

    @Override
    public SearchFieldKey getFieldKey() {
        return fieldKey;
    }
}
