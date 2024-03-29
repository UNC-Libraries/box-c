package edu.unc.lib.boxc.search.solr.facets;

import edu.unc.lib.boxc.search.api.exceptions.InvalidHierarchicalFacetException;
import edu.unc.lib.boxc.search.api.facets.CutoffFacetNode;
import edu.unc.lib.boxc.search.api.facets.HierarchicalFacetNode;

/**
 * Implementation of a node within a hierarchical facet which supports cut off operations
 * @author bbpennel
 *
 */
public class CutoffFacetNodeImpl implements HierarchicalFacetNode, CutoffFacetNode {
    private final String searchValue;
    private String searchKey;
    private String facetValue;
    private Integer tier;

    public CutoffFacetNodeImpl(String facetValue) {
        this.facetValue = facetValue;
        String[] facetComponents = facetValue.split(",", 3);
        if (facetComponents.length > 0) {
            try {
                this.tier = new Integer(facetComponents[0]);
            } catch (Exception e) {
                throw new InvalidHierarchicalFacetException("Invalid tier value "
                        + facetComponents[0] + " from facet string " + facetValue);
            }
        }
        if (facetComponents.length > 1) {
            this.searchKey = facetComponents[1];
        } else {
            // If there isn't a search value
            throw new InvalidHierarchicalFacetException("No search value provided.");
        }

        this.searchValue = this.tier + "," + this.searchKey;
    }

    public CutoffFacetNodeImpl(String searchKey, Integer tier) {
        this.searchKey = searchKey;
        this.tier = tier;
        this.searchValue = this.tier + "," + this.searchKey;
    }

    public CutoffFacetNodeImpl(CutoffFacetNodeImpl node) {
        this.facetValue = node.facetValue;
        this.searchKey = node.searchKey;
        this.searchValue = node.searchValue;
        this.tier = new Integer(node.tier);
    }

    @Override
    public String getDisplayValue() {
        return searchKey;
    }

    @Override
    public String getSearchKey() {
        return searchKey;
    }

    @Override
    public String getFacetValue() {
        if (facetValue == null && searchValue != null) {
            facetValue = searchValue;
        }
        return facetValue;
    }

    @Override
    public Integer getTier() {
        return tier;
    }

    @Override
    public String getSearchValue() {
        return searchValue;
    }

    @Override
    public String getPivotValue() {
        return (this.tier + 1) + ",";
    }

    @Override
    public String getLimitToValue() {
        return getSearchValue() + "!" + (this.tier + 1);
    }

    @Override
    public Object clone() {
        return new CutoffFacetNodeImpl(this);
    }
}
