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
package edu.unc.lib.dl.search.solr.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;

/**
 * Facet used for case insensitive facet searching. Requires two fields, a display field and a search field. The display
 * field is the "default" facet for the field.
 *
 * @author bbpennel
 *
 */
public class CaseInsensitiveFacet extends GenericFacet {

    private static final String SEARCH_FIELD_SUFFIX = "_LC";
    // Name of the facet field that supples the
    private String searchName;

    /**
     *
     * @param fieldName Field name of the display field
     * @param countObject Solr facet count object, the value coming from the display field for this facet
     */
    public CaseInsensitiveFacet(String fieldName, FacetField.Count countObject) {
        super(fieldName, countObject);
        this.setFieldName(fieldName);

        if (countObject != null && countObject.getName() != null) {
            this.value = countObject.getName().toLowerCase();
        }
    }

    public CaseInsensitiveFacet(String fieldName, String facetValue) {
        super(fieldName, facetValue);
        this.setFieldName(fieldName);
        if (facetValue != null) {
            this.value = facetValue.toLowerCase();
        }
    }

    public CaseInsensitiveFacet(CaseInsensitiveFacet facet) {
        super(facet);
        this.searchName = facet.searchName;
        if (this.value != null) {
            this.value = this.value.toLowerCase();
        }
    }

    @Override
    public void setFieldName(String fieldName) {
        int index = fieldName.indexOf(SEARCH_FIELD_SUFFIX);
        if (index != -1) {
            this.searchName = fieldName;
            this.fieldName = fieldName.substring(0, index);
        } else {
            this.fieldName = fieldName;
            this.searchName = fieldName + SEARCH_FIELD_SUFFIX;
        }
    }

    public String getSearchName() {
        return searchName;
    }

    public void setSearchName(String searchName) {
        this.searchName = searchName;
    }

    public static void deduplicateCaseInsensitiveValues(FacetFieldObject facetFieldObject) {
        Map<String, GenericFacet> rollupMap = new LinkedHashMap<>(facetFieldObject.getValues().size());
        for (GenericFacet genericFacet: facetFieldObject.getValues()) {
            CaseInsensitiveFacet deptFacet = (CaseInsensitiveFacet) genericFacet;
            GenericFacet existingFacet = rollupMap.get(deptFacet.getSearchValue());
            if (existingFacet == null) {
                rollupMap.put(deptFacet.getSearchValue(), deptFacet);
            } else {
                existingFacet.setCount(existingFacet.getCount() + deptFacet.getCount());
            }
        }
        if (rollupMap.size() < facetFieldObject.getValues().size()) {
            facetFieldObject.setValues(rollupMap.values());
        }
    }

    @Override
    public Object clone() {
        return new CaseInsensitiveFacet(this);
    }
}
