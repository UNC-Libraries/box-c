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
package edu.unc.lib.dl.search.solr.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.GenericFacet;
import edu.unc.lib.dl.search.solr.model.MultivaluedHierarchicalFacet;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/solr-search-context-unit.xml" })
public class SearchActionServiceTest extends Assert {
    @Autowired
    private SearchSettings searchSettings;
    @Autowired
    private SearchActionService searchActionService;
    private String emptyStateString = null;

    @Before
    public void setUp() throws Exception {
        SearchState searchState = new SearchState();
        this.emptyStateString = SearchStateUtil.generateStateParameterString(searchState);
    }

    @Test
    public void executeActions() {
        SearchState searchState = new SearchState();

        //Action on null search state
        searchActionService.executeActions(null, new LinkedHashMap<String,String[]>());

        //No actions
        searchActionService.executeActions(searchState, new LinkedHashMap<String,String[]>());
        assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
        searchActionService.executeActions(searchState, null);
        assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
    }

    @Test
    public void searchFieldActions() {
        SearchState searchState = new SearchState();

        Map<String,String[]> params = new LinkedHashMap<String,String[]>();
        //Missing parameter
        params.put("a." + searchSettings.actionName("SET_SEARCH_FIELD"), new String[]{"anywhere"});
        searchActionService.executeActions(searchState, params);
        assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
        //Removing field that wasn't added
        params.clear();
        params.put("a." + searchSettings.actionName("REMOVE_SEARCH_FIELD"), new String[]{"anywhere"});
        searchActionService.executeActions(searchState, params);
        assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
        //Adding correctly
        params.clear();
        params.put("a." + searchSettings.actionName("SET_SEARCH_FIELD"), new String[]{"anywhere:hello"});
        searchActionService.executeActions(searchState, params);
        assertFalse("".equals(SearchStateUtil.generateStateParameterString(searchState)));
        //Removing
        params.clear();
        params.put("a." + searchSettings.actionName("REMOVE_SEARCH_FIELD"), new String[]{"anywhere"});
        searchActionService.executeActions(searchState, params);
        assertEquals("", SearchStateUtil.generateStateParameterString(searchState));

        //Adding multiple, with duplicate
        params.clear();
        params.put("a." + searchSettings.actionName("SET_SEARCH_FIELD"), new String[]{"anywhere:hello"});
        searchActionService.executeActions(searchState, params);
        assertFalse(searchState.getSearchFields().containsKey("anywhere"));
        //Verify that it remapped the index name to the internal name
        assertTrue(searchState.getSearchFields().containsKey("DEFAULT_INDEX"));
        assertTrue(searchState.getSearchFields().get("DEFAULT_INDEX").equals("hello"));
        params.clear();
        params.put("a." + searchSettings.actionName("SET_SEARCH_FIELD"), new String[]{"titleIndex:hello"});
        searchActionService.executeActions(searchState, params);
        //Non-existent search field
        params.clear();
        params.put("a." + searchSettings.actionName("SET_SEARCH_FIELD"), new String[]{"author:hello"});
        searchActionService.executeActions(searchState, params);
        assertFalse(searchState.getSearchFields().containsKey("author"));
        assertFalse(searchState.getSearchFields().containsKey(null));
        params.clear();
        params.put("a." + searchSettings.actionName("SET_SEARCH_FIELD"), new String[]{"contributorIndex:hello", "subjectIndex:hello", "anywhere:hello"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getSearchFields().size(), 4);

        //Remove nothing
        params.clear();
        params.put("a." + searchSettings.actionName("REMOVE_SEARCH_FIELD"), new String[]{});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getSearchFields().size(), 4);
        params.clear();
        params.put("a." + searchSettings.actionName("REMOVE_SEARCH_FIELD"), new String[]{"title"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getSearchFields().size(), 4);
        //Remove
        params.clear();
        params.put("a." + searchSettings.actionName("REMOVE_SEARCH_FIELD"), new String[]{"titleIndex"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getSearchFields().size(), 3);
        params.clear();
        params.put("a." + searchSettings.actionName("REMOVE_SEARCH_FIELD"), new String[]{"anywhere", "contributorIndex", "subjectIndex"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getSearchFields().size(), 0);

        //Test add field
        params.clear();
        params.put("a." + searchSettings.actionName("ADD_SEARCH_FIELD"), new String[]{"anywhere:hello"});
        searchActionService.executeActions(searchState, params);
        assertTrue("hello".equals(searchState.getSearchFields().get("DEFAULT_INDEX")));
        params.put("a." + searchSettings.actionName("ADD_SEARCH_FIELD"), new String[]{"anywhere:world"});
        searchActionService.executeActions(searchState, params);
        assertTrue("hello world".equals(searchState.getSearchFields().get("DEFAULT_INDEX")));
    }

    @Test
    public void pagingActions() {
        Map<String,String[]> params = new LinkedHashMap<String,String[]>();
        SearchState searchState = new SearchState();

        //Rows per page not set, no change
        params.put("a." + searchSettings.actionName("NEXT_PAGE"), new String[]{});
        searchActionService.executeActions(searchState, params);
        assertNull(searchState.getStartRow());

        //Can't have negative rows per page
        params.clear();
        params.put("a." + searchSettings.actionName("SET_ROWS_PER_PAGE"), new String[]{"-1"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getRowsPerPage().intValue(), 0);

        params.put("a." + searchSettings.actionName("SET_ROWS_PER_PAGE"), new String[]{Integer.toString(searchSettings.getDefaultPerPage())});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getRowsPerPage().intValue(), searchSettings.getDefaultPerPage());
        params.clear();
        params.put("a." + searchSettings.actionName("NEXT_PAGE"), new String[]{});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getStartRow().intValue(), searchSettings.getDefaultPerPage());

        //Action that should have a parameter
        params.clear();
        params.put("a." + searchSettings.actionName("SET_START_ROW"), new String[]{Integer.toString(searchSettings.getDefaultPerPage())});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getStartRow().intValue(), searchSettings.getDefaultPerPage());

        params.put("a." + searchSettings.actionName("SET_START_ROW"), new String[]{Integer.toString(0)});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getStartRow().intValue(), 0);

        //Test action that takes no parameters, with a param
        params.clear();
        params.put("a." + searchSettings.actionName("NEXT_PAGE"), new String[]{"anywere"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getStartRow().intValue(), searchSettings.getDefaultPerPage());

        params.clear();
        params.put("a." + searchSettings.actionName("PREVIOUS_PAGE"), new String[]{});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getStartRow().intValue(), 0);
        //Verify that can't go to a negative page
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getStartRow().intValue(), 0);
    }

    @Test
    public void rangeActions() {
        Map<String,String[]> params = new LinkedHashMap<String,String[]>();
        SearchState searchState = new SearchState();
        params.put("a." + searchSettings.actionName("SET_RANGE_FIELD"), new String[]{"added"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getRangeFields().size(), 0);

        params.put("a." + searchSettings.actionName("SET_RANGE_FIELD"), new String[]{"added:2011"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getRangeFields().size(), 0);
        params.put("a." + searchSettings.actionName("SET_RANGE_FIELD"), new String[]{"added:2010,2011"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getRangeFields().size(), 1);
        assertTrue(searchState.getRangeFields().get("DATE_ADDED").getLeftHand().equals("2010") &&
                searchState.getRangeFields().get("DATE_ADDED").getRightHand().equals("2011"));

        params.clear();
        params.put("a." + searchSettings.actionName("REMOVE_RANGE_FIELD"), new String[]{"added"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getRangeFields().size(), 0);

        //Test empty range boundries
        params.clear();
        params.put("a." + searchSettings.actionName("SET_RANGE_FIELD"), new String[]{"added:2010,"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getRangeFields().size(), 1);
        assertTrue(searchState.getRangeFields().get("DATE_ADDED").getLeftHand().equals("2010") &&
                searchState.getRangeFields().get("DATE_ADDED").getRightHand() == null);

        params.put("a." + searchSettings.actionName("SET_RANGE_FIELD"), new String[]{"added:,2011"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getRangeFields().size(), 1);
        assertTrue(searchState.getRangeFields().get("DATE_ADDED").getLeftHand() == null &&
                searchState.getRangeFields().get("DATE_ADDED").getRightHand().equals("2011"));

        params.put("a." + searchSettings.actionName("SET_RANGE_FIELD"), new String[]{"added:,"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getRangeFields().size(), 1);
        assertTrue(searchState.getRangeFields().get("DATE_ADDED").getLeftHand() == null &&
                searchState.getRangeFields().get("DATE_ADDED").getRightHand() == null);

        params.clear();
        params.put("a." + searchSettings.actionName("REMOVE_RANGE_FIELD"), new String[]{"added"});
        searchActionService.executeActions(searchState, params);

        //Empty boundry with too few parameters
        params.clear();
        params.put("a." + searchSettings.actionName("SET_RANGE_FIELD"), new String[]{"added:"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getRangeFields().size(), 0);

        //Multiple
        params.put("a." + searchSettings.actionName("SET_RANGE_FIELD"), new String[]{"added:2010,2011", "created:2010-06,2011", "invalidField:2010-06,2011"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getRangeFields().size(), 2);
    }

    @Test
    public void resourceTypeActions() {
        Map<String,String[]> params = new LinkedHashMap<String,String[]>();
        SearchState searchState = new SearchState();

        params.put("a." + searchSettings.actionName("SET_RESOURCE_TYPE"), new String[]{});
        searchActionService.executeActions(searchState, params);
        assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
        params.put("a." + searchSettings.actionName("SET_RESOURCE_TYPE"), new String[]{",,,,,"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getResourceTypes().size(), 0);
        params.put("a." + searchSettings.actionName("SET_RESOURCE_TYPE"), new String[]{"file"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getResourceTypes().size(), 1);
        params.put("a." + searchSettings.actionName("SET_RESOURCE_TYPE"), new String[]{"folder"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getResourceTypes().size(), 1);
        params.put("a." + searchSettings.actionName("SET_RESOURCE_TYPE"), new String[]{"file,folder,collection"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getResourceTypes().size(), 3);
        params.put("a." + searchSettings.actionName("REMOVE_RESOURCE_TYPE"), new String[]{});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getResourceTypes().size(), 3);
        params.put("a." + searchSettings.actionName("REMOVE_RESOURCE_TYPE"), new String[]{"invalid,resource,types,go,here"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getResourceTypes().size(), 3);
        params.put("a." + searchSettings.actionName("REMOVE_RESOURCE_TYPE"), new String[]{"file,folder"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getResourceTypes().size(), 1);
        params.put("a." + searchSettings.actionName("REMOVE_RESOURCE_TYPE"), new String[]{"file,folder,collection"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getResourceTypes().size(), 0);
    }

    @Test
    public void facetActions() {
        Map<String,String[]> params = new LinkedHashMap<String,String[]>();
        SearchState searchState = new SearchState();

        params.put("a." + searchSettings.actionName("SET_FACET"), new String[]{});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacets().size(), 0);
        params.put("a." + searchSettings.actionName("SET_FACET"), new String[]{"invalid"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacets().size(), 0);
        params.put("a." + searchSettings.actionName("SET_FACET"), new String[]{"format"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacets().size(), 0);
        params.put("a." + searchSettings.actionName("SET_FACET"), new String[]{"language:"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacets().size(), 0);
        params.put("a." + searchSettings.actionName("SET_FACET"), new String[]{"language:English"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacets().size(), 1);
        params.clear();
        params.put("a." + searchSettings.actionName("REMOVE_FACET"), new String[]{});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacets().size(), 1);
        params.put("a." + searchSettings.actionName("REMOVE_FACET"), new String[]{"language"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacets().size(), 0);
        params.clear();
        params.put("a." + searchSettings.actionName("SET_FACET"), new String[]{"language:English"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacets().size(), 1);
        params.put("a." + searchSettings.actionName("SET_FACET"), new String[]{"language:French"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacets().size(), 1);
        assertEquals("French", ((GenericFacet)searchState.getFacets().get("LANGUAGE")).getDisplayValue());
        params.put("a." + searchSettings.actionName("SET_FACET"), new String[]{"subject:France"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacets().size(), 2);
        params.clear();
        params.put("a." + searchSettings.actionName("REMOVE_FACET"), new String[]{"language", "subject"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacets().size(), 0);
        params.clear();
        params.put("a." + searchSettings.actionName("SET_FACET"), new String[]{"language:English", "subject:France"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacets().size(), 2);
    }

    @Test
    public void hierarchicalFacetTest() {
        Map<String,String[]> params = new LinkedHashMap<String,String[]>();
        SearchState searchState = new SearchState();

        assertEquals(0, searchState.getFacets().size());
        params.put("a." + searchSettings.actionName("SET_FACET"), new String[]{"format:audio"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacets().size(), 1);
//        params.put("a." + searchSettings.actionName("SET_FACET"), new String[]{"format:%5Eaudio"});
//        searchActionService.executeActions(searchState, params);
//        assertEquals(searchState.getFacets().size(), 1);
        assertTrue(searchState.getFacets().get("CONTENT_TYPE").getClass().equals(MultivaluedHierarchicalFacet.class));
        params.clear();
        params.put("a." + searchSettings.actionName("REMOVE_FACET"), new String[]{"format"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacets().size(), 0);
        params.clear();
        params.put("a." + searchSettings.actionName("SET_FACET"), new String[]{"format:audio/wav"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacets().size(), 1);


    }

    @Test
    public void facetSelectTest() {
        Map<String,String[]> params = new LinkedHashMap<String,String[]>();
        SearchState searchState = new SearchState();

        params.put("a." + searchSettings.actionName("SET_FACET_SELECT"), new String[]{});
        searchActionService.executeActions(searchState, params);
        assertNull(searchState.getFacetsToRetrieve());
        params.put("a." + searchSettings.actionName("SET_FACET_SELECT"), new String[]{""});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacetsToRetrieve().size(),1);
        params.put("a." + searchSettings.actionName("SET_FACET_SELECT"), new String[]{"format,"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacetsToRetrieve().size(), 1);
        params.put("a." + searchSettings.actionName("SET_FACET_SELECT"), new String[]{"format"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacetsToRetrieve().size(), 1);
        params.put("a." + searchSettings.actionName("SET_FACET_SELECT"), new String[]{"format,contributor,subject"});
        searchActionService.executeActions(searchState, params);
        assertEquals(searchState.getFacetsToRetrieve().size(), 3);
    }

    @Test
    public void cutoffFacet() {
        Map<String,String[]> params = new LinkedHashMap<String,String[]>();
        SearchState searchState = new SearchState();

        params.put("a." + searchSettings.actionName("SET_FACET"), new String[]{"path:2,uuid:test!3"});
        searchActionService.executeActions(searchState, params);

        CutoffFacet facet = (CutoffFacet)searchState.getFacets().get("ANCESTOR_PATH");

        assertEquals(3, facet.getCutoff().intValue());
        assertEquals("uuid:test", facet.getSearchKey());
    }

    @Test
    public void cutoffAndReset() {
        Map<String,String[]> params = new LinkedHashMap<String,String[]>();
        SearchState searchState = new SearchState();

        params.put("a." + searchSettings.actionName("SET_FACET"), new String[]{"path:2,uuid:test!3"});
        params.put("a." + searchSettings.actionName("RESET_NAV"), new String[]{"search"});
        searchActionService.executeActions(searchState, params);

        CutoffFacet facet = (CutoffFacet)searchState.getFacets().get("ANCESTOR_PATH");

        assertEquals(3, facet.getCutoff().intValue());
        assertEquals("uuid:test", facet.getSearchKey());
    }
}
