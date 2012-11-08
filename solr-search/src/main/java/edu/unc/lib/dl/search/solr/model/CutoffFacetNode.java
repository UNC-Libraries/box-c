package edu.unc.lib.dl.search.solr.model;

import edu.unc.lib.dl.search.solr.exception.InvalidHierarchicalFacetException;

public class CutoffFacetNode implements HierarchicalFacetNode {
	private String displayValue;
	private String searchValue;
	private String searchKey;
	private String facetValue;
	private Integer tier;

	public CutoffFacetNode(String facetValue) {
		this.facetValue = facetValue;
		String facetComponents[] = facetValue.split(",", 3);
		if (facetComponents.length > 0) {
			try {
				this.tier = new Integer(facetComponents[0]);
			} catch (Exception e) {
				throw new InvalidHierarchicalFacetException("Invalid tier value.");
			}
		}
		if (facetComponents.length > 1) {
			this.searchKey = facetComponents[1];
		} else {
			// If there isn't a search value
			throw new InvalidHierarchicalFacetException("No search value provided.");
		}
		if (facetComponents.length > 2) {
			this.displayValue = facetComponents[2];
		} else {
			this.displayValue = this.searchValue;
		}

		this.searchValue = this.tier + "," + this.searchKey;
	}

	public CutoffFacetNode(String displayValue, String searchKey, Integer tier) {
		this.displayValue = displayValue;
		this.searchKey = searchKey;
		this.tier = tier;
		this.searchValue = this.tier + "," + this.searchKey;
	}
	
	public CutoffFacetNode(CutoffFacetNode node) {
		this.facetValue = node.facetValue;
		this.displayValue = node.displayValue;
		this.searchKey = node.searchKey;
		this.searchValue = node.searchValue;
		this.tier = new Integer(node.tier);
	}

	@Override
	public String getDisplayValue() {
		return displayValue;
	}

	@Override
	public String getSearchKey() {
		return searchKey;
	}

	@Override
	public String getFacetValue() {
		if (facetValue == null && searchValue != null){
			facetValue = searchValue;
			if (displayValue != null)
				facetValue += "," + displayValue;
		}
		return facetValue;
	}

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
		return getSearchValue() + "," + (this.tier + 1); 
	}
	
	@Override
	public Object clone() {
		return new CutoffFacetNode(this);
	}
}
