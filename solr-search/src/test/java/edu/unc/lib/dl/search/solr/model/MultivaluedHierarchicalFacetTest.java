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

    @Test
    public void individualConstructorTest() {
        MultivaluedHierarchicalFacet facet = new MultivaluedHierarchicalFacet("", "/image^jpg");

        assertEquals(2, facet.getFacetNodes().size());
    }

    @Test
    public void containsTest() {
        List<MultivaluedHierarchicalFacet> facets = MultivaluedHierarchicalFacet
                .createMultivaluedHierarchicalFacets(null, Arrays.asList("^image,Image", "/image^jpg,jpg"));
        MultivaluedHierarchicalFacet facet1 = facets.get(0);

        facets = MultivaluedHierarchicalFacet
                .createMultivaluedHierarchicalFacets(null, Arrays.asList("^image"));
        MultivaluedHierarchicalFacet facet2 = facets.get(0);

        assertTrue(facet1.contains(facet2));
    }

    @Test
    public void containsMultipleTierTest() {
        List<MultivaluedHierarchicalFacet> facets = MultivaluedHierarchicalFacet
                .createMultivaluedHierarchicalFacets(null, Arrays.asList("^image,Image", "/image^jpg,jpg"));
        MultivaluedHierarchicalFacet facet1 = facets.get(0);

        facets = MultivaluedHierarchicalFacet
                .createMultivaluedHierarchicalFacets(null, Arrays.asList("^image", "/image^jpg"));
        MultivaluedHierarchicalFacet facet2 = facets.get(0);

        assertTrue(facet1.contains(facet2));
    }

    @Test
    public void containsMultipleTierTest2() {
        List<MultivaluedHierarchicalFacet> facets = MultivaluedHierarchicalFacet
                .createMultivaluedHierarchicalFacets(null, Arrays.asList("^text,Text", "/text^pdf,pdf"));
        MultivaluedHierarchicalFacet facet1 = facets.get(0);

        //This constructor probably isn't working since it only creates one node
        MultivaluedHierarchicalFacet facet2 = new MultivaluedHierarchicalFacet(null, "/text^pdf");
        assertEquals(2, facet2.getFacetNodes().size());

        assertTrue(facet1.contains(facet2));
    }

    @Test
    public void containsNotMatchTest() {
        List<MultivaluedHierarchicalFacet> facets = MultivaluedHierarchicalFacet
                .createMultivaluedHierarchicalFacets(null, Arrays.asList("^image,Image", "/image^jpg,jpg"));
        MultivaluedHierarchicalFacet facet1 = facets.get(0);

        facets = MultivaluedHierarchicalFacet
                .createMultivaluedHierarchicalFacets(null, Arrays.asList("^text", "/text^pdf"));
        MultivaluedHierarchicalFacet facet2 = facets.get(0);

        assertFalse(facet1.contains(facet2));

        facets = MultivaluedHierarchicalFacet
                .createMultivaluedHierarchicalFacets(null, Arrays.asList("^text"));
        facet2 = facets.get(0);

        assertFalse(facet1.contains(facet2));
    }

    @Test
    public void parseSearchSyntax() {
        MultivaluedHierarchicalFacet facet = new MultivaluedHierarchicalFacet("format", "image");

        MultivaluedHierarchicalFacetNode firstTier = (MultivaluedHierarchicalFacetNode) facet.getFacetNodes().get(0);
        assertEquals("image", firstTier.getSearchKey());
        assertEquals("image", facet.getLimitToValue());
        assertEquals("^image", facet.getSearchValue());
        assertEquals(1, firstTier.getTiers().size());

        facet = new MultivaluedHierarchicalFacet("format", "image/jpg");

        firstTier = (MultivaluedHierarchicalFacetNode) facet.getFacetNodes().get(0);
        assertEquals("image", firstTier.getSearchKey());
        MultivaluedHierarchicalFacetNode secondTier = (MultivaluedHierarchicalFacetNode) facet.getFacetNodes().get(1);
        assertEquals("jpg", secondTier.getSearchKey());
        assertEquals("image/jpg", facet.getLimitToValue());
        assertEquals("/image^jpg", facet.getSearchValue());
    }
}
