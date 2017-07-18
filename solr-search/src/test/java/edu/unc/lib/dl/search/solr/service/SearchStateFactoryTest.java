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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.FacetFieldFactory;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
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
        Map<String, String[]> parameters = new LinkedHashMap<String, String[]>();
        parameters.put("action", new String[]{"setFacet%3apath%2c%221%2cuuid%3ac34ae354-8626-48c6-9963-d907aa65a713%22"});
        SearchState searchState = searchStateFactory.createSearchState(parameters);

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
        Map<String, String[]> parameters = new LinkedHashMap<String, String[]>();
        parameters.put("a.setFacet", new String[]{"path:2,uuid%3a52726582-2cea-455a-8220-c360dbe5082b!3"});
        parameters.put("a.resetNav", new String[]{"search"});
        parameters.put("anywhere", new String[]{"query"});
        parameters.put("rows", new String[]{"20"});
        SearchState searchState = searchStateFactory.createSearchState(parameters);
        SearchActionService sas = new SearchActionService();
        sas.setSearchSettings(searchSettings);
        FacetFieldFactory fff = new FacetFieldFactory();
        fff.setSearchSettings(searchSettings);
        sas.setFacetFieldFactory(fff);

        sas.executeActions(searchState, parameters);

        assertTrue(searchState.getFacets().containsKey("ANCESTOR_PATH"));
        CutoffFacet facet = (CutoffFacet)searchState.getFacets().get("ANCESTOR_PATH");
        assertEquals(3, facet.getCutoff().intValue());
    }

    @Test
    public void extractFacets() throws Exception {
        Properties properties = new Properties();
        properties.load(new FileInputStream("src/test/resources/search.properties"));

        SearchSettings searchSettings = new SearchSettings();
        searchSettings.setProperties(properties);

        SearchStateFactory searchStateFactory = new SearchStateFactory();
        searchStateFactory.setSearchSettings(searchSettings);
        Map<String, String[]> parameters = new LinkedHashMap<String, String[]>();
        parameters.put("path", new String[]{"2,uuid:52726582-2cea-455a-8220-c360dbe5082b!3"});
        parameters.put("anywhere", new String[]{"query"});
        parameters.put("start", new String[]{"55"});
        FacetFieldFactory fff = new FacetFieldFactory();
        fff.setSearchSettings(searchSettings);
        searchStateFactory.setFacetFieldFactory(fff);

        SearchState searchState = searchStateFactory.createSearchState(parameters);

        assertTrue(searchState.getFacets().containsKey("ANCESTOR_PATH"));
        CutoffFacet facet = (CutoffFacet)searchState.getFacets().get("ANCESTOR_PATH");
        assertEquals(3, facet.getCutoff().intValue());
        assertTrue(searchState.getSearchFields().containsKey(SearchFieldKeys.DEFAULT_INDEX.name()));
        assertEquals(55, searchState.getStartRow().intValue());
    }
}
