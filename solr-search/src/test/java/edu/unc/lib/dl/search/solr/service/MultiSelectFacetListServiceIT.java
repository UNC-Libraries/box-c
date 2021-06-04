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

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.ANCESTOR_PATH;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.CONTENT_TYPE;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.RESOURCE_TYPE;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.ROLE_GROUP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.fcrepo4.GlobalPermissionEvaluator;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.FacetFieldFactory;
import edu.unc.lib.dl.search.solr.model.FacetFieldObject;
import edu.unc.lib.dl.search.solr.model.MultivaluedHierarchicalFacet;
import edu.unc.lib.dl.search.solr.model.RoleGroupFacet;
import edu.unc.lib.dl.search.solr.model.SearchFacet;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.test.BaseEmbeddedSolrTest;
import edu.unc.lib.dl.search.solr.test.TestCorpus;
import edu.unc.lib.dl.search.solr.util.AccessRestrictionUtil;
import edu.unc.lib.dl.search.solr.util.FacetFieldUtil;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;

/**
 * @author bbpennel
 */
public class MultiSelectFacetListServiceIT extends BaseEmbeddedSolrTest {

    private static final List<String> FACETS_TO_RETRIEVE = Arrays.asList(ANCESTOR_PATH.name(),
            CONTENT_TYPE.name(), ROLE_GROUP.name(), RESOURCE_TYPE.name());

    private TestCorpus testCorpus;
    private boolean corpusLoaded;
    private AccessGroupSet accessGroups;
    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;

    private FacetFieldFactory facetFieldFactory;
    private FacetFieldUtil facetFieldUtil;
    private AccessRestrictionUtil accessRestrictionUtil;
    private SolrSearchService searchService;
    private MultiSelectFacetListService service;

    // with container
    // with path and type
    // with container and type
    // no permissions
    // as patron

    public MultiSelectFacetListServiceIT() {
        testCorpus = new TestCorpus();
    }

    @Before
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

        service = new MultiSelectFacetListService();
        service.setSolrSettings(solrSettings);
        service.setSearchSettings(searchSettings);
        service.setSearchService(searchService);

