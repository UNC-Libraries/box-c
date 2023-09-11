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
import edu.unc.lib.boxc.search.solr.ranges.RangePair;
import edu.unc.lib.boxc.search.solr.ranges.UnknownRange;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.test.BaseEmbeddedSolrTest;
import edu.unc.lib.boxc.search.solr.test.TestCorpus;
import edu.unc.lib.boxc.search.solr.utils.AccessRestrictionUtil;
import edu.unc.lib.boxc.search.solr.utils.FacetFieldUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.DATE_CREATED_YEAR;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.FILE_FORMAT_CATEGORY;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.FILE_FORMAT_TYPE;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.PARENT_COLLECTION;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.ROLE_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.MockitoAnnotations.openMocks;

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
    private AutoCloseable closeable;
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

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);
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

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
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

    @Test
    public void noAccessTest() throws Exception {
        Assertions.assertThrows(NotFoundException.class, () -> {
            accessGroups = new AccessGroupSetImpl(PUBLIC_PRINC);

            SearchState searchState = new SearchState();
            searchState.setFacetsToRetrieve(FACETS_TO_RETRIEVE);

            SearchRequest request = new SearchRequest(searchState, accessGroups);
            request.setRootPid(testCorpus.privateFolderPid);
            request.setApplyCutoffs(false);
            service.getFacetListResult(request);
        });
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

    @Test
    public void facetIncludeUnknownTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacetsToRetrieve(Arrays.asList(DATE_CREATED_YEAR.name()));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, DATE_CREATED_YEAR, 1);
        assertFacetValueCount(resp, DATE_CREATED_YEAR, UnknownRange.UNKNOWN_VALUE, 2);
    }

    @Test
    public void facetIncludeUnknownWithFiltersTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.getSearchFields().put(SearchFieldKey.DEFAULT_INDEX.name(), "folder");

        searchState.setFacetsToRetrieve(Arrays.asList(DATE_CREATED_YEAR.name()));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, DATE_CREATED_YEAR, 1);
        assertFacetValueCount(resp, DATE_CREATED_YEAR, UnknownRange.UNKNOWN_VALUE, 1);
    }

    @Test
    public void facetIncludeUnknownScopedToContainerIgnoreCutoffTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.addFacet(new CutoffFacetImpl(SearchFieldKey.ANCESTOR_PATH.name(), "2," + testCorpus.unitPid.getId() + "!3"));

        searchState.setFacetsToRetrieve(Arrays.asList(DATE_CREATED_YEAR.name()));

        SearchRequest request = new SearchRequest(searchState, accessGroups);
        SearchResultResponse resp = service.getFacetListResult(request);

        assertNumberFacetsReturned(resp, DATE_CREATED_YEAR, 1);
        assertFacetValueCount(resp, DATE_CREATED_YEAR, UnknownRange.UNKNOWN_VALUE, 1);
    }

    @Test
    public void getMinimumDateCreatedYearBlankSearchTest() throws Exception {
        SearchState searchState = new SearchState();
        SearchRequest request = new SearchRequest(searchState, accessGroups);
        String result = service.getMinimumDateCreatedYear(searchState, request);
        assertEquals("2017", result);
    }

    @Test
    public void getMinimumDateCreatedYearScopedTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setFacet(new CutoffFacetImpl(
                SearchFieldKey.ANCESTOR_PATH.name(), "4," + testCorpus.folder1Pid.getId()));
        SearchRequest request = new SearchRequest(searchState, accessGroups);
        String result = service.getMinimumDateCreatedYear(searchState, request);
        assertEquals("2018", result);
    }

    @Test
    public void getMinimumDateCreatedYearNoValuesTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setResourceTypes(Arrays.asList(ResourceType.AdminUnit.name()));
        SearchRequest request = new SearchRequest(searchState, accessGroups);
        String result = service.getMinimumDateCreatedYear(searchState, request);
        assertNull(result);
    }

    @Test
    public void getMinimumDateCreatedYearNoResultsTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.getSearchFields().put(SearchFieldKey.DEFAULT_INDEX.name(), "notgoingtofindthisnoway");
        SearchRequest request = new SearchRequest(searchState, accessGroups);
        String result = service.getMinimumDateCreatedYear(searchState, request);
        assertNull(result);
    }

    @Test
    public void getMinimumDateCreatedYearDateSelectTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.getRangeFields().put(DATE_CREATED_YEAR.name(), new RangePair("2019", "2022"));
        SearchRequest request = new SearchRequest(searchState, accessGroups);
        String result = service.getMinimumDateCreatedYear(searchState, request);
        assertEquals("2019", result);
    }

    @Test
    public void getMinimumDateCreatedYearUnknownSelectedTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.getRangeFields().put(DATE_CREATED_YEAR.name(), new UnknownRange());
        SearchRequest request = new SearchRequest(searchState, accessGroups);
        String result = service.getMinimumDateCreatedYear(searchState, request);
        assertNull(result);
    }

    @Test
    public void getMinimumDateCreatedYearWithRollupEnabledTest() throws Exception {
        SearchState searchState = new SearchState();
        searchState.setRollup(true);
        SearchRequest request = new SearchRequest(searchState, accessGroups);
        String result = service.getMinimumDateCreatedYear(searchState, request);
        assertEquals("2017", result);
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
