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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.Permission;

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
    private Map<String, RangePair> rangeFields;
    private Map<String, Object> facets;
    private Map<String, Integer> facetLimits;
    private Map<String, String> facetSorts;
    private Collection<String> facetsToRetrieve;
    private Collection<Permission> permissionLimits;
    private Integer baseFacetLimit;
    private Integer startRow;
    private Integer rowsPerPage;
    private String sortType;
    private boolean sortNormalOrder = true;
    private List<String> resourceTypes;
    private String searchTermOperator;
    private List<String> resultFields;
    private Boolean rollup;
    private String rollupField;
    private Boolean includeParts;

    public SearchState() {
        LOG.debug("Instantiating new SearchState");
        searchFields = new HashMap<String, String>();
        rangeFields = new HashMap<String, RangePair>();
        permissionLimits = null;
        facets = new HashMap<String, Object>();
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
            this.searchFields = new HashMap<String, String>(searchState.getSearchFields());
        }
        if (searchState.getRangeFields() != null) {
            rangeFields = new HashMap<String, RangePair>();
            for (Entry<String, RangePair> item : searchState.getRangeFields().entrySet()) {
                rangeFields.put(item.getKey(), new RangePair(item.getValue()));
            }
        }
        if (searchState.getFacets() != null) {
            facets = new HashMap<String, Object>();
            for (Entry<String, Object> item : searchState.getFacets().entrySet()) {
                if (item.getValue() instanceof edu.unc.lib.dl.search.solr.model.GenericFacet) {
                    facets.put(item.getKey(), ((GenericFacet)item.getValue()).clone());
                } else {
                    facets.put(item.getKey(), item.getValue());
                }
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

    public Map<String, Object> getFacets() {
        return facets;
    }

    public void setFacets(Map<String, Object> facets) {
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

    public Map<String, RangePair> getRangeFields() {
        return rangeFields;
    }

    public void setRangeFields(Map<String, RangePair> rangeFields) {
        this.rangeFields = rangeFields;
    }

    public static class RangePair {
        // A null in either side of the pair indicates no restriction
        private String leftHand;
        private String rightHand;

        public RangePair() {

        }

        public RangePair(String pairString) {
            String[] pairParts = pairString.split(",", 2);
            if (pairParts[0].length() > 0) {
                this.leftHand = pairParts[0];
            } else {
                this.leftHand = null;
            }
            if (pairParts[1].length() > 0) {
                this.rightHand = pairParts[1];
            } else {
                this.rightHand = null;
            }
        }

        public RangePair(String leftHand, String rightHand) {
            this.leftHand = leftHand;
            this.rightHand = rightHand;
        }

        public RangePair(RangePair rangePair) {
            this.leftHand = rangePair.getLeftHand();
            this.rightHand = rangePair.getRightHand();
        }

        public String getLeftHand() {
            return leftHand;
        }

        public void setLeftHand(String leftHand) {
            this.leftHand = leftHand;
        }

        public String getRightHand() {
            return rightHand;
        }

        public void setRightHand(String rightHand) {
            this.rightHand = rightHand;
        }

        @Override
        public String toString() {
            if (leftHand == null) {
                if (rightHand == null) {
                    return "";
                }
                return "," + rightHand;
            }
            if (rightHand == null) {
                return leftHand + ",";
            }
            return leftHand + "," + rightHand;
        }
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

    public Collection<String> getFacetsToRetrieve() {
        return facetsToRetrieve;
    }

    public void setFacetsToRetrieve(Collection<String> facetsToRetrieve) {
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
