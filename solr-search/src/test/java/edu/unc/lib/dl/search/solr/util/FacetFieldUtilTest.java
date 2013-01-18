package edu.unc.lib.dl.search.solr.util;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import static org.mockito.Mockito.*;

public class FacetFieldUtilTest extends Assert {

	@Test
	public void addFacetCutoffToQuery() {
		SolrQuery query = new SolrQuery();
		
		CutoffFacet facet = new CutoffFacet("ANCESTOR_PATH", "2,uuid:test!3");
		
		SolrSettings solrSettings = mock(SolrSettings.class);
		when(solrSettings.getFieldName(anyString())).thenReturn("ancestorPath");
		
		FacetFieldUtil facetFieldUtil = new FacetFieldUtil();
		facetFieldUtil.setSolrSettings(solrSettings);
		
		facetFieldUtil.addToSolrQuery(facet, query);
		
		String[] filterQueries = query.getFilterQueries();
		assertEquals(2, filterQueries.length);
		
		assertEquals("ancestorPath:2,uuid\\:test,*", filterQueries[0]);
		assertEquals("!ancestorPath:3,*", filterQueries[1]);
		
//		for (String filterQuery: query.getFilterQueries()){
//			query.removeFilterQuery(filterQuery);
//		}
//		
//		System.out.println(query);
	}
}
