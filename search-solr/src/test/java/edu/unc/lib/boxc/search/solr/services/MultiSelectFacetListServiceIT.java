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
package edu.unc.lib.boxc.search.solr.services;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.FacetFieldList;
import edu.unc.lib.boxc.search.api.facets.FacetFieldObject;
import edu.unc.lib.boxc.search.api.facets.SearchFacet;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.facets.CutoffFacetImpl;
import edu.unc.lib.boxc.search.solr.facets.FilterableDisplayValueFacet;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import edu.unc.lib.boxc.search.solr.facets.RoleGroupFacet;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.test.BaseEmbeddedSolrTest;
import edu.unc.lib.boxc.search.solr.test.TestCorpus;
import edu.unc.lib.boxc.search.solr.utils.AccessRestrictionUtil;
import edu.unc.lib.boxc.search.solr.utils.FacetFieldUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.FILE_FORMAT_CATEGORY;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.FILE_FORMAT_TYPE;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.PARENT_COLLECTION;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.ROLE_GROUP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author bbpennel
 */
public class MultiSelectFacetListServiceIT extends BaseEmbeddedSolrTest {

    private static final List<String> FACETS_TO_RETRIEVE = Arrays.asList(
            FILE_FORMAT_CATEGORY.name(), FILE_FORMAT_TYPE.name(), ROLE_GROUP.name(), PARENT_COLLECTION.name());
    private static final List<String> RESOURCE_TYPES_TO_RETRIEVE = Arrays.asList(
            ResourceType.AdminUnit.name(), ResourceType.Collection.name(),
            ResourceType.Folder.name(), ResourceType.Work.name());

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

