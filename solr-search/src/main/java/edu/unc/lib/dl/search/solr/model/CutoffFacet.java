 package edu.unc.lib.dl.search.solr.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;

import edu.unc.lib.dl.search.solr.util.SolrSettings;

public class CutoffFacet extends AbstractHierarchicalFacet {
	private Integer cutoff;
	
	public CutoffFacet(String fieldName, String facetString) {
		super(fieldName, facetString);
		CutoffFacetNode node = new CutoffFacetNode(facetString);
		this.facetNodes.add(node);
	}
	
	public CutoffFacet(String fieldName, String facetString, long count) {
		super(fieldName, facetString, count);
		CutoffFacetNode node = new CutoffFacetNode(facetString);
		this.facetNodes.add(node);
	}
	
	public CutoffFacet(String fieldName, List<String> facetStrings, long count) {
		super(fieldName, null, count);
		for (String facetString: facetStrings) {
			CutoffFacetNode node = new CutoffFacetNode(facetString);
			this.facetNodes.add(node);
		}
		this.sortTiers();
	}
	
	public CutoffFacet(CutoffFacet facet) {
		super(facet.getFieldName(), facet.getValue(), facet.getCount());
		this.cutoff = facet.getCutoff();
		this.facetNodes = new ArrayList<HierarchicalFacetNode>(facet.getFacetNodes());
	}
	
	private void sortTiers(){
		// Hooray for bubble sorts
		for (int i = 0; i < this.facetNodes.size(); i++) {
			CutoffFacetNode node = (CutoffFacetNode)this.facetNodes.get(i);
			for (int j = i + 1; j < this.getFacetNodes().size(); j++) {
				CutoffFacetNode swap = (CutoffFacetNode)this.facetNodes.get(j);
				if (node.getTier() > swap.getTier()) {
					this.facetNodes.set(i, swap);
					this.facetNodes.set(j, node);
				}
			}
		}
	}
	
	public void addNode(HierarchicalFacetNode node) {
		facetNodes.add(node);
	}
	
	public void addNode(String searchValue, String displayValue){
		int highestTier = getHighestTier();
		CutoffFacetNode node = new CutoffFacetNode(displayValue, searchValue, highestTier + 1);
		this.facetNodes.add(node);
	}
	
	public HierarchicalFacetNode getNode(String searchValue) {
		for (HierarchicalFacetNode node: this.facetNodes) {
			if (((CutoffFacetNode)node).getSearchValue().equals(searchValue))
				return node;
		}
		return null;
	}
	
	public CutoffFacetNode getHighestTierNode() {
		if (this.facetNodes == null || this.facetNodes.size() == 0)
			return null;
		
		CutoffFacetNode lastNode = (CutoffFacetNode)this.facetNodes.get(this.facetNodes.size()-1);
		if ("*".equals(lastNode.getSearchKey())) {
			if (this.facetNodes.size() == 1)
				return null;
			return (CutoffFacetNode)this.facetNodes.get(this.facetNodes.size()-2); 
		}
		return lastNode;
	}
	
	public int getHighestTier(){
		CutoffFacetNode lastNode = this.getHighestTierNode();
		if (lastNode == null)
			return -1;
		return lastNode.getTier();
	}
	
	@Override
	public String getDisplayValue() {
		CutoffFacetNode lastNode = this.getHighestTierNode();
		if (lastNode == null)
			return null;
		return lastNode.getDisplayValue();
	}
	
	@Override
	public String getSearchKey() {
		CutoffFacetNode lastNode = this.getHighestTierNode();
		if (lastNode == null)
			return null;
		return lastNode.getSearchKey();
	}
	
	public String getSearchValue() {
		CutoffFacetNode lastNode = this.getHighestTierNode();
		if (lastNode == null)
			return null;
		return lastNode.getSearchValue();
	}

	@Override
	public void addToSolrQuery(SolrQuery solrQuery) {
		CutoffFacetNode endNode = (CutoffFacetNode)facetNodes.get(facetNodes.size() - 1);
		
		StringBuilder filterQuery = new StringBuilder();
		filterQuery.append(this.fieldName).append(":") 
				.append(endNode.getTier()).append(",");
		if (!endNode.getSearchKey().equals("*")){
			filterQuery.append(SolrSettings.sanitize(endNode.getSearchKey())).append(",");
		}
		filterQuery.append('*');
		solrQuery.addFilterQuery(filterQuery.toString());
		
		if (this.getCutoff() != null){
			filterQuery = new StringBuilder();
			filterQuery.append('!').append(this.fieldName).append(':') 
					.append(this.getCutoff()).append(',').append('*');
			solrQuery.addFilterQuery(filterQuery.toString());
		}
	}
	
	public Integer getCutoff() {
		return cutoff;
	}

	public void setCutoff(Integer cutoff) {
		this.cutoff = cutoff;
	}
}
