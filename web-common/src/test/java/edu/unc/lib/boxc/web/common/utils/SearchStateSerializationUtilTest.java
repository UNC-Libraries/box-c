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
package edu.unc.lib.boxc.web.common.utils;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import edu.unc.lib.boxc.search.solr.facets.MultivaluedHierarchicalFacet;
import edu.unc.lib.boxc.search.solr.services.FacetFieldFactory;
import edu.unc.lib.boxc.search.solr.services.SearchStateFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static edu.unc.lib.boxc.web.common.utils.SearchStateSerializationUtil.FACET_DISPLAY_VALUE_KEY;
import static edu.unc.lib.boxc.web.common.utils.SearchStateSerializationUtil.FACET_VALUE_KEY;

/**
 * @author bbpennel
 */
public class SearchStateSerializationUtilTest {
    private SearchStateFactory searchStateFactory;

    @Before
    public void setup() throws Exception {
        var properties = new Properties();
        properties.load(new FileInputStream("src/test/resources/search.properties"));
        var searchSettings = new SearchSettings();
        searchSettings.setProperties(properties);

        var facetFieldFactory = new FacetFieldFactory();
        facetFieldFactory.setSearchSettings(searchSettings);

        searchStateFactory = new SearchStateFactory();
        searchStateFactory.setFacetFieldFactory(facetFieldFactory);
        searchStateFactory.setSearchSettings(searchSettings);
    }

