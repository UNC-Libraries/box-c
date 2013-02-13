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

import java.io.FileInputStream;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.FacetFieldFactory;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchSettings;

public class SearchStateFactoryTest extends Assert {

	@Test
	public void nullTier() throws Exception {
		Properties properties = new Properties();
		properties.load(new FileInputStream("src/test/resources/search.properties"));
		
		SearchSettings searchSettings = new SearchSettings();
		searchSettings.setProperties(properties);
		
		SearchStateFactory searchStateFactory = new SearchStateFactory();
		searchStateFactory.setSearchSettings(searchSettings);
		MockHttpServletRequest request = new MockHttpServletRequest(
				"GET",
				"https://localhost/search?action=setFacet%3apath%2c%221%2cuuid%3ac34ae354-8626-48c6-9963-d907aa65a713%22&sort=default&sortOrder=&facets=path:null&terms=anywhere:&rows=20");
		SearchState searchState = searchStateFactory.createSearchState(request.getParameterMap());
		
		assertFalse(searchState.getFacets().containsKey("ANCESTOR_PATH"));
	}
	
	
	@Test
	public void cutoffTier() throws Exception {
		Properties properties = new Properties();
		properties.load(new FileInputStream("src/test/resources/search.properties"));
		
		SearchSettings searchSettings = new SearchSettings();
		searchSettings.setProperties(properties);
		
		SearchStateFactory searchStateFactory = new SearchStateFactory();
		searchStateFactory.setSearchSettings(searchSettings);
		MockHttpServletRequest request = new MockHttpServletRequest(
				"GET",
				"https://localhost/search?action=setFacet%3apath%2c%222%2cuuid%3a52726582-2cea-455a-8220-c360dbe5082b%2c3%22|resetNav%3asearch&sort=default&sortOrder=&terms=anywhere:&rows=20");
		SearchState searchState = searchStateFactory.createSearchState(request.getParameterMap());
		SearchActionService sas = new SearchActionService();
		sas.setSearchSettings(searchSettings);
		FacetFieldFactory fff = new FacetFieldFactory();
		fff.setSearchSettings(searchSettings);
		sas.setFacetFieldFactory(fff);
		
		sas.executeActions(searchState, "setFacet:path,\"2,uuid:52726582-2cea-455a-8220-c360dbe5082b!3\"|resetNav:search");
		
		System.out.println(searchState);
		
		assertTrue(searchState.getFacets().containsKey("ANCESTOR_PATH"));
		CutoffFacet facet = (CutoffFacet)searchState.getFacets().get("ANCESTOR_PATH");
		assertEquals(3, facet.getCutoff().intValue());
	}
}
