package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.filters.QueryFilter;

import java.util.List;
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
        return new MultipleDirectlyOwnedDatastreamsFilter(fieldKey, datastreamTypes, false);
    }

    /**
     * Create a filter which restricts results to entries which contain at least one of the listed datastreams,
     * where the datastreams are owned by the object itself rather than one of its children
     * @param fieldKey key of field to filter
     * @param datastreamTypes datastreams to filter for
     * @return  new QueryFilter instance with the provided datastream types
     */
    public static QueryFilter createDirectlyOwnedFilter(SearchFieldKey fieldKey, Set<DatastreamType> datastreamTypes) {
        return new MultipleDirectlyOwnedDatastreamsFilter(fieldKey, datastreamTypes, true);
    }

    /**
     * @param fieldKey key of field to filter
     * @return new QueryFilter instance with the provided search key
     */
    public static QueryFilter createFilter(SearchFieldKey fieldKey) {
        return new HasPopulatedFieldFilter(fieldKey);
    }

    /**
     * @param fileTypes mimetypes
     * @return new QueryFilter instance with the provided file types
     */
    public static QueryFilter createIIIFv3ViewableFilter(List<String> fileTypes) {
        return new IIIFv3ViewableFilter(fileTypes);
    }

    /**
     *
     * @param fieldKey searchField
     * @param fieldValues list of values to search for
     * @return new QueryFilter instance with the provided file type
     */
    public static QueryFilter createHasValuesFilter(SearchFieldKey fieldKey, List<String> fieldValues) {
        return new HasValuesFilter(fieldKey, fieldValues);
    }
}
