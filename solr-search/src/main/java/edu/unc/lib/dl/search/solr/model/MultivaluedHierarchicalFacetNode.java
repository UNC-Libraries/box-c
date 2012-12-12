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
			if (facetParts.length <= 1)
				throw new InvalidHierarchicalFacetException("Incorrect facet format for value " + facetValue);
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
	
	public MultivaluedHierarchicalFacetNode(MultivaluedHierarchicalFacetNode node) {
		this.displayValue = node.displayValue;
		this.searchKey = node.searchKey;
		this.facetValue = node.facetValue;
		this.searchValue = node.searchValue;
		this.pivotValue = node.pivotValue;
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
	
	@Override
	public Object clone() {
		return new MultivaluedHierarchicalFacetNode(this);
	}
}
