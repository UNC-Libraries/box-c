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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.search.solr.util.SearchSettings;
import static org.mockito.Mockito.*;

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
	
	@Test
	public void getNode() {
		List<String> facetValues = Arrays.asList("1,uuid:a,A", "2,uuid:b,B", "3,uuid:c,C");
		CutoffFacet facet = new CutoffFacet(null, facetValues, 0);
		
		assertEquals("A", facet.getNode("uuid:a").getDisplayValue());
		assertEquals("B", facet.getNode("uuid:b").getDisplayValue());
		assertEquals("C", facet.getNode("uuid:c").getDisplayValue());
		
		assertNull(facet.getNode("uuid:d"));
	}
	
	@Test
	public void createInstance() {
		FacetField.Count count = mock(FacetField.Count.class);
		when(count.getCount()).thenReturn(1L);
		when(count.getName()).thenReturn("1,uuid:a,A");
		List<FacetField.Count> countList = Arrays.asList(count);
		
		FacetField facetField = mock(FacetField.class);
		when(facetField.getValues()).thenReturn(countList);
		
		FacetFieldFactory facetFieldFactory = new FacetFieldFactory();
		SearchSettings searchSettings = mock(SearchSettings.class);
		Map<String,Class<?>> facetClasses = new HashMap<String,Class<?>>();
		facetClasses.put("ANCESTOR_PATH", CutoffFacet.class);
		when(searchSettings.getFacetClasses()).thenReturn(facetClasses);
		facetFieldFactory.setSearchSettings(searchSettings);
		
		FacetFieldObject ffo = facetFieldFactory.createFacetFieldObject("ANCESTOR_PATH", facetField);
		assertTrue(ffo.getValues().get(0) instanceof CutoffFacet);
		assertEquals("1,uuid:a", ffo.getValues().get(0).getSearchValue());
		assertEquals("1,uuid:a,A", ffo.getValues().get(0).getValue());
		assertEquals(1, ffo.getValues().get(0).getCount());
		assertEquals("A", ffo.getValues().get(0).getDisplayValue());
	}
}
