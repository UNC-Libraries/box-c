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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.search.solr.util.SearchSettings;

public class HierarchicalFacetTest extends Assert {
	private static final Logger LOG = LoggerFactory.getLogger(HierarchicalFacetTest.class);
	private SearchSettings searchSettings;
	
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
   	searchSettings = new SearchSettings();
   	searchSettings.setFacetSubfieldDelimiter(",");
   	searchSettings.setFacetTierDelimiter("/");
   	HierarchicalFacet hier = new HierarchicalFacet();
   	hier.setSearchSettings(searchSettings);
   }

   /**
    * @throws java.lang.Exception
    */
   @After
   public void tearDown() throws Exception {
   }
   
   @Test
   public void defaultConstructorTest(){
   	try {
   		new HierarchicalFacet();
   		assertTrue(true);
   	} catch (Exception e){
   		LOG.error("Test failed", e);
   		assertFalse(true);
   	}
   }
   
   @Test
   public void constructorTest(){
   	try {
   		String displayValue = "Display Value";
   		String id = "uuid:1234567";
   		HierarchicalFacet facet = new HierarchicalFacet("facet name", "1," + id + "," + displayValue);
   		
   		assertTrue(facet.getDisplayValue().equals(displayValue));
   		
   		LOG.info(facet.getSearchKey());
   		assertTrue(facet.getSearchKey().equals(id));
   		
   		assertTrue(facet.getHighestTier() == 1);
   		
   		assertTrue(facet.getFacetTiers().size() == 1);
   	} catch (Exception e){
   		assertFalse(true);
   	}
   	
   	try {
   		HierarchicalFacet facet = new HierarchicalFacet("facet name", "uuid:1234567");
   		assertTrue(facet.getFacetTiers().size() == 0);
   	} catch (RuntimeException e){
   		assertTrue(true);
   	}
   	
   	try {
   		HierarchicalFacet facet = new HierarchicalFacet("facet name", "1,uuid:1234567,Collection/2,uuid:2345678,Folder/3,uuid:3456789,Item");
   		
   		assertTrue(facet.getFacetTiers().size() == 3);
   		
   		assertTrue(facet.getHighestTier() == 3);
   		
   		assertTrue(facet.getDisplayValue().equals("Item"));
   		
   	} catch (Exception e){
   		assertFalse(true);
   	}
   }
}
