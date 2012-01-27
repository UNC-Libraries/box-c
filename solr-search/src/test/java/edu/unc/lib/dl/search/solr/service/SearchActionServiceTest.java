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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.search.solr.exception.InvalidHierarchicalFacetException;
import edu.unc.lib.dl.search.solr.model.HierarchicalFacet;
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
	public void executeActions(){
		SearchState searchState = new SearchState();
		
		//Action on null search state
		searchActionService.executeActions(null, searchSettings.actionName("SET_SEARCH_FIELD") + ":anywhere");
		
		//No actions
		searchActionService.executeActions(searchState, "nothing");
		assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
		searchActionService.executeActions(searchState, null);
		assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
	}
	
	@Test
	public void searchFieldActions(){
		SearchState searchState = new SearchState();
		
		//Missing parameter
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_SEARCH_FIELD") + ":anywhere");
		assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
		//Removing field that wasn't added
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_SEARCH_FIELD") + ":anywhere");
		assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
		//Adding correctly
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_SEARCH_FIELD") + ":anywhere,hello");
		assertFalse(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
		//Removing
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_SEARCH_FIELD") + ":anywhere");
		assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
		
		//Adding multiple, with duplicate
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_SEARCH_FIELD") + ":anywhere,hello");
		assertFalse(searchState.getSearchFields().containsKey("anywhere"));
		//Verify that it remapped the index name to the internal name
		assertTrue(searchState.getSearchFields().containsKey("DEFAULT_INDEX"));
		assertTrue(searchState.getSearchFields().get("DEFAULT_INDEX").equals("hello"));
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_SEARCH_FIELD") + ":titleIndex,hello");
		//Non-existent search field
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_SEARCH_FIELD") + ":author,hello");
		assertFalse(searchState.getSearchFields().containsKey("author"));
		assertFalse(searchState.getSearchFields().containsKey(null));
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_SEARCH_FIELD") + ":contributorIndex,hello");
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_SEARCH_FIELD") + ":subjectIndex,hello");
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_SEARCH_FIELD") + ":anywhere,hello");
		assertEquals(searchState.getSearchFields().size(), 4);
		
		//Remove nothing
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_SEARCH_FIELD"));
		assertEquals(searchState.getSearchFields().size(), 4);
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_SEARCH_FIELD") + ":title");
		assertEquals(searchState.getSearchFields().size(), 4);
		//Remove
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_SEARCH_FIELD") + ":titleIndex");
		assertEquals(searchState.getSearchFields().size(), 3);
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_SEARCH_FIELD") + ":anywhere");
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_SEARCH_FIELD") + ":contributorIndex");
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_SEARCH_FIELD") + ":subjectIndex");
		assertEquals(searchState.getSearchFields().size(), 0);
		
		//Test add field
		searchActionService.executeActions(searchState, searchSettings.actionName("ADD_SEARCH_FIELD") + ":anywhere,hello");
		assertTrue("hello".equals(searchState.getSearchFields().get("DEFAULT_INDEX")));
		searchActionService.executeActions(searchState, searchSettings.actionName("ADD_SEARCH_FIELD") + ":anywhere,world");
		assertTrue("hello world".equals(searchState.getSearchFields().get("DEFAULT_INDEX")));
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_SEARCH_FIELD") + ":anywhere,!");
		assertTrue("!".equals(searchState.getSearchFields().get("DEFAULT_INDEX")));
		//Too many parameters
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_SEARCH_FIELD") + ":anywhere,hello,world");
		assertTrue("!".equals(searchState.getSearchFields().get("DEFAULT_INDEX")));
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_SEARCH_FIELD") + ":anywhere");
		
		searchState = new SearchState();
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_SEARCH_FIELD") + ":format,hello \"1,audio\" world woo \"doit\"");
		assertEquals(searchState.getSearchFields().size(), 1);
	}
	
	@Test
	public void pagingActions(){
		SearchState searchState = new SearchState();
		
		//Rows per page not set, no change
		searchActionService.executeActions(searchState, searchSettings.actionName("NEXT_PAGE"));
		assertEquals(searchState.getStartRow(), 0);
		
		//Can't have negative rows per page
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_ROWS_PER_PAGE") + ":-1");
		assertEquals(searchState.getRowsPerPage().intValue(), 0);
		
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_ROWS_PER_PAGE") + ":" + searchSettings.getDefaultPerPage());
		assertEquals(searchState.getRowsPerPage().intValue(), searchSettings.getDefaultPerPage());
		searchActionService.executeActions(searchState, searchSettings.actionName("NEXT_PAGE"));
		assertEquals(searchState.getStartRow(), searchSettings.getDefaultPerPage());
		
		//Action that should have a parameter
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_START_ROW"));
		assertEquals(searchState.getStartRow(), searchSettings.getDefaultPerPage());
		
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_START_ROW") + ":0");
		assertEquals(searchState.getStartRow(), 0);
		
		//Test action that takes no parameters, with a param
		searchActionService.executeActions(searchState, searchSettings.actionName("NEXT_PAGE") + ":anywhere");
		assertEquals(searchState.getStartRow(), searchSettings.getDefaultPerPage());
		
		searchActionService.executeActions(searchState, searchSettings.actionName("PREVIOUS_PAGE"));
		assertEquals(searchState.getStartRow(), 0);
		//Verify that can't go to a negative page
		searchActionService.executeActions(searchState, searchSettings.actionName("PREVIOUS_PAGE"));
		assertEquals(searchState.getStartRow(), 0);
	}
	
	@Test
	public void rangeActions(){
		SearchState searchState = new SearchState();
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_RANGE_FIELD") + ":" 
				+ searchSettings.getSearchFieldParam("DATE_ADDED"));
		assertEquals(searchState.getRangeFields().size(), 0);
		
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_RANGE_FIELD") + ":" +
				searchSettings.getSearchFieldParam("DATE_ADDED") + ",2011");
		assertEquals(searchState.getRangeFields().size(), 0);
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_RANGE_FIELD") + ":" +
				searchSettings.getSearchFieldParam("DATE_ADDED") + ",2010,2011");
		assertEquals(searchState.getRangeFields().size(), 1);
		assertTrue(searchState.getRangeFields().get("DATE_ADDED").getLeftHand().equals("2010") && 
				searchState.getRangeFields().get("DATE_ADDED").getRightHand().equals("2011"));
		
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_RANGE_FIELD") + ":" 
				+ searchSettings.getSearchFieldParam("DATE_ADDED"));
		assertEquals(searchState.getRangeFields().size(), 0);
		
		//Test empty range boundries
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_RANGE_FIELD") + ":" +
				searchSettings.getSearchFieldParam("DATE_ADDED") + ",2010,");
		assertEquals(searchState.getRangeFields().size(), 1);
		assertTrue(searchState.getRangeFields().get("DATE_ADDED").getLeftHand().equals("2010") && 
				searchState.getRangeFields().get("DATE_ADDED").getRightHand() == null);
		
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_RANGE_FIELD") + ":" +
				searchSettings.getSearchFieldParam("DATE_ADDED") + ",,2011");
		assertEquals(searchState.getRangeFields().size(), 1);
		assertTrue(searchState.getRangeFields().get("DATE_ADDED").getLeftHand() == null && 
				searchState.getRangeFields().get("DATE_ADDED").getRightHand().equals("2011"));
		
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_RANGE_FIELD") + ":" +
				searchSettings.getSearchFieldParam("DATE_ADDED") + ",,");
		assertEquals(searchState.getRangeFields().size(), 1);
		assertTrue(searchState.getRangeFields().get("DATE_ADDED").getLeftHand() == null && 
				searchState.getRangeFields().get("DATE_ADDED").getRightHand() == null);
		
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_RANGE_FIELD") + ":" 
				+ searchSettings.getSearchFieldParam("DATE_ADDED"));
		
		//Empty boundry with too few parameters
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_RANGE_FIELD") + ":" +
				searchSettings.getSearchFieldParam("DATE_ADDED") + ",");
		assertEquals(searchState.getRangeFields().size(), 0);
		
		//Multiple
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_RANGE_FIELD") + ":" +
				searchSettings.getSearchFieldParam("DATE_ADDED") + ",2010,2011");
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_RANGE_FIELD") + ":" +
				searchSettings.getSearchFieldParam("DATE_CREATED") + ",2010-06,2011");
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_RANGE_FIELD") + ":invalidfield,2010-06,2011");
		assertEquals(searchState.getRangeFields().size(), 2);
	}
	
	@Test
	public void sortActions(){
		SearchState searchState = new SearchState();
		
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_SORT"));
		assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_SORT") + ":title");
		assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_SORT") + ":title,desc");
		assertTrue(searchState.getSortType().equals("title") && searchState.getSortOrder().equals("desc"));
	}
	
	@Test
	public void accessFilterActions(){
		SearchState searchState = new SearchState();
		
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_ACCESS_FILTER"));
		assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_ACCESS_FILTER") + ":invalidField");
		assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_ACCESS_FILTER") + ":" +
				searchSettings.getSearchFieldParam("FILE_ACCESS"));
		assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_ACCESS_FILTER"));
		assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_ACCESS_FILTER") + ":" +
				searchSettings.getSearchFieldParam("FILE_ACCESS"));
		assertTrue(searchState.getAccessTypeFilter().equals("FILE_ACCESS"));
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_ACCESS_FILTER") + ":" +
				searchSettings.getSearchFieldParam("RECORD_ACCESS"));
		assertTrue(searchState.getAccessTypeFilter().equals("RECORD_ACCESS"));
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_ACCESS_FILTER"));
		assertNull(searchState.getAccessTypeFilter());
	}
	
	@Test
	public void resourceTypeActions(){
		SearchState searchState = new SearchState();
		
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_RESOURCE_TYPE"));
		assertTrue(emptyStateString.equals(SearchStateUtil.generateStateParameterString(searchState)));
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_RESOURCE_TYPE") + ":");
		assertEquals(searchState.getResourceTypes().size(), 0);
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_RESOURCE_TYPE") + ":,,,,,,,,");
		assertEquals(searchState.getResourceTypes().size(), 0);
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_RESOURCE_TYPE") + ":file");
		assertEquals(searchState.getResourceTypes().size(), 1);
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_RESOURCE_TYPE") + ":folder");
		assertEquals(searchState.getResourceTypes().size(), 1);
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_RESOURCE_TYPE") + ":file,folder,collection");
		assertEquals(searchState.getResourceTypes().size(), 3);
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_RESOURCE_TYPE"));
		assertEquals(searchState.getResourceTypes().size(), 3);
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_RESOURCE_TYPE") + ":,,,,,,,,,,");
		assertEquals(searchState.getResourceTypes().size(), 3);
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_RESOURCE_TYPE") + ":invalid,resource,types,go,here,thank,you");
		assertEquals(searchState.getResourceTypes().size(), 3);
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_RESOURCE_TYPE") + ":file,folder");
		assertEquals(searchState.getResourceTypes().size(), 1);
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_RESOURCE_TYPE") + ":file,folder,collection");
		assertEquals(searchState.getResourceTypes().size(), 0);
	}
	
	@Test
	public void facetActions(){
		SearchState searchState = new SearchState();
		
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET"));
		assertEquals(searchState.getFacets().size(), 0);
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET") + ":invalid");
		assertEquals(searchState.getFacets().size(), 0);
		//https://cdr.lib.unc.edu/search?action=setFacet%3aformat%2c%221%2ctext%22&sort=default&sortOrder=&rows=20
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET") + ":format");
		assertEquals(searchState.getFacets().size(), 0);
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET") + ":language,");
		assertEquals(searchState.getFacets().size(), 0);
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET") + ":language,English");
		assertEquals(searchState.getFacets().size(), 1);
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_FACET"));
		assertEquals(searchState.getFacets().size(), 1);
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_FACET") + ":language");
		assertEquals(searchState.getFacets().size(), 0);
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET") + ":language,\"English\"");
		assertEquals(searchState.getFacets().size(), 1);
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET") + ":language,French");
		assertEquals(searchState.getFacets().size(), 1);
		assertTrue(searchState.getFacets().get("LANGUAGE").equals("French"));
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET") + ":subject,France");
		assertEquals(searchState.getFacets().size(), 2);
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_FACET") + ":language");
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_FACET") + ":subject");
		assertEquals(searchState.getFacets().size(), 0);
		//Invalid hier
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET") + ":format,\"audio\"");
		assertEquals(searchState.getFacets().size(), 0);
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET") + ":format,\"1,audio\"");
		assertEquals(searchState.getFacets().size(), 1);
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET") + ":format,\"1%2caudio\"");
		assertEquals(searchState.getFacets().size(), 1);
		assertTrue(searchState.getFacets().get("CONTENT_TYPE").getClass().equals(HierarchicalFacet.class));
		searchActionService.executeActions(searchState, searchSettings.actionName("REMOVE_FACET") + ":format");
		assertEquals(searchState.getFacets().size(), 0);
		try {
			searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET") + ":format,\"1,audio,wav\"");
			assertTrue(false);
		} catch (InvalidHierarchicalFacetException e){
			assertEquals(searchState.getFacets().size(), 0);
		}
		//Invalid third field, should be a cutoff
		try {
			searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET") + ":format,\"1%2caudio%2cwav\"");
			assertTrue(false);
		} catch (InvalidHierarchicalFacetException e){
			assertEquals(searchState.getFacets().size(), 0);
		}
		
		//With cutoff
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET") + ":format,\"1%2caudio%2c2\"");
		assertEquals(searchState.getFacets().size(), 1);
		assertTrue(searchState.getFacets().get("CONTENT_TYPE").getClass().equals(HierarchicalFacet.class));
		
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET_SELECT"));
		assertNull(searchState.getFacetsToRetrieve());
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET_SELECT") + ":");
		assertEquals(searchState.getFacetsToRetrieve().size(),1);
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET_SELECT") + ":format,");
		assertEquals(searchState.getFacetsToRetrieve().size(), 2);
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET_SELECT") + ":format");
		assertEquals(searchState.getFacetsToRetrieve().size(), 1);
		searchActionService.executeActions(searchState, searchSettings.actionName("SET_FACET_SELECT") + ":format,contributor,subject");
		assertEquals(searchState.getFacetsToRetrieve().size(), 3);
	}
}
