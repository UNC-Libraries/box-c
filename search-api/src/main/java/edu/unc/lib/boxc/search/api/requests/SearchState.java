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
package edu.unc.lib.boxc.search.api.requests;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.unc.lib.boxc.search.api.ranges.RangeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.SearchFacet;

/**
 * Object representing the state of a search query, containing all of the search related parameters for specifying
 * terms, facets, page/facet sizes, sorts, and filters.
 *
 * @author bbpennel
 */
public class SearchState implements Serializable, Cloneable {
    private static final Logger LOG = LoggerFactory.getLogger(SearchState.class);
    private static final long serialVersionUID = 1L;

    private Map<String, String> searchFields;
    private Map<String, RangeValue> rangeFields;
    private Map<String, List<SearchFacet>> facets;
    private Map<String, Integer> facetLimits;
    private Map<String, String> facetSorts;
    private List<String> facetsToRetrieve;
    private Collection<Permission> permissionLimits;
    private Integer baseFacetLimit;
    private Integer startRow;
    private Integer rowsPerPage;
    private String sortType;
    private boolean sortNormalOrder = true;
    private List<String> resourceTypes;
    private String searchTermOperator;
    private boolean ignoreMaxRows = false;
    private List<String> resultFields;
    private Boolean rollup;
    private String rollupField;
    private Boolean includeParts;

    public SearchState() {
        LOG.debug("Instantiating new SearchState");
        searchFields = new HashMap<String, String>();
        rangeFields = new HashMap<String, RangeValue>();
        permissionLimits = null;
        facets = new HashMap<String, List<SearchFacet>>();
        facetLimits = new HashMap<String, Integer>();
        facetSorts = new HashMap<String, String>();
        resultFields = null;
        facetsToRetrieve = null;
        rollup = null;
        baseFacetLimit = null;
        startRow = null;
        includeParts = true;
    }

    public SearchState(SearchState searchState) {
        if (searchState.getSearchFields() != null) {
            this.searchFields = new HashMap<>(searchState.getSearchFields());
        }
        if (searchState.getRangeFields() != null) {
            rangeFields = new HashMap<>();
            for (var item : searchState.getRangeFields().entrySet()) {
                rangeFields.put(item.getKey(), item.getValue().clone());
            }
        }
        if (searchState.getFacets() != null) {
            facets = new HashMap<>();
            for (Entry<String, List<SearchFacet>> item : searchState.getFacets().entrySet()) {
                facets.put(item.getKey(), item.getValue());
            }
        }
        if (searchState.getFacetLimits() != null) {
            this.facetLimits = new HashMap<String, Integer>(searchState.getFacetLimits());
        }
        if (searchState.getFacetSorts() != null) {
            this.facetSorts = new HashMap<String, String>(searchState.getFacetSorts());
        }
        if (searchState.getResourceTypes() != null) {
            this.resourceTypes = new ArrayList<String>(searchState.getResourceTypes());
        }
        if (searchState.getResultFields() != null) {
            this.resultFields = new ArrayList<String>(searchState.getResultFields());
        }
        if (searchState.getFacetsToRetrieve() != null) {
            this.facetsToRetrieve = new ArrayList<String>(searchState.getFacetsToRetrieve());
        }
        if (searchState.getPermissionLimits() != null) {
            permissionLimits = new ArrayList<Permission>(searchState.getPermissionLimits());
        }

        baseFacetLimit = searchState.getBaseFacetLimit();
        startRow = searchState.getStartRow();
        rowsPerPage = searchState.getRowsPerPage();
        sortType = searchState.getSortType();
        sortNormalOrder = searchState.getSortNormalOrder();
        ignoreMaxRows = searchState.getIgnoreMaxRows();
        searchTermOperator = searchState.getSearchTermOperator();
        rollup = searchState.getRollup();
        rollupField = searchState.rollupField;
        includeParts = searchState.includeParts;
    }

    public Map<String, String> getSearchFields() {
        return searchFields;
    }

    public void setSearchFields(Map<String, String> searchFields) {
        this.searchFields = searchFields;
    }

    /**
     * Retrieve the facets for this search state. The addFacet and setFacet methods should be preferred for
     * updating facet values.
     * @return
     */
    public Map<String, List<SearchFacet>> getFacets() {
        return facets;
    }

    /**
     * Add a facet value to this search state
     * @param value facet value
     */
    public void addFacet(SearchFacet value) {
        List<SearchFacet> existing = facets.get(value.getFieldName());
        if (existing == null) {
            existing = new ArrayList<SearchFacet>();
        }
        facets.put(value.getFieldName(), existing);
        existing.add(value);
    }

    /**
     * Set the value of the specified facet to the provided value
     * @param value value of the facet to set.
     */
    public void setFacet(SearchFacet value) {
        SearchFieldKey key = SearchFieldKey.valueOf(value.getFieldName());
        facets.put(key.name(), Arrays.asList(value));
    }

