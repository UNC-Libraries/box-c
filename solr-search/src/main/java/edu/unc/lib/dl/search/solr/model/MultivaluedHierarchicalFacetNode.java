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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import edu.unc.lib.dl.search.solr.exception.InvalidHierarchicalFacetException;

/**
 * 
 * @author bbpennel
 *
 */
public class MultivaluedHierarchicalFacetNode implements HierarchicalFacetNode {
    public static Pattern extractFacetParts = Pattern.compile("[\\^/]");

    private String displayValue;
    private String searchKey;
    private String facetValue;
    private String searchValue;
    private String pivotValue;
    private String limitValue;
    private List<String> tiers;

    /**
     * Initializes from an array of tier values, using the first tierNumber values.
     *
     * @param tierValues
     * @param tierNumber
     */
    public MultivaluedHierarchicalFacetNode(String[] facetParts, int tierNumber) {
        if (facetParts.length == 0) {
            throw new InvalidHierarchicalFacetException("Invalid facet format, no values were provided");
        }
        StringBuilder facetBuilder = new StringBuilder();
        this.tiers = new ArrayList<String>();
        for (int i = "".equals(facetParts[0]) ? 1 : 0; i < facetParts.length && i <= tierNumber; i++) {
            this.populateNode(facetParts[i], i, tierNumber);
            if (i > 0) {
                facetBuilder.append('/');
            }
            facetBuilder.append(facetParts[i]);
        }
        this.facetValue = facetBuilder.toString();
    }

    public MultivaluedHierarchicalFacetNode(String facetValue) {
        this.facetValue = facetValue.replaceAll("\"", "");
        this.tiers = new ArrayList<String>();
        try {
            String[] facetParts = extractFacetParts.split(facetValue);
            if (facetParts.length == 0 ) {
                throw new InvalidHierarchicalFacetException("Incorrect facet format for value " + facetValue);
            }
            for (int i = "".equals(facetParts[0]) ? 1 : 0; i < facetParts.length; i++) {
                this.populateNode(facetParts[i], i, facetParts.length - 1);
            }
        } catch (NullPointerException e) {
            throw new InvalidHierarchicalFacetException("Facet value did not match expected format: " + facetValue, e);
        } catch (IndexOutOfBoundsException e) {
            throw new InvalidHierarchicalFacetException("Facet value did not match expected format: " + facetValue, e);
        }
    }

    private void populateNode(String facetPart, int index, int lastIndex) {
        if (index == lastIndex) {
            String[] facetPair = facetPart.split(",", 2);
            // Query values will not have the display value part of the pair
            if (facetPair.length == 2) {
                displayValue = facetPair[1];
            } else {
                displayValue = null;
            }
            searchKey = facetPair[0];
            tiers.add(facetPair[0]);
        } else {
            tiers.add(facetPart);
        }
    }

    public MultivaluedHierarchicalFacetNode(String displayValue, String searchKey,
            String facetValue, List<String> tiers) {
        this.displayValue = displayValue;
        this.searchKey = searchKey;
        this.facetValue = facetValue;
        this.tiers = tiers;
    }

    public MultivaluedHierarchicalFacetNode(MultivaluedHierarchicalFacetNode node) {
        this.displayValue = node.displayValue;
        this.searchKey = node.searchKey;
        this.facetValue = node.facetValue;
        this.searchValue = node.searchValue;
        this.pivotValue = node.pivotValue;
        this.limitValue = node.limitValue;
        this.tiers = new ArrayList<String>(node.tiers);
    }

    @Override
    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }

    @Override
    public String getSearchKey() {
        return searchKey;
    }

    @Override
    public String getFacetValue() {
        return facetValue;
    }

    public List<String> getTiers() {
        return tiers;
    }

    public String joinTiers(boolean designateLastNode) {
        StringBuilder joined = new StringBuilder();
        int i = 0;
        for (String tier : tiers) {
            if (designateLastNode && i++ == tiers.size() - 1) {
                joined.append('^');
            } else {
                joined.append('/');
            }
            joined.append(tier);
        }

        return joined.toString();
    }

    public void setSearchValue(String searchValue) {

    }

    /**
     * Returns index query syntax for an exact value, with leading and trailing node designators
     * format: /tier1/tier2^tier3
     */
    @Override
    public String getSearchValue() {
        if (searchValue == null) {
            this.searchValue = joinTiers(true);
        }
        return this.searchValue;
    }

    /**
     * Returns index query syntax for getting the next tier of values, with trailing last node designator
     *
     * Format: /tier1/tier2^
     */
    @Override
    public String getPivotValue() {
        if (pivotValue == null) {
            this.pivotValue = joinTiers(false) + "^";
        }
        return this.pivotValue;
    }

    /**
     * Returns expected search query syntax, with no first or last tier designation
     *
     * Formated: tier1/tier2/tier3
     */
    @Override
    public String getLimitToValue() {
        if (this.limitValue == null) {
            StringBuilder joined = new StringBuilder();
            for (String tier : tiers) {
                if (joined.length() > 0) {
                    joined.append('/');
                }
                joined.append(tier);
            }
            this.limitValue = joined.toString();
        }
        return this.limitValue;
    }

    @Override
    public Object clone() {
        return new MultivaluedHierarchicalFacetNode(this);
    }
}
