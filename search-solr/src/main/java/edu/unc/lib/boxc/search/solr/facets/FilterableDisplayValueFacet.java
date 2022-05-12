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
package edu.unc.lib.boxc.search.solr.facets;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import org.apache.solr.client.solrj.response.FacetField;

/**
 * Facet which contains distinct display and search values, where the display value is used for
 * filtering and sorting operations.
 *
 * @author bbpennel
 */
public class FilterableDisplayValueFacet extends GenericFacet {

    public FilterableDisplayValueFacet() {
    }

    public FilterableDisplayValueFacet(SearchFieldKey fieldKey, String facetString) {
        this(fieldKey.name(), 0, facetString);
    }

    public FilterableDisplayValueFacet(String fieldName, FacetField.Count countObject) {
        this(fieldName, countObject.getCount(), countObject.getName());
    }

    public FilterableDisplayValueFacet(String fieldName, String facetString) {
        this(fieldName, 0, facetString);
    }

    private FilterableDisplayValueFacet(String fieldName, long count, String facetString) {
        this.count = count;
        this.fieldName = fieldName;
        var parts = facetString.split("\\|", 2);
        // Only one value provided, so treat it as the search value
        if (parts.length == 1) {
            this.value = parts[0];
            this.displayValue = "*";
        } else {
            this.value = parts[1];
            this.displayValue = parts[0];
        }
    }

    /**
     * Construct complete facet value form used for this facet type, used for storing in solr.
     * @param displayValue
     * @param searchValue
     * @return
     */
    public static String buildValue(String displayValue, String searchValue) {
        if (displayValue == null) {
            return "*|" + searchValue.trim();
        }
        return displayValue.replaceAll("\\|", "").trim() + "|" + searchValue.trim();
    }
}
