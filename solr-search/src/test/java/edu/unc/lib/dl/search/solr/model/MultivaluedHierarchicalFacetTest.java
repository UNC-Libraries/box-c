package edu.unc.lib.dl.search.solr.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.search.solr.util.SearchSettings;

public class MultivaluedHierarchicalFacetTest extends Assert {

	@Test
	public void createFacetList() {
		List<String> facetValues = Arrays.asList("^image,Image", "/image^jpg,jpg");
		List<MultivaluedHierarchicalFacet> facets = MultivaluedHierarchicalFacet
				.createMultivaluedHierarchicalFacets(null, facetValues);
		
		assertEquals(1, facets.size());
		
		MultivaluedHierarchicalFacetNode firstTier = (MultivaluedHierarchicalFacetNode)facets.get(0).getFacetNodes().get(0);
		assertEquals("Image", firstTier.getDisplayValue());
		assertEquals("image", firstTier.getSearchKey());
		assertEquals(1, firstTier.getTiers().size());
		
		MultivaluedHierarchicalFacetNode secondTier = (MultivaluedHierarchicalFacetNode)facets.get(0).getFacetNodes().get(1);
		assertEquals("jpg", secondTier.getDisplayValue());
		assertEquals("jpg", secondTier.getSearchKey());
		assertEquals(2, secondTier.getTiers().size());
		
		assertEquals("/image^jpg", facets.get(0).getSearchValue());
	}
	
	@Test
	public void createFacetListMultiple() {
		List<String> facetValues = Arrays.asList("^image,Image", "/image^jpg,jpg", "^audio,Audio", "/audio^wav,wav");
		List<MultivaluedHierarchicalFacet> facets = MultivaluedHierarchicalFacet
				.createMultivaluedHierarchicalFacets(null, facetValues);
		
		assertEquals(2, facets.size());
		
		MultivaluedHierarchicalFacetNode firstTier = (MultivaluedHierarchicalFacetNode)facets.get(0).getFacetNodes().get(0);
		assertEquals("Image", firstTier.getDisplayValue());
		assertEquals("image", firstTier.getSearchKey());
		assertEquals(1, firstTier.getTiers().size());
		
		MultivaluedHierarchicalFacetNode secondTier = (MultivaluedHierarchicalFacetNode)facets.get(0).getFacetNodes().get(1);
		assertEquals("jpg", secondTier.getDisplayValue());
		assertEquals("jpg", secondTier.getSearchKey());
		assertEquals(2, secondTier.getTiers().size());
		
		assertEquals("/image^jpg", facets.get(0).getSearchValue());
		
		firstTier = (MultivaluedHierarchicalFacetNode)facets.get(1).getFacetNodes().get(0);
		assertEquals("Audio", firstTier.getDisplayValue());
		assertEquals("audio", firstTier.getSearchKey());
		assertEquals(1, firstTier.getTiers().size());
		
		secondTier = (MultivaluedHierarchicalFacetNode)facets.get(1).getFacetNodes().get(1);
		assertEquals("wav", secondTier.getDisplayValue());
		assertEquals("wav", secondTier.getSearchKey());
		assertEquals(2, secondTier.getTiers().size());
		
		assertEquals("/audio^wav", facets.get(1).getSearchValue());
	}
	
	@Test
	public void createFacetListMultipleResort() {
		List<String> facetValues = Arrays.asList("/image^jpg,jpg", "^audio,Audio", "^image,Image", "/audio^wav,wav");
		List<MultivaluedHierarchicalFacet> facets = MultivaluedHierarchicalFacet
				.createMultivaluedHierarchicalFacets(null, facetValues);
		
		assertEquals(2, facets.size());
		
		MultivaluedHierarchicalFacetNode firstTier = (MultivaluedHierarchicalFacetNode)facets.get(0).getFacetNodes().get(0);
		assertEquals("Image", firstTier.getDisplayValue());
		assertEquals("image", firstTier.getSearchKey());
		assertEquals(1, firstTier.getTiers().size());
		
		MultivaluedHierarchicalFacetNode secondTier = (MultivaluedHierarchicalFacetNode)facets.get(0).getFacetNodes().get(1);
		assertEquals("jpg", secondTier.getDisplayValue());
		assertEquals("jpg", secondTier.getSearchKey());
		assertEquals(2, secondTier.getTiers().size());
		
		assertEquals("/image^jpg", facets.get(0).getSearchValue());
		
		firstTier = (MultivaluedHierarchicalFacetNode)facets.get(1).getFacetNodes().get(0);
		assertEquals("Audio", firstTier.getDisplayValue());
		assertEquals("audio", firstTier.getSearchKey());
		assertEquals(1, firstTier.getTiers().size());
		
		secondTier = (MultivaluedHierarchicalFacetNode)facets.get(1).getFacetNodes().get(1);
		assertEquals("wav", secondTier.getDisplayValue());
		assertEquals("wav", secondTier.getSearchKey());
		assertEquals(2, secondTier.getTiers().size());
		
		assertEquals("/audio^wav", facets.get(1).getSearchValue());
	}
	