    /**
     * Set the value of the specified facet to the provided list of values
     * @param key key of the facet to add
     * @param values list of values to set.
     */
    public void setFacet(SearchFieldKey key, List<SearchFacet> values) {
        facets.put(key.name(), values);
    }

    public void setFacets(Map<String, List<SearchFacet>> facets) {
        this.facets = facets;
    }

    public Integer getStartRow() {
        return startRow;
    }

    public void setStartRow(Integer startRow) {
        if (startRow < 0) {
            this.startRow = 0;
            return;
        }
        this.startRow = startRow;
    }

    public Integer getRowsPerPage() {
        return rowsPerPage;
    }

    public void setRowsPerPage(Integer rowsPerPage) {
        if (rowsPerPage < 0) {
            this.rowsPerPage = 0;
            return;
        }
        this.rowsPerPage = rowsPerPage;
    }

    public String getSortType() {
        return sortType;
    }

    public void setSortType(String sortType) {
        this.sortType = sortType;
    }

    public boolean getSortNormalOrder() {
        return sortNormalOrder;
    }

    public void setSortNormalOrder(boolean sortNormalOrder) {
        this.sortNormalOrder = sortNormalOrder;
    }

    public boolean getIgnoreMaxRows() {
        return ignoreMaxRows;
    }

    public void setIgnoreMaxRows(boolean ignoreMaxRows) {
        this.ignoreMaxRows = ignoreMaxRows;
    }

    private static Pattern splitTermFragmentsRegex = Pattern.compile("(\"[^\"]*\"|[^\" ,]+)");
    /**
     * Retrieves all the search term fragments contained in the selected field. Fragments are either single words
     * separated by non-alphanumeric characters, or phrases encapsulated by quotes.
     *
     * @param fieldType
     *           field type of the search term string to retrieve fragments of.
     * @return An arraylist of strings containing all of the word fragments in the selected search term field.
     */
    public ArrayList<String> getSearchTermFragments(String fieldType) {
        if (this.searchFields == null || fieldType == null) {
            return null;
        }
        String value = this.searchFields.get(fieldType);
        if (value == null) {
            return null;
        }
        Matcher matcher = splitTermFragmentsRegex.matcher(value);
        ArrayList<String> fragments = new ArrayList<String>();
        while (matcher.find()) {
            if (matcher.groupCount() == 1) {
                fragments.add(matcher.group(1));
            }
        }
        return fragments;
    }

    public Map<String, RangeValue> getRangeFields() {
        return rangeFields;
    }

    public void setRangeFields(Map<String, RangeValue> rangeFields) {
        this.rangeFields = rangeFields;
    }

    public Collection<Permission> getPermissionLimits() {
        return permissionLimits;
    }

    public void setPermissionLimits(Collection<Permission> permissionLimits) {
        this.permissionLimits = permissionLimits;
    }

    public List<String> getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(List<String> resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    public void setResourceTypes(Collection<String> resourceCollection) {
        this.resourceTypes = new ArrayList<String>(resourceCollection);
    }

    public String getSearchTermOperator() {
        return searchTermOperator;
    }

    public void setSearchTermOperator(String searchTermOperator) {
        this.searchTermOperator = searchTermOperator;
    }

    public Map<String, Integer> getFacetLimits() {
        return facetLimits;
    }

    public void setFacetLimits(Map<String, Integer> facetLimits) {
        this.facetLimits = facetLimits;
    }

    public Integer getBaseFacetLimit() {
        return baseFacetLimit;
    }

    public void setBaseFacetLimit(Integer baseFacetLimit) {
        this.baseFacetLimit = baseFacetLimit;
    }

    public List<String> getResultFields() {
        return resultFields;
    }

    public void setResultFields(List<String> resultFields) {
        this.resultFields = resultFields;
    }

    public List<String> getFacetsToRetrieve() {
        return facetsToRetrieve;
    }

    public void setFacetsToRetrieve(List<String> facetsToRetrieve) {
        this.facetsToRetrieve = facetsToRetrieve;
    }

    public Map<String, String> getFacetSorts() {
        return facetSorts;
    }

    public void setFacetSorts(Map<String, String> facetSorts) {
        this.facetSorts = facetSorts;
    }

    public Boolean getRollup() {
        return rollup;
    }

    public void setRollup(Boolean rollup) {
        this.rollup = rollup;
    }

    public String getRollupField() {
        return rollupField;
    }

    public void setRollupField(String rollupField) {
        this.rollupField = rollupField;
    }

    public void setIncludeParts(boolean include) {
        this.includeParts = include;
    }

    public boolean getIncludeParts() {
        return this.includeParts;
    }

    /**
     * Returns if any search fields that would indicate search-like behavior have been populated
     *
     * @return
     */
    public boolean isPopulatedSearch() {
        return this.getRangeFields().size() > 0 || this.getSearchFields().size() > 0
                || (permissionLimits != null && permissionLimits.size() > 0);
    }

    @Override
    public Object clone() {
        return new SearchState(this);
    }

}
