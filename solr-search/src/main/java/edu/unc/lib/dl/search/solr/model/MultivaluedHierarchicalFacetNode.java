package edu.unc.lib.dl.search.solr.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import edu.unc.lib.dl.search.solr.exception.InvalidHierarchicalFacetException;

public class MultivaluedHierarchicalFacetNode implements HierarchicalFacetNode {
	private static Pattern extractFacetParts = Pattern.compile("[\\^/]");

	private String displayValue;
	private String searchKey;
	private String facetValue;
	private String searchValue;
	private String pivotValue;
	private List<String> tiers;

	public MultivaluedHierarchicalFacetNode(String facetValue) {
		this.facetValue = facetValue.replaceAll("\"", "");
		this.tiers = new ArrayList<String>();
		try {
			String[] facetParts = extractFacetParts.split(facetValue);
			for (int i = 1; i < facetParts.length; i++) {
				if (i == facetParts.length - 1) {
					String[] facetPair = facetParts[i].split(",", 2);
					// Query values will not have the display value part of the pair
					if (facetPair.length == 2)
						displayValue = facetPair[1];
					else displayValue = null;
					searchKey = facetPair[0];
					tiers.add(facetPair[0]);
				} else {
					tiers.add(facetParts[i]);
				}
			}
		} catch (NullPointerException e) {
			throw new InvalidHierarchicalFacetException("Facet value did not match expected format: " + facetValue, e);
		} catch (IndexOutOfBoundsException e) {
			throw new InvalidHierarchicalFacetException("Facet value did not match expected format: " + facetValue, e);
		}
	}

	public MultivaluedHierarchicalFacetNode(String displayValue, String searchKey, String facetValue, List<String> tiers) {
		this.displayValue = displayValue;
		this.searchKey = searchKey;
		this.facetValue = facetValue;
		this.tiers = tiers;
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
		return facetValue;
	}

	public List<String> getTiers() {
		return tiers;
	}
	
	private String joinTiers(boolean designateLastNode) {
		StringBuilder joined = new StringBuilder();
		int i = 0;
		for (String tier: tiers) {
			if (designateLastNode && i++ == tiers.size() - 1) {
				joined.append('^');
			} else {
				joined.append('/');
			}
			joined.append(tier);
		}
		
		return joined.toString();
	}
	
	@Override
	public String getSearchValue() {
		if (searchValue == null)
			this.searchValue = joinTiers(true);
		return this.searchValue;
	}
	
	@Override
	public String getPivotValue() {
		if (pivotValue == null)
			this.pivotValue = joinTiers(false) + "^";
		return this.pivotValue;
	}
	
	@Override
	public String getLimitToValue() { 
		return getSearchValue();
	}
}
