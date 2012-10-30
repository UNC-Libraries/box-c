package edu.unc.lib.dl.search.solr.util;

import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;

import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.CutoffFacetNode;
import edu.unc.lib.dl.search.solr.model.GenericFacet;
import edu.unc.lib.dl.search.solr.model.HierarchicalFacetNode;
import edu.unc.lib.dl.search.solr.model.MultivaluedHierarchicalFacet;

public class FacetFieldUtil {

	private SearchSettings searchSettings;
	private SolrSettings solrSettings;

	/**
	 * Apply facet restrictions to a solr query based on the type of facet provided
	 * 
	 * @param facetObject
	 * @param solrQuery
	 */
	public void addToSolrQuery(Object facetObject, SolrQuery solrQuery) {
		if (facetObject instanceof CutoffFacet) {
			this.addCutoffFacetValue((CutoffFacet) facetObject, solrQuery);
		} else if (facetObject instanceof MultivaluedHierarchicalFacet) {
			this.addMultivaluedFacetValue((MultivaluedHierarchicalFacet) facetObject, solrQuery);
		} else if (facetObject instanceof GenericFacet) {
			this.addGenericFacetValue((GenericFacet) facetObject, solrQuery);
		}
	}

	private void addCutoffFacetValue(CutoffFacet facet, SolrQuery solrQuery) {
		List<HierarchicalFacetNode> facetNodes = facet.getFacetNodes();
		CutoffFacetNode endNode = (CutoffFacetNode) facetNodes.get(facetNodes.size() - 1);
		String solrFieldName = solrSettings.getFieldName(facet.getFieldName());

		StringBuilder filterQuery = new StringBuilder();
		filterQuery.append(solrFieldName).append(":").append(endNode.getTier()).append(",");
		if (!endNode.getSearchKey().equals("*")) {
			filterQuery.append(SolrSettings.sanitize(endNode.getSearchKey())).append(",");
		}
		filterQuery.append('*');
		solrQuery.addFilterQuery(filterQuery.toString());

		if (facet.getCutoff() != null) {
			filterQuery = new StringBuilder();
			filterQuery.append('!').append(solrFieldName).append(':').append(facet.getCutoff()).append(',').append('*');
			solrQuery.addFilterQuery(filterQuery.toString());

			solrQuery.setFacetPrefix(solrFieldName, facet.getPivotValue());
		}
	}

	private void addMultivaluedFacetValue(MultivaluedHierarchicalFacet facet, SolrQuery solrQuery) {
		StringBuilder filterQuery = new StringBuilder();
		String solrFieldName = solrSettings.getFieldName(facet.getFieldName());

		filterQuery.append(solrFieldName).append(":").append(SolrSettings.sanitize(facet.getSearchValue())).append(",*");
		solrQuery.addFilterQuery(filterQuery.toString());
		
		solrQuery.add("f." + solrFieldName + ".facet.prefix", facet.getPivotValue());
	}

	private void addGenericFacetValue(GenericFacet facet, SolrQuery solrQuery) {
		solrQuery.addFilterQuery(solrSettings.getFieldName(facet.getFieldName()) + ":\""
				+ SolrSettings.sanitize((String) facet.getSearchValue()) + "\"");
	}

	/**
	 * Default pivoting values used for restricting facet list results.
	 * 
	 * @param fieldKey
	 * @param solrQuery
	 */
	public void addDefaultFacetPivot(String fieldKey, SolrQuery solrQuery) {
		Class<?> facetClass = searchSettings.getFacetClasses().get(fieldKey);
		this.addDefaultFacetPivot(fieldKey, facetClass, solrQuery);
	}
	
	public void addDefaultFacetPivot(GenericFacet facet, SolrQuery solrQuery) {
		this.addDefaultFacetPivot(facet.getFieldName(), facet.getClass(), solrQuery);
	}
	
	public void addDefaultFacetPivot(String fieldKey, Class<?> facetClass, SolrQuery solrQuery) {
		String solrFieldName = solrSettings.getFieldName(fieldKey);
		if (CutoffFacet.class.equals(facetClass)) {
			solrQuery.add("f." + solrFieldName + ".facet.prefix", "1,");
		} else if (MultivaluedHierarchicalFacet.class.equals(facetClass)) {
			solrQuery.add("f." + solrFieldName + ".facet.prefix", "^");
		}
	}

	public void setSearchSettings(SearchSettings searchSettings) {
		this.searchSettings = searchSettings;
	}

	public void setSolrSettings(SolrSettings solrSettings) {
		this.solrSettings = solrSettings;
	}
}