        accessGroups = new AccessGroupSet("unitOwner", PUBLIC_PRINC);
    }

    @Test
    public void noFacetFiltersTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, ANCESTOR_PATH, 2);
        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll1Pid.getId(), 6);
        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll2Pid.getId(), 5);

        assertNumberFacetsReturned(resp, CONTENT_TYPE, 2);
        assertFacetValueCount(resp, CONTENT_TYPE, "^text", 3);
        assertFacetValueCount(resp, CONTENT_TYPE, "^image", 2);

        try {
            assertFacetValueCount(resp, SearchFieldKeys.RESOURCE_TYPE, "canViewOriginals|everyone", 14);
        } catch (Throwable e) {
        }

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 11);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 14);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 7);
    }

    @Test
    public void singlePathFacetTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.setFacet(new CutoffFacet(ANCESTOR_PATH.name(), "3," + testCorpus.coll1Pid.getId()));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, ANCESTOR_PATH, 2);
        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll1Pid.getId(), 6);
        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll2Pid.getId(), 5);

        assertNumberFacetsReturned(resp, CONTENT_TYPE, 2);
        assertFacetValueCount(resp, CONTENT_TYPE, "^text", 2);
        assertFacetValueCount(resp, CONTENT_TYPE, "^image", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 6);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 6);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 6);
    }

    @Test
    public void multiplePathFacetTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.addFacet(new CutoffFacet(ANCESTOR_PATH.name(), "3," + testCorpus.coll1Pid.getId()));
        searchState.addFacet(new CutoffFacet(ANCESTOR_PATH.name(), "4," + testCorpus.work3Pid.getId()));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, ANCESTOR_PATH, 2);
        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll1Pid.getId(), 6);
        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll2Pid.getId(), 5);

        assertNumberFacetsReturned(resp, CONTENT_TYPE, 2);
        assertFacetValueCount(resp, CONTENT_TYPE, "^text", 3);
        assertFacetValueCount(resp, CONTENT_TYPE, "^image", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 7);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 7);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 6);
    }

    @Test
    public void singleContentTypeFacetTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.setFacet(new MultivaluedHierarchicalFacet(CONTENT_TYPE.name(), "text"));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, ANCESTOR_PATH, 2);
        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll1Pid.getId(), 2);
        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll2Pid.getId(), 1);

        assertNumberFacetsReturned(resp, CONTENT_TYPE, 4);
        assertFacetValueCount(resp, CONTENT_TYPE, "^text", 3);
        assertFacetValueCount(resp, CONTENT_TYPE, "^image", 2);
        assertFacetValueCount(resp, CONTENT_TYPE, "/text^txt", 2);
        assertFacetValueCount(resp, CONTENT_TYPE, "/text^pdf", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 3);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 2);
    }

    @Test
    public void secondTierContentTypeFacetTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.setFacet(new MultivaluedHierarchicalFacet(CONTENT_TYPE.name(), "text/txt"));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, ANCESTOR_PATH, 2);
        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll1Pid.getId(), 1);
        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll2Pid.getId(), 1);

        assertNumberFacetsReturned(resp, CONTENT_TYPE, 4);
        assertFacetValueCount(resp, CONTENT_TYPE, "^text", 3);
        assertFacetValueCount(resp, CONTENT_TYPE, "^image", 2);
        assertFacetValueCount(resp, CONTENT_TYPE, "/text^txt", 2);
        assertFacetValueCount(resp, CONTENT_TYPE, "/text^pdf", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 2);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 2);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 1);
    }

    @Test
    public void multipleContentTypeFacetTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.addFacet(new MultivaluedHierarchicalFacet(CONTENT_TYPE.name(), "text/txt"));
        searchState.addFacet(new MultivaluedHierarchicalFacet(CONTENT_TYPE.name(), "image"));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, ANCESTOR_PATH, 2);
        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll1Pid.getId(), 2);
        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll2Pid.getId(), 2);

        assertNumberFacetsReturned(resp, CONTENT_TYPE, 6);
        assertFacetValueCount(resp, CONTENT_TYPE, "^text", 3);
        assertFacetValueCount(resp, CONTENT_TYPE, "^image", 2);
        assertFacetValueCount(resp, CONTENT_TYPE, "/text^txt", 2);
        assertFacetValueCount(resp, CONTENT_TYPE, "/text^pdf", 1);
        assertFacetValueCount(resp, CONTENT_TYPE, "/image^jpg", 1);
        assertFacetValueCount(resp, CONTENT_TYPE, "/image^png", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 3);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 4);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 2);
    }

    @Test
    public void multipleSameParentContentTypeFacetTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.addFacet(new MultivaluedHierarchicalFacet(CONTENT_TYPE.name(), "text/txt"));
        searchState.addFacet(new MultivaluedHierarchicalFacet(CONTENT_TYPE.name(), "text/pdf"));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, ANCESTOR_PATH, 2);
        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll1Pid.getId(), 2);
        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll2Pid.getId(), 1);

        assertFacetValueCount(resp, CONTENT_TYPE, "^text", 3);
        assertFacetValueCount(resp, CONTENT_TYPE, "^image", 2);
        assertFacetValueCount(resp, CONTENT_TYPE, "/text^txt", 2);
        assertFacetValueCount(resp, CONTENT_TYPE, "/text^pdf", 1);
        assertNumberFacetsReturned(resp, CONTENT_TYPE, 4);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 3);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 2);
    }

    @Test
    public void singleRoleGroupTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.setFacet(new RoleGroupFacet(ROLE_GROUP.name(), "canManage|manager"));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, ANCESTOR_PATH, 1);
        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll1Pid.getId(), 6);

        assertNumberFacetsReturned(resp, CONTENT_TYPE, 2);
        assertFacetValueCount(resp, CONTENT_TYPE, "^text", 2);
        assertFacetValueCount(resp, CONTENT_TYPE, "^image", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 11);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 14);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 7);
    }

    @Test
    public void setContainerTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        request.setRootPid(testCorpus.folder1Pid);
        SearchResultResponse resp = service.getFacetListResult(request);
o

        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll1Pid.getId(), 6);
        assertFacetValueCount(resp, ANCESTOR_PATH, "3," + testCorpus.coll2Pid.getId(), 5);
        assertNumberFacetsReturned(resp, ANCESTOR_PATH, 2);

        assertNumberFacetsReturned(resp, CONTENT_TYPE, 2);
        assertFacetValueCount(resp, CONTENT_TYPE, "^text", 2);
        assertFacetValueCount(resp, CONTENT_TYPE, "^image", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 6);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 6);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 6);
    }

    private SearchFacet getFacetByValue(FacetFieldObject ffo, String value) {
        System.out.print("----");
        return ffo.getValues().stream().peek(f -> System.out.println(f.getSearchValue() + " " + f.getCount())).filter(f -> f.getSearchValue().equals(value)).findFirst().orElse(null);
    }

    private void assertFacetValueCount(SearchResultResponse resp, SearchFieldKeys key, String value, int expectedCount) {
        FacetFieldObject ffo = resp.getFacetFields().get(key.name());
        SearchFacet facetValue = getFacetByValue(ffo, value);
        assertNotNull(facetValue);
        assertEquals(expectedCount, facetValue.getCount());
    }

    private void assertNumberFacetsReturned(SearchResultResponse resp, SearchFieldKeys key, int expectedCount) {
        FacetFieldObject ffo = resp.getFacetFields().get(key.name());
        assertEquals(expectedCount, ffo.getValues().size());
    }
}
