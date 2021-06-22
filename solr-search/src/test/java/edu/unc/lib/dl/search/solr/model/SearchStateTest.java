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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;

public class SearchStateTest {

    @Test
    public void hierarchicalFacetCloning() {
        SearchState searchState = new SearchState();
        searchState.setFacet(SearchFieldKeys.CONTENT_TYPE,
                new MultivaluedHierarchicalFacet("CONTENT_TYPE", "^text,Text"));

        SearchState searchStatePartDeux = new SearchState(searchState);
        List<SearchFacet> facetObject = searchStatePartDeux.getFacets().get("CONTENT_TYPE");
        assertNotNull(facetObject);

        assertTrue(facetObject.get(0) instanceof MultivaluedHierarchicalFacet);

        assertEquals(1, ((AbstractHierarchicalFacet)facetObject.get(0)).getFacetNodes().size());

    }
}
