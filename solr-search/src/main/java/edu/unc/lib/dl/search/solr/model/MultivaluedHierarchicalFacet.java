package edu.unc.lib.dl.search.solr.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.search.solr.exception.InvalidHierarchicalFacetException;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

public class MultivaluedHierarchicalFacet extends AbstractHierarchicalFacet {
	private static final Logger log = LoggerFactory.getLogger(MultivaluedHierarchicalFacet.class);

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

	@Override
	public String getSearchKey() {
		MultivaluedHierarchicalFacetNode lastNode = (MultivaluedHierarchicalFacetNode) this.facetNodes
				.get(this.facetNodes.size() - 1);
		
		return lastNode.getSearchKey();
	}
	
	@Override
	public String getSearchValue() {
		MultivaluedHierarchicalFacetNode lastNode = (MultivaluedHierarchicalFacetNode) this.facetNodes
				.get(this.facetNodes.size() - 1);
		
		StringBuilder searchKey = new StringBuilder();
		int i = 0;
		for (String tier: lastNode.getTiers()) {
			if (i++ == lastNode.getTiers().size() - 1) {
				searchKey.append('|');
			} else {
				searchKey.append('/');
			}
			searchKey.append(tier);
		}
		
		return searchKey.toString();
	}

	@Override
	public void addToSolrQuery(SolrQuery solrQuery) {
		StringBuilder filterQuery = new StringBuilder();
		
		filterQuery.append(this.fieldName).append(":").append(SolrSettings.sanitize(this.getSearchKey())).append(",*");
		solrQuery.addFilterQuery(filterQuery.toString());
	}

}
