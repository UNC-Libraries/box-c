package edu.unc.lib.dl.search.solr.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.search.solr.exception.InvalidHierarchicalFacetException;

public class MultivaluedHierarchicalFacet extends AbstractHierarchicalFacet {
	private static final Logger log = LoggerFactory.getLogger(MultivaluedHierarchicalFacet.class);

	public MultivaluedHierarchicalFacet(String fieldName, String facetString) {
		super(fieldName, facetString);
		MultivaluedHierarchicalFacetNode node = new MultivaluedHierarchicalFacetNode(facetString);
		this.facetNodes.add(node);
	}
	
	public MultivaluedHierarchicalFacet(String fieldName, FacetField.Count countObject) {
		super(fieldName, countObject);
		MultivaluedHierarchicalFacetNode node = new MultivaluedHierarchicalFacetNode(this.value);
		this.facetNodes.add(node);
	}
	
	public static List<MultivaluedHierarchicalFacet> createMultivaluedHierarchicalFacets(String fieldName,
			List<String> facetValues) {
		Map<String, MultivaluedHierarchicalFacet> facetMap = new LinkedHashMap<String, MultivaluedHierarchicalFacet>();
		for (String facetValue : facetValues) {
			try {
				MultivaluedHierarchicalFacetNode node = new MultivaluedHierarchicalFacetNode(facetValue);
				String firstTier = node.getTiers().get(0);

				MultivaluedHierarchicalFacet matchingFacet = facetMap.get(firstTier);
				if (matchingFacet == null) {
					matchingFacet = new MultivaluedHierarchicalFacet(fieldName);
					facetMap.put(firstTier, matchingFacet);
				}
				matchingFacet.addNode(node);
			} catch (InvalidHierarchicalFacetException e) {
				log.warn("Invalid hierarchical facet", e);
			}
		}
		for (MultivaluedHierarchicalFacet facet : facetMap.values()) {
			facet.sortTiers();
		}

		return new ArrayList<MultivaluedHierarchicalFacet>(facetMap.values());
	}

	public MultivaluedHierarchicalFacet(String fieldName) {
		super(fieldName);
	}
	
	public MultivaluedHierarchicalFacet(MultivaluedHierarchicalFacet facet) {
		super((GenericFacet)facet);
		this.displayValue = facet.getDisplayValue();
		
		for (HierarchicalFacetNode node: facet.getFacetNodes()) {
			MultivaluedHierarchicalFacetNode newNode = new MultivaluedHierarchicalFacetNode(node.getFacetValue());
			this.facetNodes.add(newNode);
		}
	}

	public void sortTiers() {
		for (int i = 0; i < this.facetNodes.size(); i++) {
			MultivaluedHierarchicalFacetNode node = (MultivaluedHierarchicalFacetNode) this.facetNodes.get(i);
			for (int j = i + 1; j < this.getFacetNodes().size(); j++) {
				MultivaluedHierarchicalFacetNode swap = (MultivaluedHierarchicalFacetNode) this.facetNodes.get(j);
				if (node.getTiers().size() > swap.getTiers().size()) {
					this.facetNodes.set(i, swap);
					this.facetNodes.set(j, node);
				}
			}
		}
	}
	
	private MultivaluedHierarchicalFacetNode getLastNode() {
		return (MultivaluedHierarchicalFacetNode) this.facetNodes
				.get(this.facetNodes.size() - 1);
	}
	
	public HierarchicalFacetNode getNodeBySearchValue(String searchValue) {
		for (HierarchicalFacetNode node: this.facetNodes) {
			if (node.getSearchValue().equals(searchValue))
				return node;
		}
		return null;
	}
	
	public void setDisplayValues(MultivaluedHierarchicalFacet facet) {
		int startingCount = this.facetNodes.size();
		String targetJoined = ((MultivaluedHierarchicalFacetNode)this.getLastNode()).joinTiers(false);
		for (HierarchicalFacetNode node: facet.getFacetNodes()) {
			MultivaluedHierarchicalFacetNode targetNode = (MultivaluedHierarchicalFacetNode)this.getNodeBySearchValue(node.getSearchValue());
			if (targetNode != null) {
				log.debug("Adding in display value " + node.getDisplayValue());
				((MultivaluedHierarchicalFacetNode)targetNode).setDisplayValue(node.getDisplayValue());
			} else {
				String joined = ((MultivaluedHierarchicalFacetNode)node).joinTiers(false);
				if (targetJoined.indexOf(joined) == 0) {
					log.debug("Adding in missing node " + node.getSearchValue() + "," + node.getDisplayValue());
					this.facetNodes.add((HierarchicalFacetNode)node.clone());
				}
			}
		}
		// If the number of nodes has changed, then resort
		if (this.facetNodes.size() != startingCount) {
			this.sortTiers();
		}
	}

	@Override
	public String getSearchKey() {
		return getLastNode().getSearchKey();
	}
	
	@Override
	public String getSearchValue() {
		return getLastNode().getSearchValue();
	}
	
	@Override
	public String getDisplayValue() {
		return getLastNode().getDisplayValue();
	}
	
	@Override
	public String getPivotValue() {
		return getLastNode().getPivotValue();
	}
	
	@Override
	public String getLimitToValue() {
		return getLastNode().getLimitToValue();
	}
	
	@Override
	public Object clone() {
		return new MultivaluedHierarchicalFacet(this);
	}
}
