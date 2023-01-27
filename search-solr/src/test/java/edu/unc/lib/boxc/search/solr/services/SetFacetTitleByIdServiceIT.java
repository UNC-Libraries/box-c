package edu.unc.lib.boxc.search.solr.services;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.FILE_FORMAT_CATEGORY;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.PARENT_COLLECTION;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.PARENT_UNIT;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.SUBJECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Map;

import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.FacetFieldList;
import edu.unc.lib.boxc.search.api.facets.FacetFieldObject;
import edu.unc.lib.boxc.search.api.facets.SearchFacet;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.test.BaseEmbeddedSolrTest;
import edu.unc.lib.boxc.search.solr.test.TestCorpus;
import edu.unc.lib.boxc.search.solr.utils.AccessRestrictionUtil;
import edu.unc.lib.boxc.search.solr.utils.FacetFieldUtil;

/**
 * @author bbpennel
 */
public class SetFacetTitleByIdServiceIT extends BaseEmbeddedSolrTest {

    private TestCorpus testCorpus;
    private boolean corpusLoaded;
    private AccessGroupSet accessGroups;

    private ObjectPathFactory pathFactory;
    private SetFacetTitleByIdService titleService;

    private MultiSelectFacetListService facetListService;

    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;
    private FacetFieldFactory facetFieldFactory;
    private FacetFieldUtil facetFieldUtil;
    private AccessRestrictionUtil accessRestrictionUtil;
    private SolrSearchService searchService;
    private SearchStateFactory searchStateFactory;

    public SetFacetTitleByIdServiceIT() {
        testCorpus = new TestCorpus();
    }

    @BeforeEach
    public void init() throws Exception {
        initMocks(this);
        if (!corpusLoaded) {
            corpusLoaded = true;
            index(testCorpus.populate());
        }

        accessRestrictionUtil = new AccessRestrictionUtil();
        accessRestrictionUtil.setSearchSettings(searchSettings);
        accessRestrictionUtil.setGlobalPermissionEvaluator(globalPermissionEvaluator);

        facetFieldFactory = new FacetFieldFactory();
        facetFieldFactory.setSearchSettings(searchSettings);
        facetFieldFactory.setSolrSettings(solrSettings);

        facetFieldUtil = new FacetFieldUtil();
        facetFieldUtil.setSearchSettings(searchSettings);
        facetFieldUtil.setSolrSettings(solrSettings);

        searchService = new SolrSearchService();
        searchService.setSolrSettings(solrSettings);
        searchService.setSearchSettings(searchSettings);
        searchService.setFacetFieldUtil(facetFieldUtil);
        searchService.setAccessRestrictionUtil(accessRestrictionUtil);
        searchService.setFacetFieldFactory(facetFieldFactory);
        searchService.setSolrClient(server);

        facetListService = new MultiSelectFacetListService();
        facetListService.setSolrSettings(solrSettings);
        facetListService.setSearchSettings(searchSettings);
        facetListService.setSearchService(searchService);

        pathFactory = new ObjectPathFactory();
        pathFactory.setSolrSettings(solrSettings);
        pathFactory.setSearch(searchService);
        pathFactory.init();

        titleService = new SetFacetTitleByIdService();
        titleService.setPathFactory(pathFactory);

        accessGroups = new AccessGroupSetImpl("unitOwner", PUBLIC_PRINC);

        searchStateFactory = new SearchStateFactory();
        searchStateFactory.setFacetFieldFactory(facetFieldFactory);
        searchStateFactory.setSearchSettings(searchSettings);
    }

    @Test
    public void populateSearchStateCollectionAndUnitTest() throws Exception {
        var subject = "Digital Repositories";
        var state = searchStateFactory.createSearchState(Map.of(
                PARENT_UNIT.getUrlParam(), new String[] { testCorpus.unitPid.getId() },
                SearchFieldKey.PARENT_COLLECTION.getUrlParam(), new String[] { testCorpus.coll1Pid.getId() },
                SearchFieldKey.DEFAULT_INDEX.getUrlParam(), new String[] { "boxc" },
                SearchFieldKey.SUBJECT.getUrlParam(), new String[] { subject }
        ));

        assertHasSearchFacet(state, PARENT_COLLECTION, testCorpus.coll1Pid.getId(), "*");
        assertHasSearchFacet(state, PARENT_UNIT, testCorpus.unitPid.getId(), "*");
        assertHasSearchFacet(state, SUBJECT, subject, subject);

        titleService.populateSearchState(state);

        assertHasSearchFacet(state, PARENT_COLLECTION, testCorpus.coll1Pid.getId(), "Collection 1");
        assertHasSearchFacet(state, PARENT_UNIT, testCorpus.unitPid.getId(), "Unit");
        assertHasSearchFacet(state, SUBJECT, subject, subject);
        assertEquals(3, state.getFacets().size());
        assertEquals("boxc", state.getSearchFields().get(SearchFieldKey.DEFAULT_INDEX.name()));
        assertEquals(1, state.getSearchFields().size());
    }

    private void assertHasSearchFacet(SearchState state, SearchFieldKey field, String value, String displayValue) {
        var facetValues = state.getFacets().get(field.name());
        assertNotNull(facetValues, "Search state did not contain facet " + field.name());
        var facet = state.getFacets().get(field.name()).get(0);
        assertEquals(value, facet.getValue(), "Incorrect facet value for " + field.name());
        assertEquals(displayValue, facet.getDisplayValue(), "Incorrect display value for " + field.name());
    }

    private SearchFacet getFacetByValue(FacetFieldObject ffo, String value) {
        return ffo.getValues().stream().filter(f -> f.getSearchValue().equals(value)).findFirst().orElse(null);
    }

    private void assertFacetTitleEquals(FacetFieldObject ffo, SearchFieldKey expectedKey,
                                        PID pid, String expectedTitle) {
        SearchFacet facetValue = getFacetByValue(ffo, pid.getId());
        assertNotNull(facetValue);
        assertEquals(expectedTitle, facetValue.getDisplayValue());
        assertEquals(expectedKey.name(), facetValue.getFieldName());
        assertTrue(facetValue.getCount( )> 0);
    }
}
