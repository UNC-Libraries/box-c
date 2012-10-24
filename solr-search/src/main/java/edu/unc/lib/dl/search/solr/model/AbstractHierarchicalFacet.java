package edu.unc.lib.dl.search.solr.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;

public abstract class AbstractHierarchicalFacet extends GenericFacet {
	protected List<HierarchicalFacetNode> facetNodes;
	
	protected AbstractHierarchicalFacet() {
		facetNodes = new ArrayList<HierarchicalFacetNode>();
		this.count = 0;
	}
	
	protected AbstractHierarchicalFacet(String fieldName) {
		this();
		this.fieldName = fieldName;
	}
	
	protected AbstractHierarchicalFacet(String fieldName, String facetValue) {
		this();
		this.fieldName = fieldName;
		this.value = facetValue;
	}
	
	protected AbstractHierarchicalFacet(String fieldName, String facetValue, long count) {
		this();
		this.fieldName = fieldName;
		this.value = facetValue;
		this.count = count;
	}
	
	public void addNode(HierarchicalFacetNode node) {
		facetNodes.add(node);
	}
	
	public List<HierarchicalFacetNode> getFacetNodes() {
		return facetNodes;
	}

	public void setFacetNodes(List<HierarchicalFacetNode> facetNodes) {
		this.facetNodes = facetNodes;
	}

	public abstract String getSearchKey();
	public abstract void addToSolrQuery(SolrQuery solrQuery);
}
