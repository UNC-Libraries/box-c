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
package edu.unc.lib.dl.ui.test;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/uiapp-servlet.xml" })
public class SolrSearchServiceIT extends Assert  {
    private static final Logger LOG = LoggerFactory.getLogger(SolrSearchService.class);
    @Resource
    private SolrSearchService solrSearchService = null;
    @Autowired
    private SearchSettings searchSettings;
    @Autowired
    private SearchStateFactory searchStateFactory;
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetObjectById() {
        AccessGroupSet accessGroups = new AccessGroupSet(new String[]{"all"});		
        SimpleIdRequest requestObject = new SimpleIdRequest("2345678", accessGroups);
        BriefObjectMetadataBean metadataBean = null;
        try {
            metadataBean = solrSearchService.getObjectById(requestObject);
            Assert.assertNotNull(metadataBean);
            LOG.debug(metadataBean.toString());
            requestObject.setId("invalidid");
            metadataBean = solrSearchService.getObjectById(requestObject);
            Assert.assertNull(metadataBean);
        } catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }
    
    
    //Faceted without query
    //Faceted with query
    
    
    //Blank search
    @Test
    public void testGetSearchResultsBlankSearch() {
        AccessGroupSet accessGroups = new AccessGroupSet(new String[]{"all"});
        SearchRequest requestObject = new SearchRequest();
        SearchState searchState = searchStateFactory.createSearchState();
        
        requestObject.setAccessGroups(accessGroups);
        searchState.setRowsPerPage(searchSettings.defaultPerPage);
        searchState.setStartRow(0);
        
        requestObject.setSearchState(searchState);
        SearchResultResponse response = null;
        try {
            response = solrSearchService.getSearchResults(requestObject);
            Assert.assertTrue(response.getResultCount() > 0);
            LOG.debug(response.toString());
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    //Navigation search, 2 page
    @Test
    public void testGetSearchResultsNavigationSearch() {
        AccessGroupSet accessGroups = new AccessGroupSet(new String[]{"all"});
        SearchRequest requestObject = new SearchRequest();
        SearchState searchState = searchStateFactory.createSearchState();
        
        requestObject.setAccessGroups(accessGroups);
        searchState.setRowsPerPage(2);
        searchState.setStartRow(3);
        
        requestObject.setSearchState(searchState);
        SearchResultResponse response = null;
        try {
            response = solrSearchService.getSearchResults(requestObject);
            Assert.assertTrue(response.getResultCount() > 0);
            LOG.debug(response.toString());
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    //Navigation search, invalid page
    @Test
    public void testGetSearchResultsNavigationSearchInvalidPage() {
        AccessGroupSet accessGroups = new AccessGroupSet(new String[]{"all"});
        SearchRequest requestObject = new SearchRequest();
        SearchState searchState = searchStateFactory.createSearchState();
        
        requestObject.setAccessGroups(accessGroups);
        searchState.setRowsPerPage(2);
        searchState.setStartRow(-4);
        
        requestObject.setSearchState(searchState);
        SearchResultResponse response = null;
        try {
            response = solrSearchService.getSearchResults(requestObject);
            
            Assert.assertTrue(response.getResultCount() == 0);
        } catch (Exception e){
            Assert.assertTrue(true);
        }
    }
    
    //Multiple query types
    @Test
    public void testGetSearchResultsMultipleQueries() {
        AccessGroupSet accessGroups = new AccessGroupSet(new String[]{"all"});
        SearchRequest requestObject = new SearchRequest();
        SearchState searchState = searchStateFactory.createSearchState();
        
        searchState.setSearchTermOperator("AND");
        requestObject.setAccessGroups(accessGroups);
        searchState.setRowsPerPage(searchSettings.defaultPerPage);
        searchState.setStartRow(0);
        
        HashMap<String,String> searchFields = new HashMap<String,String>();
        searchFields.put("text", "keyword");
        searchFields.put("contributorIndex", "ben");
        searchFields.put("subjectIndex", "\"Test Subject\"");
        
        searchState.setSearchFields(searchFields);
        
        requestObject.setSearchState(searchState);
        SearchResultResponse response = null;
        try {
            response = solrSearchService.getSearchResults(requestObject);
            Assert.assertTrue(response.getResultCount() > 0);
            LOG.debug(response.toString());
        } catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }
    
    //Quoted query Fields
    @Test
    public void testGetSearchResultsQuotedSearch() {
        AccessGroupSet accessGroups = new AccessGroupSet(new String[]{"all"});
        SearchRequest requestObject = new SearchRequest();
        SearchState searchState = searchStateFactory.createSearchState();
        
        requestObject.setAccessGroups(accessGroups);
        searchState.setRowsPerPage(searchSettings.defaultPerPage);
        searchState.setStartRow(0);
        
        HashMap<String,String> searchFields = new HashMap<String,String>();
        searchFields.put("text", "\"Example File\"");
        
        searchState.setSearchFields(searchFields);
        
        SearchResultResponse response = null;
        try {
            response = solrSearchService.getSearchResults(requestObject);
            Assert.assertTrue(response.getResultCount() > 0);
            LOG.debug(response.toString());
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    //Hierarchical, multiple facets with query
    //@Test
    /*public void testGetSearchResultsMultipleFacetsWithQuery() {
        AccessGroupSet accessGroups = new AccessGroupSet(new String[]{"all"});
        SearchRequest requestObject = new SearchRequest();
        SearchState searchState = searchStateFactory.createSearchState();
        
        requestObject.setAccessGroups(accessGroups);
        searchState.setRowsPerPage(searchSettings.defaultPerPage);
        searchState.setStartRow(0);
        
        HashMap<String,String> searchFields = new HashMap<String,String>();
        searchFields.put("text", "\"Example File\"");
        
        searchState.setSearchFields(searchFields);
        
        HashMap<String,Object> facets = new HashMap<String,String>();
        facets.put("ancestorPath", "2|9876543");
        facets.put("department", "UNC Libraries");
        
        searchState.setFacets(facets);
        
        requestObject.setSearchState(searchState);
        SearchResultResponse response = null;
        try {
            response = solrSearchService.getSearchResults(requestObject);
            Assert.assertTrue(response.getResultCount() > 0);
            LOG.debug(response.toString());
        } catch (Exception e){
            e.printStackTrace();
        }
    }*/
    
    //Query with sort
    @Test
    public void testGetSearchResultsSortSearch() {
        AccessGroupSet accessGroups = new AccessGroupSet(new String[]{"all"});
        SearchRequest requestObject = new SearchRequest();
        SearchState searchState = searchStateFactory.createSearchState();
        
        searchState.setSearchTermOperator("AND");
        requestObject.setAccessGroups(accessGroups);
        searchState.setRowsPerPage(searchSettings.defaultPerPage);
        searchState.setStartRow(0);
        
        HashMap<String,String> searchFields = new HashMap<String,String>();
        searchFields.put("text", "Example File");
        
        searchState.setSearchFields(searchFields);
        
        
        searchState.setSortType("dateUpdated");
        
        requestObject.setSearchState(searchState);
        SearchResultResponse response = null;
        try {
            response = solrSearchService.getSearchResults(requestObject);
            Assert.assertTrue(response.getResultCount() > 0);
            LOG.debug(response.toString());
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    //Collection sort
    @Test
    public void testGetSearchResultsCollectionSortSearch() {
        AccessGroupSet accessGroups = new AccessGroupSet(new String[]{"all"});
        SearchRequest requestObject = new SearchRequest();
        SearchState searchState = searchStateFactory.createSearchState();
        
        requestObject.setAccessGroups(accessGroups);
        searchState.setRowsPerPage(searchSettings.defaultPerPage);
        searchState.setStartRow(0);
        
        searchState.setSortType("collection");
        
        requestObject.setSearchState(searchState);
        SearchResultResponse response = null;
        try {
            response = solrSearchService.getSearchResults(requestObject);
            Assert.assertTrue(response.getResultCount() > 0);
            LOG.debug(response.toString());
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    //Request a restricted item by title without having permission
    @Test
    public void testGetSearchResultsRestrictedWithoutPermission() {
        AccessGroupSet accessGroups = new AccessGroupSet(new String[]{"all"});
        SearchRequest requestObject = new SearchRequest();
        SearchState searchState = searchStateFactory.createSearchState();
        
        requestObject.setAccessGroups(accessGroups);
        searchState.setRowsPerPage(searchSettings.defaultPerPage);
        searchState.setStartRow(0);
        
        HashMap<String,String> searchFields = new HashMap<String,String>();
        searchFields.put("titleIndex", "\"Restricted permission file\"");
        searchState.setSearchFields(searchFields);
        
        requestObject.setSearchState(searchState);
        SearchResultResponse response = null;
        try {
            response = solrSearchService.getSearchResults(requestObject);
            Assert.assertTrue(response.getResultCount() == 0);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    //Request a restricted item by title with permission
    @Test
    public void testGetSearchResultsRestrictedWithPermission() {
        AccessGroupSet accessGroups = new AccessGroupSet(new String[]{"all", "rla"});
        SearchRequest requestObject = new SearchRequest();
        SearchState searchState = searchStateFactory.createSearchState();
        
        requestObject.setAccessGroups(accessGroups);
        searchState.setRowsPerPage(searchSettings.defaultPerPage);
        searchState.setStartRow(0);
        
        HashMap<String,String> searchFields = new HashMap<String,String>();
        searchFields.put("titleIndex", "\"Restricted permission file\"");
        searchState.setSearchFields(searchFields);
        
        requestObject.setSearchState(searchState);
        SearchResultResponse response = null;
        try {
            response = solrSearchService.getSearchResults(requestObject);
            Assert.assertTrue(response.getResultCount() > 0);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    //Date range search
    //Quoted query Fields
    @Test
    public void testGetSearchResultsDateRange() {
        AccessGroupSet accessGroups = new AccessGroupSet(new String[]{"all"});
        SearchRequest requestObject = new SearchRequest();
        SearchState searchState = searchStateFactory.createSearchState();
        
        requestObject.setAccessGroups(accessGroups);
        
        HashMap<String,SearchState.RangePair> rangeFields = new HashMap<String,SearchState.RangePair>();
        rangeFields.put("dateUpdated", new SearchState.RangePair("2010-11-15T00:00:00Z", "2010-11-30T00:00:00Z"));
        rangeFields.put("dateAdded", new SearchState.RangePair("*", "2010-11-30T00:00:00Z"));
        
        searchState.setRangeFields(rangeFields);
        
        requestObject.setSearchState(searchState);
        SearchResultResponse response = null;
        try {
            response = solrSearchService.getSearchResults(requestObject);
            Assert.assertTrue(response.getResultCount() > 0);
            LOG.debug(response.toString());
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
