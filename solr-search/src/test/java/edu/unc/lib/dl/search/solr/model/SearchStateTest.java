package edu.unc.lib.dl.search.solr.model;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class SearchStateTest extends Assert {

	@Test
	public void hierarchicalFacetCloning() {
		SearchState searchState = new SearchState();
		Map<String,Object> facets = new HashMap<String,Object>();
		facets.put("CONTENT_TYPE", new MultivaluedHierarchicalFacet("CONTENT_TYPE", "^text,Text"));
		searchState.setFacets(facets);
		
		SearchState searchStatePartDeux = new SearchState(searchState);
		Object facetObject = searchStatePartDeux.getFacets().get("CONTENT_TYPE");
		assertNotNull(facetObject);
		
		assertTrue(facetObject instanceof MultivaluedHierarchicalFacet);
		
		assertEquals(1, ((AbstractHierarchicalFacet)facetObject).getFacetNodes().size());
		
	}
}