        accessGroups = new AccessGroupSetImpl("unitOwner", PUBLIC_PRINC);
    }

    @Test
    public void noFacetFiltersTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, PARENT_COLLECTION, 2);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 1", testCorpus.coll1Pid.getId(), 3);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 2", testCorpus.coll2Pid.getId(), 3);

        assertNumberFacetsReturned(resp, FILE_FORMAT_CATEGORY, 2);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Text", 2);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Image", 2);

        assertNumberFacetsReturned(resp, FILE_FORMAT_TYPE, 4);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "text/plain", 2);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "application/pdf", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/jpeg", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/png", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 7);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 9);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 4);
    }

    @Test
    public void singleCollectionFacetTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.setFacet(new FilterableDisplayValueFacet(PARENT_COLLECTION.name(), testCorpus.coll1Pid.getId()));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, PARENT_COLLECTION, 2);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 1", testCorpus.coll1Pid.getId(), 3);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 2", testCorpus.coll2Pid.getId(), 3);

        assertNumberFacetsReturned(resp, FILE_FORMAT_CATEGORY, 2);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Text", 1);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Image", 1);

        assertNumberFacetsReturned(resp, FILE_FORMAT_TYPE, 3);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "text/plain", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "application/pdf", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/jpeg", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 3);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 3);
    }

    @Test
    public void multipleCollectionsFacetTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.addFacet(new FilterableDisplayValueFacet(PARENT_COLLECTION.name(), testCorpus.coll1Pid.getId()));
        searchState.addFacet(new FilterableDisplayValueFacet(PARENT_COLLECTION.name(), testCorpus.coll2Pid.getId()));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, PARENT_COLLECTION, 2);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 1", testCorpus.coll1Pid.getId(), 3);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 2", testCorpus.coll2Pid.getId(), 3);

        assertNumberFacetsReturned(resp, FILE_FORMAT_CATEGORY, 2);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Text", 2);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Image", 2);

        assertNumberFacetsReturned(resp, FILE_FORMAT_TYPE, 4);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "text/plain", 2);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "application/pdf", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/jpeg", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/png", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 4);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 6);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 3);
    }

    @Test
    public void singleFileFormatCategoryTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.addFacet(new GenericFacet(FILE_FORMAT_CATEGORY.name(), "Text"));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, PARENT_COLLECTION, 2);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 1", testCorpus.coll1Pid.getId(), 1);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 2", testCorpus.coll2Pid.getId(), 1);

        assertNumberFacetsReturned(resp, FILE_FORMAT_CATEGORY, 2);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Text", 2);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Image", 2);

        assertNumberFacetsReturned(resp, FILE_FORMAT_TYPE, 2);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "text/plain", 2);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "application/pdf", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 2);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 2);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 1);

        assertEquals(2, resp.getResultCount());
    }

    @Test
    public void singleFileFormatTypeTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.addFacet(new GenericFacet(FILE_FORMAT_TYPE.name(), "image/jpeg"));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, PARENT_COLLECTION, 1);
        assertFacetValueCount(resp, PARENT_COLLECTION, testCorpus.coll1Pid.getId(), 1);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 1", testCorpus.coll1Pid.getId(), 1);

        assertNumberFacetsReturned(resp, FILE_FORMAT_CATEGORY, 1);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Image", 1);

        assertNumberFacetsReturned(resp, FILE_FORMAT_TYPE, 4);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "text/plain", 2);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "application/pdf", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/jpeg", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/png", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 1);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 1);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 1);

        assertEquals(1, resp.getResultCount());
    }

    @Test
    public void fileFormatCategoryAndTypeTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.addFacet(new GenericFacet(FILE_FORMAT_CATEGORY.name(), "Image"));
        searchState.addFacet(new GenericFacet(FILE_FORMAT_TYPE.name(), "image/jpeg"));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, PARENT_COLLECTION, 1);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 1", testCorpus.coll1Pid.getId(), 1);

        assertNumberFacetsReturned(resp, FILE_FORMAT_CATEGORY, 1);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Image", 1);

        assertNumberFacetsReturned(resp, FILE_FORMAT_TYPE, 2);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/jpeg", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/png", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 1);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 1);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 1);

        assertEquals(1, resp.getResultCount());
    }

    @Test
    public void multipleFileFormatCategoriesSingleTypeTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.addFacet(new GenericFacet(FILE_FORMAT_CATEGORY.name(), "Image"));
        searchState.addFacet(new GenericFacet(FILE_FORMAT_CATEGORY.name(), "Text"));
        searchState.addFacet(new GenericFacet(FILE_FORMAT_TYPE.name(), "text/plain"));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, PARENT_COLLECTION, 2);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 1", testCorpus.coll1Pid.getId(), 1);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 2", testCorpus.coll2Pid.getId(), 1);

        assertNumberFacetsReturned(resp, FILE_FORMAT_CATEGORY, 1);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Text", 2);

        assertNumberFacetsReturned(resp, FILE_FORMAT_TYPE, 4);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "text/plain", 2);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "application/pdf", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 2);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 2);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 1);

        assertEquals(2, resp.getResultCount());
    }

    @Test
    public void multipleFileFormatTypesInSameCategoryTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.addFacet(new GenericFacet(FILE_FORMAT_TYPE.name(), "image/jpeg"));
        searchState.addFacet(new GenericFacet(FILE_FORMAT_TYPE.name(), "image/png"));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, PARENT_COLLECTION, 2);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 1", testCorpus.coll1Pid.getId(), 1);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 2", testCorpus.coll2Pid.getId(), 1);

        assertNumberFacetsReturned(resp, FILE_FORMAT_CATEGORY, 1);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Image", 2);

        assertNumberFacetsReturned(resp, FILE_FORMAT_TYPE, 4);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "text/plain", 2);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "application/pdf", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/jpeg", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/png", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 1);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 2);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 1);
    }

    @Test
    public void singleRoleGroupTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.setFacet(new RoleGroupFacet(ROLE_GROUP.name(), "canManage|manager"));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, PARENT_COLLECTION, 1);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 1", testCorpus.coll1Pid.getId(), 3);

        assertNumberFacetsReturned(resp, FILE_FORMAT_CATEGORY, 2);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Text", 1);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Image", 1);

        assertNumberFacetsReturned(resp, FILE_FORMAT_TYPE, 3);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "text/plain", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "application/pdf", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/jpeg", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 7);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 9);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 4);
    }

    @Test
    public void setContainerTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        request.setRootPid(testCorpus.folder1Pid);
        request.setApplyCutoffs(false);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, PARENT_COLLECTION, 1);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 1", testCorpus.coll1Pid.getId(), 2);

        assertNumberFacetsReturned(resp, FILE_FORMAT_CATEGORY, 2);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Text", 1);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Image", 1);

        assertNumberFacetsReturned(resp, FILE_FORMAT_TYPE, 3);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "text/plain", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "application/pdf", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/jpeg", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 2);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 2);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 2);
    }

    @Test
    public void setContainerAncestorPathCutoffUnitTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.setResourceTypes(RESOURCE_TYPES_TO_RETRIEVE);

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        searchState.addFacet(new CutoffFacetImpl(SearchFieldKey.ANCESTOR_PATH.name(), "2," + testCorpus.unitPid.getId() + "!3"));
        request.setApplyCutoffs(false);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, PARENT_COLLECTION, 2);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 1", testCorpus.coll1Pid.getId(), 3);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 2", testCorpus.coll2Pid.getId(), 3);

        assertNumberFacetsReturned(resp, FILE_FORMAT_CATEGORY, 2);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Text", 2);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Image", 2);

        assertNumberFacetsReturned(resp, FILE_FORMAT_TYPE, 4);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "text/plain", 2);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "application/pdf", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/jpeg", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/png", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 6);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 8);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 4);
    }

    @Test
    public void setContainerAndFileFormatTypeTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.addFacet(new GenericFacet(FILE_FORMAT_TYPE.name(), "text/plain"));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        request.setRootPid(testCorpus.folder1Pid);
        request.setApplyCutoffs(false);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, PARENT_COLLECTION, 1);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 1", testCorpus.coll1Pid.getId(), 1);

        assertNumberFacetsReturned(resp, FILE_FORMAT_CATEGORY, 1);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Text", 1);

        assertNumberFacetsReturned(resp, FILE_FORMAT_TYPE, 3);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "text/plain", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "application/pdf", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/jpeg", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 1);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 1);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 1);
    }

    @Test
    public void setParentCollectionAndFileFormatTypeTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.setFacet(new FilterableDisplayValueFacet(PARENT_COLLECTION.name(), testCorpus.coll1Pid.getId()));
        searchState.addFacet(new GenericFacet(FILE_FORMAT_TYPE.name(), "text/plain"));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        request.setApplyCutoffs(false);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, PARENT_COLLECTION, 2);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 1", testCorpus.coll1Pid.getId(), 1);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 2", testCorpus.coll2Pid.getId(), 1);

        assertNumberFacetsReturned(resp, FILE_FORMAT_CATEGORY, 1);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Text", 1);

        assertNumberFacetsReturned(resp, FILE_FORMAT_TYPE, 3);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "text/plain", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "application/pdf", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/jpeg", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 1);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 1);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 1);
    }

    @Test
    public void noMatchesParentCollectionTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);
        searchState.setFacet(new FilterableDisplayValueFacet(PARENT_COLLECTION.name(), "doesnotexist"));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, PARENT_COLLECTION, 2);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 1", testCorpus.coll1Pid.getId(), 3);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 2", testCorpus.coll2Pid.getId(), 3);

        assertNumberFacetsReturned(resp, FILE_FORMAT_CATEGORY, 0);
        assertNumberFacetsReturned(resp, FILE_FORMAT_TYPE, 0);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 0);
    }

    @Test
    public void limitedPatronAccessTest() throws Exception {
        accessGroups = new AccessGroupSetImpl(PUBLIC_PRINC);

        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        request.setApplyCutoffs(false);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, PARENT_COLLECTION, 2);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 1", testCorpus.coll1Pid.getId(), 3);
        assertFacetDisplayValueCount(resp, PARENT_COLLECTION, "Collection 2", testCorpus.coll2Pid.getId(), 1);

        assertNumberFacetsReturned(resp, FILE_FORMAT_CATEGORY, 2);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Text", 2);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Image", 1);

        assertNumberFacetsReturned(resp, FILE_FORMAT_TYPE, 3);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "text/plain", 2);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "application/pdf", 1);
        assertFacetValueCount(resp, FILE_FORMAT_TYPE, "image/jpeg", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 7);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 7);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 4);
    }

    @Test(expected = NotFoundException.class)
    public void noAccessTest() throws Exception {
        accessGroups = new AccessGroupSetImpl(PUBLIC_PRINC);

        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        request.setRootPid(testCorpus.privateFolderPid);
        request.setApplyCutoffs(false);
        service.getFacetListResult(request);
    }

    @Test
    public void filterByFacetNotBeingRetrievedTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(Arrays.asList(
                FILE_FORMAT_CATEGORY.name(), ROLE_GROUP.name()));
        searchState.setFacet(new FilterableDisplayValueFacet(PARENT_COLLECTION.name(), testCorpus.coll1Pid.getId()));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        request.setApplyCutoffs(false);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertFalse(resp.getFacetFields().hasFacet(PARENT_COLLECTION.name()));

        assertNumberFacetsReturned(resp, FILE_FORMAT_CATEGORY, 2);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Text", 1);
        assertFacetValueCount(resp, FILE_FORMAT_CATEGORY, "Image", 1);

        assertNumberFacetsReturned(resp, ROLE_GROUP, 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canViewOriginals|everyone", 3);
        assertFacetValueCount(resp, ROLE_GROUP, "unitOwner|unitOwner", 3);
        assertFacetValueCount(resp, ROLE_GROUP, "canManage|manager", 3);
    }

    @Test
    public void retainFacetOrderTest() throws Exception {
        SearchState searchState = new SearchState();
        List<String> facetsInOrder = Arrays.asList(PARENT_COLLECTION.name(), FILE_FORMAT_CATEGORY.name(), ROLE_GROUP.name());
        searchState.setFacetsToRetrieve(facetsInOrder);

        SearchRequest request1 = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp1 = service.getFacetListResult(request1);

        FacetFieldList facets1 = resp1.getFacetFields();
        List<String> names1 = facets1.stream().map(FacetFieldObject::getName).collect(Collectors.toList());
        assertEquals(facetsInOrder, names1);

        // Add filter to first facet, to ensure that it stays at the first first
        searchState.setFacet(new FilterableDisplayValueFacet(PARENT_COLLECTION.name(), testCorpus.coll1Pid.getId()));

        SearchRequest request2 = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp2 = service.getFacetListResult(request2);

        FacetFieldList facets2 = resp2.getFacetFields();
        List<String> names2 = facets2.stream().map(FacetFieldObject::getName).collect(Collectors.toList());
        assertEquals(facetsInOrder, names2);
    }

    private SearchFacet getFacetByValue(FacetFieldObject ffo, String value) {
        return ffo.getValues().stream().filter(f -> f.getSearchValue().equals(value)).findFirst().orElse(null);
    }

    private void assertFacetValueCount(SearchResultResponse resp, SearchFieldKey key, String value, int expectedCount) {
        FacetFieldObject ffo = resp.getFacetFields().get(key.name());
        SearchFacet facetValue = getFacetByValue(ffo, value);
        assertNotNull(facetValue);
        assertEquals(expectedCount, facetValue.getCount());
    }

    private void assertFacetDisplayValueCount(SearchResultResponse resp, SearchFieldKey key, String display,
                                              String value, int expectedCount) {
        FacetFieldObject ffo = resp.getFacetFields().get(key.name());
        SearchFacet facetValue = getFacetByValue(ffo, value);
        assertNotNull(facetValue);
        assertEquals(expectedCount, facetValue.getCount());
        assertEquals(display, facetValue.getDisplayValue());
    }

    private void assertNumberFacetsReturned(SearchResultResponse resp, SearchFieldKey key, int expectedCount) {
        FacetFieldObject ffo = resp.getFacetFields().get(key.name());
        assertEquals(expectedCount, ffo.getValues().size());
    }
}
