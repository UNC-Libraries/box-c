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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class CaseInsensitiveFacetTest extends Assert {

    @Test
    public void deduplicateTest() {

        List<GenericFacet> facetList = new ArrayList<GenericFacet>();
        facetList.add(new CaseInsensitiveFacet("f", "aaa"));
        CaseInsensitiveFacet facet = new CaseInsensitiveFacet("f", "baa");
        facet.setCount(2);
        facetList.add(facet);
        facet = new CaseInsensitiveFacet("f", "BBa");
        facet.setCount(16);
        facetList.add(facet);
        facet = new CaseInsensitiveFacet("f", "bba");
        facet.setCount(1);
        facetList.add(facet);
        facet = new CaseInsensitiveFacet("f", "bBa");
        facet.setCount(2);
        facetList.add(facet);
        facetList.add(new CaseInsensitiveFacet("f", "dDd"));
        facetList.add(new CaseInsensitiveFacet("f", "ddd"));
        facet = new CaseInsensitiveFacet("f", "ccc");
        facet.setCount(3);
        facetList.add(facet);


        FacetFieldObject facetFieldObject = new FacetFieldObject("f", facetList);
        CaseInsensitiveFacet.deduplicateCaseInsensitiveValues(facetFieldObject);

        assertEquals(5, facetFieldObject.getValues().size());

        // aaa
        assertEquals("aaa", facetFieldObject.getValues().get(0).getDisplayValue());
        assertEquals(0, facetFieldObject.getValues().get(0).getCount());
        // baa
        assertEquals("baa", facetFieldObject.getValues().get(1).getDisplayValue());
        assertEquals(2, facetFieldObject.getValues().get(1).getCount());
        // BBa
        assertEquals("BBa", facetFieldObject.getValues().get(2).getDisplayValue());
        assertEquals(19, facetFieldObject.getValues().get(2).getCount());
        // dDd
        assertEquals("dDd", facetFieldObject.getValues().get(3).getDisplayValue());
        assertEquals(0, facetFieldObject.getValues().get(3).getCount());
        // ccc
        assertEquals("ccc", facetFieldObject.getValues().get(4).getDisplayValue());
        assertEquals(3, facetFieldObject.getValues().get(4).getCount());
    }
}
