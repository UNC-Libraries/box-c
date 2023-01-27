package edu.unc.lib.boxc.search.solr.models;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.SearchFacet;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
