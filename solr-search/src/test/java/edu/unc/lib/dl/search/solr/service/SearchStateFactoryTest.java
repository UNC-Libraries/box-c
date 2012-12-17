package edu.unc.lib.dl.search.solr.service;

import java.io.FileInputStream;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

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
}
