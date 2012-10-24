package edu.unc.lib.dl.search.solr.model;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class MultivaluedHierarchicalFacetTest extends Assert {

	@Test
	public void createFacetList() {
		List<String> facetValues = Arrays.asList("|image,Image", "/image|jpg,jpg");
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
		
		assertEquals("/image|jpg", facets.get(0).getSearchValue());
	}
	
	@Test
	public void createFacetListMultiple() {
		List<String> facetValues = Arrays.asList("|image,Image", "/image|jpg,jpg", "|audio,Audio", "/audio|wav,wav");
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
		
		assertEquals("/image|jpg", facets.get(0).getSearchValue());
		
		firstTier = (MultivaluedHierarchicalFacetNode)facets.get(1).getFacetNodes().get(0);
		assertEquals("Audio", firstTier.getDisplayValue());
		assertEquals("audio", firstTier.getSearchKey());
		assertEquals(1, firstTier.getTiers().size());
		
		secondTier = (MultivaluedHierarchicalFacetNode)facets.get(1).getFacetNodes().get(1);
		assertEquals("wav", secondTier.getDisplayValue());
		assertEquals("wav", secondTier.getSearchKey());
		assertEquals(2, secondTier.getTiers().size());
		
		assertEquals("/audio|wav", facets.get(1).getSearchValue());
	}
	
	@Test
	public void createFacetListMultipleResort() {
		List<String> facetValues = Arrays.asList("/image|jpg,jpg", "|audio,Audio", "|image,Image", "/audio|wav,wav");
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
		
		assertEquals("/image|jpg", facets.get(0).getSearchValue());
		
		firstTier = (MultivaluedHierarchicalFacetNode)facets.get(1).getFacetNodes().get(0);
		assertEquals("Audio", firstTier.getDisplayValue());
		assertEquals("audio", firstTier.getSearchKey());
		assertEquals(1, firstTier.getTiers().size());
		
		secondTier = (MultivaluedHierarchicalFacetNode)facets.get(1).getFacetNodes().get(1);
		assertEquals("wav", secondTier.getDisplayValue());
		assertEquals("wav", secondTier.getSearchKey());
		assertEquals(2, secondTier.getTiers().size());
		
		assertEquals("/audio|wav", facets.get(1).getSearchValue());
	}
}
