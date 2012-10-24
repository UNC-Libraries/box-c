package edu.unc.lib.dl.search.solr.model;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class CutoffFacetTest extends Assert {

	@Test
	public void parseTest() {
		List<String> facetValues = Arrays.asList("1,uuid:a,A", "2,uuid:b,B", "3,uuid:c,C");
		
		CutoffFacet facet = new CutoffFacet(null, facetValues, 0);
		assertEquals(3,facet.getHighestTier());
		assertEquals("C",facet.getDisplayValue());
		assertEquals("3,uuid:c",facet.getSearchValue());
		assertEquals("uuid:c",facet.getSearchKey());
		
		assertEquals(3, facet.getFacetNodes().size());
	}
}