    @Test
    public void getFilterParametersNoFilters() throws Exception {
        SearchState state = new SearchState();
        var result = SearchStateSerializationUtil.getFilterParameters(state);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getFilterParametersWithSearchTerms() throws Exception {
        var state = searchStateFactory.createSearchState(Map.of(
                SearchFieldKey.DEFAULT_INDEX.getUrlParam(), new String[] { "term one" },
                SearchFieldKey.TITLE_INDEX.getUrlParam(), new String[] { "title term" }
        ));
        var result = SearchStateSerializationUtil.getFilterParameters(state);
        assertHasSearchTerm(result, SearchFieldKey.DEFAULT_INDEX, "term one");
        assertHasSearchTerm(result, SearchFieldKey.TITLE_INDEX, "title term");
        assertEquals(2, result.size());
    }

    @Test
    public void getFilterParametersWithMultipleContentTypes() throws Exception {
        var state = searchStateFactory.createSearchState(Map.of(
                SearchFieldKey.FILE_FORMAT_CATEGORY.getUrlParam(), new String[] { "Image%7C%7CAudio" }
        ));
        var result = SearchStateSerializationUtil.getFilterParameters(state);
        assertEquals(1, result.size());
        var contentTypeValues = (List<Map<String, String>>) result.get(SearchFieldKey.FILE_FORMAT_CATEGORY.getUrlParam());
        assertHasFacetValue(result, SearchFieldKey.FILE_FORMAT_CATEGORY, "Image", "Image");
        assertHasFacetValue(result, SearchFieldKey.FILE_FORMAT_CATEGORY, "Audio", "Audio");
        assertEquals(2, contentTypeValues.size());
    }

    @Test
    public void getFilterParametersWithCollectionAndUnit() throws Exception {
        var state = searchStateFactory.createSearchState(Map.of(
                SearchFieldKey.PARENT_UNIT.getUrlParam(), new String[] { "bbe743e0-eca2-48f8-a9e1-66483fdf51e4" },
                SearchFieldKey.PARENT_COLLECTION.getUrlParam(), new String[] { "7bfda1f0-9f81-4a58-a656-f7b3523347af" }
        ));
        // Set some display values
        ((GenericFacet)state.getFacets().get(SearchFieldKey.PARENT_UNIT.name()).get(0)).setDisplayValue("Boxc Unit");
        ((GenericFacet)state.getFacets().get(SearchFieldKey.PARENT_COLLECTION.name()).get(0)).setDisplayValue("Coll");

        var result = SearchStateSerializationUtil.getFilterParameters(state);
        assertEquals(2, result.size());
        assertHasFacetValue(result, SearchFieldKey.PARENT_UNIT, "bbe743e0-eca2-48f8-a9e1-66483fdf51e4", "Boxc Unit");
        assertHasFacetValue(result, SearchFieldKey.PARENT_COLLECTION, "7bfda1f0-9f81-4a58-a656-f7b3523347af", "Coll");
    }

    @Test
    public void getFilterParametersWithAncestorPath() throws Exception {
        var state = searchStateFactory.createSearchState(Map.of(
                SearchFieldKey.ANCESTOR_PATH.getUrlParam(), new String[] { "2,bbe743e0-eca2-48f8-a9e1-66483fdf51e4" }
        ));

        var result = SearchStateSerializationUtil.getFilterParameters(state);
        assertEquals(1, result.size());
        assertHasFacetValue(result, SearchFieldKey.ANCESTOR_PATH, "2,bbe743e0-eca2-48f8-a9e1-66483fdf51e4",
                "bbe743e0-eca2-48f8-a9e1-66483fdf51e4");
    }

    @Test
    public void getFilterParametersWithDateCreatedRange() throws Exception {
        var state = searchStateFactory.createSearchState(Map.of(
                SearchFieldKey.DATE_CREATED_YEAR.getUrlParam(), new String[] { "1840,1845" }
        ));
        var result = SearchStateSerializationUtil.getFilterParameters(state);
        assertHasSearchTerm(result, SearchFieldKey.DATE_CREATED_YEAR, "1840,1845");
        assertEquals(1, result.size());
    }

    @Test
    public void getFilterParametersWithMixOfParams() throws Exception {
        var state = searchStateFactory.createSearchState(Map.of(
                SearchFieldKey.DEFAULT_INDEX.getUrlParam(), new String[] { "boxc" },
                SearchFieldKey.DATE_CREATED_YEAR.getUrlParam(), new String[] { "2010,2020" },
                SearchFieldKey.SUBJECT.getUrlParam(), new String[] { "Digital Repositories" },
                // these types of parameters should not appear in the filter parameters result
                SearchSettings.URL_PARAM_START_ROW, new String[] { "40" },
                SearchSettings.URL_PARAM_SORT_TYPE, new String[] { "title" }
        ));
        var result = SearchStateSerializationUtil.getFilterParameters(state);
        assertHasSearchTerm(result, SearchFieldKey.DEFAULT_INDEX, "boxc");
        assertHasSearchTerm(result, SearchFieldKey.DATE_CREATED_YEAR, "2010,2020");
        assertHasFacetValue(result, SearchFieldKey.SUBJECT, "Digital Repositories", "Digital Repositories");
        assertEquals(3, result.size());
    }

    private void assertHasSearchTerm(Map<String, Object> result, SearchFieldKey field, String value) {
        assertContainsField(result, field);
        var paramName = field.getUrlParam();
        assertEquals("Results field " + paramName + " had wrong value",
                value, result.get(paramName));
    }

    private void assertHasFacetValue(Map<String, Object> result, SearchFieldKey field,
                                     String value, String displayValue) {
        assertContainsField(result, field);
        var facetValues = (List<Map<String, String>>) result.get(field.getUrlParam());
        assertTrue("Results did not contain " + field.getUrlParam()
                        + " with value=" + value + ", display=" + displayValue + ": " + result,
                facetValues.stream().anyMatch(f -> value.equals(f.get(FACET_VALUE_KEY))
                        && displayValue.equals(f.get(FACET_DISPLAY_VALUE_KEY))));
    }

    private void assertContainsField(Map<String, Object> result, SearchFieldKey field) {
        var paramName = field.getUrlParam();
        assertTrue("Results did not contain field " + paramName + ", contained: " + result,
                result.containsKey(paramName));
    }
}
