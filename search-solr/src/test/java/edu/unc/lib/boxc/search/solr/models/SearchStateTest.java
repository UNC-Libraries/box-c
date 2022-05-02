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
package edu.unc.lib.boxc.search.solr.models;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.SearchFacet;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SearchStateTest {

    @Test
    public void fileFormatCategoryFacetCloning() {
        SearchState searchState = new SearchState();
        searchState.setFacet(new GenericFacet(SearchFieldKey.FILE_FORMAT_CATEGORY.name(), "Text"));

        SearchState searchStatePartDeux = new SearchState(searchState);
        List<SearchFacet> facetObject = searchStatePartDeux.getFacets().get(SearchFieldKey.FILE_FORMAT_CATEGORY.name());
        assertNotNull(facetObject);

        assertTrue(facetObject.get(0) instanceof GenericFacet);
        assertEquals("Text", facetObject.get(0).getSearchValue());
    }
}
