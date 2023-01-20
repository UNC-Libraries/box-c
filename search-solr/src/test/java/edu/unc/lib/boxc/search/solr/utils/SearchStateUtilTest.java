package edu.unc.lib.boxc.search.solr.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import edu.unc.lib.boxc.search.solr.utils.SearchStateUtil;

/**
 * @author bbpennel
 */
public class SearchStateUtilTest {

    @BeforeAll
    public static void setupClass() throws Exception {
        Properties searchProps = new Properties();
        searchProps.load(SearchStateUtilTest.class.getResourceAsStream("/search.properties"));
        SearchSettings searchSettings = new SearchSettings();
        searchSettings.setProperties(searchProps);

        SearchStateUtil searchStateUtil = new SearchStateUtil();
        searchStateUtil.setSearchSettings(searchSettings);
    }

    @Test
    public void generateSearchParametersEmptyState() throws Exception {
        SearchState searchState = new SearchState();

        Map<String, String> params = SearchStateUtil.generateSearchParameters(searchState);
        assertTrue(params.isEmpty());
    }

    @Test
    public void generateSearchParametersKeywordState() throws Exception {
        SearchState searchState = new SearchState();
        searchState.getSearchFields().put(SearchFieldKey.KEYWORD.name(), "test words");

        Map<String, String> params = SearchStateUtil.generateSearchParameters(searchState);
        assertEquals("test words", params.get("keyword"));
    }

    @Test
    public void generateSearchParametersSubjectFacet() throws Exception {
        SearchState searchState = new SearchState();
        searchState.addFacet(new GenericFacet(SearchFieldKey.SUBJECT, "subject value"));

        Map<String, String> params = SearchStateUtil.generateSearchParameters(searchState);
        assertEquals("subject value", params.get("subject"));
    }

    @Test
    public void generateSearchParametersMultipleSubjectFacet() throws Exception {
        SearchState searchState = new SearchState();
        searchState.addFacet(new GenericFacet(SearchFieldKey.SUBJECT, "subject1"));
        searchState.addFacet(new GenericFacet(SearchFieldKey.SUBJECT, "subject two"));
        searchState.addFacet(new GenericFacet(SearchFieldKey.SUBJECT, "last one"));

        Map<String, String> params = SearchStateUtil.generateSearchParameters(searchState);
        assertEquals("subject1||subject two||last one", params.get("subject"));
    }

    @Test
    public void generateSearchParametersMultipleRoleGroup() throws Exception {
        SearchState searchState = new SearchState();
        searchState.addFacet(new GenericFacet(SearchFieldKey.ROLE_GROUP, "canViewOriginals|everyone"));
        searchState.addFacet(new GenericFacet(SearchFieldKey.ROLE_GROUP, "canViewOriginals|authenticated"));

        Map<String, String> params = SearchStateUtil.generateSearchParameters(searchState);
        assertEquals("canViewOriginals|everyone||canViewOriginals|authenticated", params.get("role"));
    }

    @Test
    public void generateSearchParametersMultipleFacets() throws Exception {
        SearchState searchState = new SearchState();
        searchState.addFacet(new GenericFacet(SearchFieldKey.SUBJECT, "subject1"));
        searchState.addFacet(new GenericFacet(SearchFieldKey.SUBJECT, "subject two"));
        searchState.addFacet(new GenericFacet(SearchFieldKey.ROLE_GROUP, "canViewOriginals|everyone"));

        Map<String, String> params = SearchStateUtil.generateSearchParameters(searchState);
        assertEquals("canViewOriginals|everyone", params.get("role"));
        assertEquals("subject1||subject two", params.get("subject"));
    }

    @Test
    public void generateSearchParametersAncestorPath() throws Exception {
        SearchState searchState = new SearchState();
        searchState.addFacet(new GenericFacet(SearchFieldKey.ANCESTOR_PATH, "3,c31a3e50-8ee7-4381-a16d-0be47c9aeee4"));

        Map<String, String> params = SearchStateUtil.generateSearchParameters(searchState);
        assertTrue(params.isEmpty());
    }

    @Test
    public void generateSearchParameterString() {
        SearchState searchState = new SearchState();
        searchState.getSearchFields().put(SearchFieldKey.KEYWORD.name(), "test words");
        searchState.addFacet(new GenericFacet(SearchFieldKey.SUBJECT, "subject1"));
        searchState.addFacet(new GenericFacet(SearchFieldKey.SUBJECT, "subject two"));
        searchState.addFacet(new GenericFacet(SearchFieldKey.ROLE_GROUP, "canViewOriginals|everyone"));

        String paramString = SearchStateUtil.generateSearchParameterString(searchState);
        Map<String, String> params = Arrays.stream(paramString.split("&"))
                .collect(Collectors.toMap(k -> k.split("=")[0], v -> v.split("=")[1]));
        assertEquals(3, params.size());
        assertEquals("test words", params.get("keyword"));
        assertEquals("canViewOriginals|everyone", params.get("role"));
        assertEquals("subject1||subject two", params.get("subject"));
    }

    @Test
    public void generateSearchParameterStringEmptyState() {
        SearchState searchState = new SearchState();

        String paramString = SearchStateUtil.generateSearchParameterString(searchState);
        assertEquals(0, paramString.length());
    }
}
