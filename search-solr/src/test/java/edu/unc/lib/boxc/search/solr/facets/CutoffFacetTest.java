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
package edu.unc.lib.boxc.search.solr.facets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.FacetField;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.exceptions.InvalidHierarchicalFacetException;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.facets.FacetFieldObject;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.facets.CutoffFacetImpl;
import edu.unc.lib.boxc.search.solr.services.FacetFieldFactory;

public class CutoffFacetTest extends Assert {

    @Test
    public void parseTest() {
        List<String> facetValues = Arrays.asList("1,uuid:a", "2,uuid:b", "3,uuid:c");

        CutoffFacetImpl facet = new CutoffFacetImpl(null, facetValues, 0);
        assertEquals(3,facet.getHighestTier());
        assertEquals("3,uuid:c",facet.getSearchValue());
        assertEquals("uuid:c",facet.getSearchKey());

        assertEquals(3, facet.getFacetNodes().size());
    }

    @Test
    public void getNode() {
        List<String> facetValues = Arrays.asList("1,uuid:a", "2,uuid:b", "3,uuid:c");
        CutoffFacet facet = new CutoffFacetImpl(null, facetValues, 0);

        assertEquals("1,uuid:a", facet.getNodeBySearchKey("uuid:a").getSearchValue());
        assertEquals("2,uuid:b", facet.getNodeBySearchKey("uuid:b").getSearchValue());
        assertEquals("3,uuid:c", facet.getNodeBySearchKey("uuid:c").getSearchValue());

        assertNull(facet.getNodeBySearchKey("uuid:d"));
    }

    @Test
    public void createInstance() {
        FacetField.Count count = mock(FacetField.Count.class);
        when(count.getCount()).thenReturn(1L);
        when(count.getName()).thenReturn("1,uuid:a");
        List<FacetField.Count> countList = Arrays.asList(count);

        FacetField facetField = mock(FacetField.class);
        when(facetField.getValues()).thenReturn(countList);

        FacetFieldFactory facetFieldFactory = new FacetFieldFactory();

        FacetFieldObject ffo = facetFieldFactory.createFacetFieldObject(SearchFieldKey.ANCESTOR_PATH, facetField);
        assertTrue(ffo.getValues().get(0) instanceof CutoffFacet);
        assertEquals("1,uuid:a", ffo.getValues().get(0).getSearchValue());
        assertEquals("1,uuid:a", ffo.getValues().get(0).getValue());
        assertEquals(1, ffo.getValues().get(0).getCount());
    }

    @Test
    public void facetStringConstructorTest() {
        CutoffFacet facet = new CutoffFacetImpl("ANCESTOR_PATH", "1,uuid:123456");

        assertEquals("1,uuid:123456", facet.getNodeBySearchKey("uuid:123456").getSearchValue());
    }

    @Test(expected=InvalidHierarchicalFacetException.class)
    public void nullfacetStringConstructorTest() {
        new CutoffFacetImpl("ANCESTOR_PATH", ",uuid:123456");
    }

    @Test
    public void constructWithCutoff() {
        CutoffFacetImpl facet = new CutoffFacetImpl("ANCESTOR_PATH", "3,uuid:test!5");

        assertEquals("ANCESTOR_PATH", facet.getFieldName());
        assertEquals("uuid:test", facet.getSearchKey());
        assertEquals("3,uuid:test", facet.getSearchValue());
        assertEquals(5, facet.getCutoff().intValue());
        assertNull(facet.getFacetCutoff());
    }

    @Test
    public void starKeyLimitTo() {
        CutoffFacetImpl depthFacet = new CutoffFacetImpl(SearchFieldKey.ANCESTOR_PATH.name(), "1,*");
        depthFacet.setCutoff(2);

        String limitToValue = depthFacet.getLimitToValue();
        assertEquals("1,*!2", limitToValue);
    }
}
