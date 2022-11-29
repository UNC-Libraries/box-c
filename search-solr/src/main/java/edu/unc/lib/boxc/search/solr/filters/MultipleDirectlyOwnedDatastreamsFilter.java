package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.filters.QueryFilter;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filter which restricts results to entries which contain at least one of the listed datastreams,
 * where the datastream is owned by the object itself rather than one of its children
 *
 * @author bbpennel
 */
public class MultipleDirectlyOwnedDatastreamsFilter implements QueryFilter {
    private Set<DatastreamType> datastreamTypes;
    private SearchFieldKey fieldKey;

    protected MultipleDirectlyOwnedDatastreamsFilter(SearchFieldKey fieldKey, Set<DatastreamType> datastreamTypes) {
        this.datastreamTypes = datastreamTypes;
        this.fieldKey = fieldKey;
    }

    @Override
    public String toFilterString() {
        var dsField = fieldKey.getSolrField();
        return getDatastreamTypes().stream()
                // Filtering datastreams to exclude those owned by other objects
                .map(ds -> dsField + ":" + ds.getId() + "|*||")
                .collect(Collectors.joining(" OR ", "(", ")"));
    }

    public Set<DatastreamType> getDatastreamTypes() {
        return datastreamTypes;
    }

    @Override
    public SearchFieldKey getFieldKey() {
        return fieldKey;
    }
}
