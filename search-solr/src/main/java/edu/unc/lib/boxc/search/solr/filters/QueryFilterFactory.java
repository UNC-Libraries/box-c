/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        return new MultipleSelfOwnedDatastreamsFilter(fieldKey, datastreamTypes);
    }
}
