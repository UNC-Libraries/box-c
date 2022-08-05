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

/**
 * Filter which restricts results to entries which contain datastreams of the specified type
 *
 * @author bbpennel
 */
public class NamedDatastreamFilter implements QueryFilter {
    private DatastreamType datastreamType;
    private SearchFieldKey fieldKey;

    public NamedDatastreamFilter(SearchFieldKey fieldKey, DatastreamType datastreamType) {
        this.datastreamType = datastreamType;
        this.fieldKey = fieldKey;
    }

    @Override
    public String toFilterString() {
        return getFieldKey().getSolrField() + ":" + datastreamType.getId() + "|*";
    }

    @Override
    public SearchFieldKey getFieldKey() {
        return fieldKey;
    }
}
