package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.filters.QueryFilter;

import java.util.Set;

/**
 * Factory for creating query filter objects
 *
 * @author bbpennel
 */
public class QueryFilterFactory {
    private QueryFilterFactory() {
    }

    /**
     * @param fieldKey key of field to filter
     * @param datastreamType
     * @return new QueryFilter instance with the provided type
     */
    public static QueryFilter createFilter(SearchFieldKey fieldKey, DatastreamType datastreamType) {
        return new NamedDatastreamFilter(fieldKey, datastreamType);
    }

    /**
     * @param fieldKey key of field to filter
     * @param datastreamTypes
     * @return new QueryFilter instance with the provided datastream types
     */
    public static QueryFilter createFilter(SearchFieldKey fieldKey, Set<DatastreamType> datastreamTypes) {
        return new MultipleDirectlyOwnedDatastreamsFilter(fieldKey, datastreamTypes);
    }
}