	@Test
	public void createInstance() {
		FacetField.Count count = mock(FacetField.Count.class);
		when(count.getCount()).thenReturn(1L);
		when(count.getName()).thenReturn("^text");
		List<FacetField.Count> countList = Arrays.asList(count);
		
		FacetField facetField = mock(FacetField.class);
		when(facetField.getValues()).thenReturn(countList);
		
		FacetFieldFactory facetFieldFactory = new FacetFieldFactory();
		SearchSettings searchSettings = mock(SearchSettings.class);
		Map<String,Class<?>> facetClasses = new HashMap<String,Class<?>>();
		facetClasses.put("CONTENT_TYPE", MultivaluedHierarchicalFacet.class);
		when(searchSettings.getFacetClasses()).thenReturn(facetClasses);
		facetFieldFactory.setSearchSettings(searchSettings);
		
		FacetFieldObject ffo = facetFieldFactory.createFacetFieldObject("CONTENT_TYPE", facetField);
		assertTrue(ffo.getValues().get(0) instanceof MultivaluedHierarchicalFacet);
		assertEquals("^text", ffo.getValues().get(0).getSearchValue());
		assertEquals("^text", ffo.getValues().get(0).getValue());
		assertEquals(1, ffo.getValues().get(0).getCount());
		assertNull(ffo.getValues().get(0).getDisplayValue());
	}
	
	@Test
	public void setDisplayValuesMergeInMissingNode() {
		List<String> facetValues = Arrays.asList("/image^jpg,jpg", "^image,Image");
		List<MultivaluedHierarchicalFacet> facetsIncoming = MultivaluedHierarchicalFacet
				.createMultivaluedHierarchicalFacets(null, facetValues);
		
		facetValues = Arrays.asList("/image^jpg,jpg");
		List<MultivaluedHierarchicalFacet> facetsBase = MultivaluedHierarchicalFacet
				.createMultivaluedHierarchicalFacets(null, facetValues);
		
		MultivaluedHierarchicalFacet facetBase = facetsBase.get(0);
		assertEquals(1, facetBase.getFacetNodes().size());
		
		facetBase.setDisplayValues(facetsIncoming.get(0));
		
		assertEquals(2, facetBase.getFacetNodes().size());
		
		assertEquals("Image", facetBase.getFacetNodes().get(0).getDisplayValue());
		assertEquals("jpg", facetBase.getFacetNodes().get(1).getDisplayValue());
		
		assertTrue(facetBase.getFacetNodes().get(0) != facetsIncoming.get(0).getFacetNodes().get(0));
		assertTrue(facetBase.getFacetNodes().get(1) != facetsIncoming.get(0).getFacetNodes().get(1));
	}
	
	@Test
	public void setDisplayValuesFewerIncomingNodes() {
		List<String> facetValues = Arrays.asList("^image,Image");
		List<MultivaluedHierarchicalFacet> facetsIncoming = MultivaluedHierarchicalFacet
				.createMultivaluedHierarchicalFacets(null, facetValues);
		
		facetValues = Arrays.asList("^image", "/image^jpg");
		List<MultivaluedHierarchicalFacet> facetsBase = MultivaluedHierarchicalFacet
				.createMultivaluedHierarchicalFacets(null, facetValues);
		
		MultivaluedHierarchicalFacet facetBase = facetsBase.get(0);
		assertEquals(2, facetBase.getFacetNodes().size());
		
		facetBase.setDisplayValues(facetsIncoming.get(0));
		
		assertEquals(2, facetBase.getFacetNodes().size());
		
		assertEquals("Image", facetBase.getFacetNodes().get(0).getDisplayValue());
		assertNull(facetBase.getFacetNodes().get(1).getDisplayValue());
		
		assertTrue(facetBase.getFacetNodes().get(0) != facetsIncoming.get(0).getFacetNodes().get(0));
	}
}
