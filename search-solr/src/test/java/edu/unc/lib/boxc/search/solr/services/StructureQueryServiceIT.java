package edu.unc.lib.boxc.search.solr.services;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static edu.unc.lib.boxc.model.api.ResourceType.Folder;
import static edu.unc.lib.boxc.model.api.ResourceType.Work;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.SUBJECT;
import static edu.unc.lib.boxc.search.solr.services.ChildrenCountService.CHILD_COUNT;
import static edu.unc.lib.boxc.search.solr.services.StructureQueryService.CONTAINERS_COUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.HierarchicalBrowseRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import edu.unc.lib.boxc.search.solr.responses.HierarchicalBrowseResultResponse;
import edu.unc.lib.boxc.search.solr.responses.HierarchicalBrowseResultResponse.ResultNode;
import edu.unc.lib.boxc.search.solr.services.ChildrenCountService;
import edu.unc.lib.boxc.search.solr.services.FacetFieldFactory;
import edu.unc.lib.boxc.search.solr.services.SearchStateFactory;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import edu.unc.lib.boxc.search.solr.services.StructureQueryService;
import edu.unc.lib.boxc.search.solr.test.BaseEmbeddedSolrTest;
import edu.unc.lib.boxc.search.solr.test.TestCorpus;
import edu.unc.lib.boxc.search.solr.utils.AccessRestrictionUtil;
import edu.unc.lib.boxc.search.solr.utils.FacetFieldUtil;

/**
 *
 * @author bbpennel
 *
 */
public class StructureQueryServiceIT extends BaseEmbeddedSolrTest {

    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;

    private String rootId;

    private AccessGroupSet principals;
    private AccessRestrictionUtil restrictionUtil;
    private TestCorpus testCorpus;

    private FacetFieldFactory facetFieldFactory;
    private FacetFieldUtil facetFieldUtil;
    private SearchStateFactory searchStateFactory;

    private SolrSearchService solrSearchService;

    private ChildrenCountService countService;

    private StructureQueryService structureService;

    public StructureQueryServiceIT() {
        testCorpus = new TestCorpus();
    }

    @BeforeEach
    public void init() throws Exception {
        initMocks(this);

        index(testCorpus.populate());

        restrictionUtil = new AccessRestrictionUtil();
        restrictionUtil.setDisablePermissionFiltering(true);
        restrictionUtil.setGlobalPermissionEvaluator(globalPermissionEvaluator);
        restrictionUtil.setSearchSettings(searchSettings);

        when(globalPermissionEvaluator.hasGlobalPrincipal(anySetOf(String.class))).thenReturn(false);

        facetFieldFactory = new FacetFieldFactory();
        facetFieldFactory.setSearchSettings(searchSettings);
        facetFieldFactory.setSolrSettings(solrSettings);

        facetFieldUtil = new FacetFieldUtil();
        facetFieldUtil.setSearchSettings(searchSettings);
        facetFieldUtil.setSolrSettings(solrSettings);
        setField(facetFieldUtil, "facetFieldFactory", facetFieldFactory);

        searchStateFactory = new SearchStateFactory();
        searchStateFactory.setFacetFieldFactory(facetFieldFactory);
        searchStateFactory.setSearchSettings(searchSettings);

        solrSearchService = new SolrSearchService();
        solrSearchService.setSolrSettings(solrSettings);
        solrSearchService.setSearchSettings(searchSettings);
        solrSearchService.setAccessRestrictionUtil(restrictionUtil);
        solrSearchService.setFacetFieldFactory(facetFieldFactory);
        solrSearchService.setFacetFieldUtil(facetFieldUtil);
        setField(solrSearchService, "solrClient", server);

        countService = new ChildrenCountService();
        countService.setSolrSettings(solrSettings);
        countService.setAccessRestrictionUtil(restrictionUtil);
        setField(countService, "solrClient", server);

        structureService = new StructureQueryService();
        structureService.setSolrSettings(solrSettings);
        structureService.setSearchSettings(searchSettings);
        structureService.setAccessRestrictionUtil(restrictionUtil);
        structureService.setSearchService(solrSearchService);
        structureService.setChildrenCountService(countService);
        structureService.setSearchStateFactory(searchStateFactory);
        setField(structureService, "solrClient", server);

        principals = new AccessGroupSetImpl(PUBLIC_PRINC);

        rootId = getContentRootPid().getId();
    }

    @Test
    public void testExpandedStructureToContentRoot() throws Exception {
        HierarchicalBrowseRequest browseRequest = makeRequest(testCorpus.rootPid);

        HierarchicalBrowseResultResponse resp = structureService.getExpandedStructurePath(browseRequest);

        ContentObjectRecord rootMd = resp.getRootNode().getMetadata();
        assertEquals(rootId, rootMd.getId(), "Root object must be the Collections object");

        assertCountEquals(5, rootMd, CONTAINERS_COUNT, "Incorrect number of child containers");
        assertEquals(1, resp.getRootNode().getChildren().size(), "Incorrect number of immediate children");
    }

    @Test
    public void testExpandedStructurePathToCollection() throws Exception {
        HierarchicalBrowseRequest browseRequest = makeRequest(testCorpus.coll1Pid);

        HierarchicalBrowseResultResponse resp = structureService.getExpandedStructurePath(browseRequest);

        ResultNode rootNode = resp.getRootNode();
        assertEquals(rootId, rootNode.getMetadata().getId(), "Root object must be the Collections object");

        // Verify that the intermediate object between root and chosen is expanded
        ResultNode unitNode = rootNode.getChildren().get(0);
        ContentObjectRecord unitMd = unitNode.getMetadata();
        assertEquals(testCorpus.unitPid.getId(), unitMd.getId(), "Root must contain Unit object");
        assertEquals(2, unitNode.getChildren().size(), "Incorrect number of immediate children of unit");

        // Verify that the selected object is expanded
        ResultNode coll1Node = getChildByPid(unitNode, testCorpus.coll1Pid);
        ContentObjectRecord coll1Md = coll1Node.getMetadata();
        assertCountEquals(3, coll1Md, CHILD_COUNT, "Incorrect number of child objects");
        assertCountEquals(1, coll1Md, CONTAINERS_COUNT, "Incorrect number of child containers");
        assertEquals(1, coll1Node.getChildren().size(), "One immediate child expected");

        // Verify that its sibling is present but not expanded
        ResultNode coll2Node = getChildByPid(unitNode, testCorpus.coll2Pid);
        ContentObjectRecord coll2Md = coll2Node.getMetadata();
        assertCountEquals(3, coll2Md, CHILD_COUNT, "Incorrect number of child objects");
        assertEquals(0, coll2Node.getChildren().size(),
                "Unexpanded sibling should not return child records");

        System.out.println(resp.getResultList());
    }

    @Test
    public void getEmptyStructureTest() throws Exception {
        PID emptyPid = PIDs.get(UUID.randomUUID().toString());
        index(testCorpus.makeContainerDocument(emptyPid, "empty", Folder,
                "2017-01-01", testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid));

        HierarchicalBrowseRequest browseRequest = makeRequest(emptyPid);
        HierarchicalBrowseResultResponse resp = structureService.getExpandedStructurePath(browseRequest);

        ResultNode rootNode = resp.getRootNode();
        ResultNode unitNode = getChildByPid(rootNode, testCorpus.unitPid);
        ResultNode collNode = getChildByPid(unitNode, testCorpus.coll1Pid);
        ResultNode emptyNode = getChildByPid(collNode, emptyPid);

        assertEquals(0, emptyNode.getChildren().size());
        assertCountEquals(0, emptyNode.getMetadata(), CHILD_COUNT, "Incorrect number of child objects");
    }

    @Test
    public void testGetStructure() throws Exception {
        HierarchicalBrowseRequest browseRequest = makeRequest(testCorpus.coll2Pid);

        HierarchicalBrowseResultResponse resp =
                structureService.getHierarchicalBrowseResults(browseRequest);

        ResultNode rootNode = resp.getRootNode();
        ContentObjectRecord coll2Md = rootNode.getMetadata();
        assertEquals(testCorpus.coll2Pid.getId(), coll2Md.getId(), "Root object must be coll2");

        assertCountEquals(3, coll2Md, CHILD_COUNT, "Incorrect number of child objects");
        assertCountEquals(1, coll2Md, CONTAINERS_COUNT, "Incorrect number of child containers");

        assertEquals(1, rootNode.getChildren().size(), "Only one child container should be present");
    }

    @Test
    public void testGetStructureWithPermissionRestriction() throws Exception {
        // Enable permission filtering
        restrictionUtil.setDisablePermissionFiltering(false);

        HierarchicalBrowseRequest browseRequest = makeRequest(testCorpus.coll2Pid);

        HierarchicalBrowseResultResponse resp =
                structureService.getHierarchicalBrowseResults(browseRequest);

        ResultNode rootNode = resp.getRootNode();
        ContentObjectRecord coll2Md = rootNode.getMetadata();
        assertEquals(testCorpus.coll2Pid.getId(), coll2Md.getId(), "Root object must be coll2");

        assertCountEquals(1, coll2Md, CHILD_COUNT, "Incorrect number of child objects");
        assertCountEquals(0, coll2Md, CONTAINERS_COUNT, "Incorrect number of child containers");

        assertEquals(0, rootNode.getChildren().size(), "No immediate children expected");
    }

    @Test
    public void testGetStructureWithFilters() throws Exception {
        // Add a child to coll1 with a subject for filter matching
        PID filterPid = PIDs.get(UUID.randomUUID().toString());
        SolrInputDocument doc = testCorpus.makeContainerDocument(filterPid, "with subj", Folder,
                "2017-01-01", testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);
        doc.setField("subject", "computers");
        index(doc);

        // Add subject filter to search state
        SearchState searchState = new SearchState();
        searchState.setFacet(new GenericFacet(SUBJECT.name(), "computers"));

        HierarchicalBrowseRequest browseRequest = makeRequest(testCorpus.coll1Pid);
        browseRequest.setSearchState(searchState);

        HierarchicalBrowseResultResponse resp =
                structureService.getHierarchicalBrowseResults(browseRequest);

        ResultNode rootNode = resp.getRootNode();
        ContentObjectRecord coll1Md = rootNode.getMetadata();
        assertEquals(testCorpus.coll1Pid.getId(), coll1Md.getId(), "Root object must be coll1");

        // Counts should only reflect what matches filters
        assertCountEquals(1, coll1Md, CHILD_COUNT, "Incorrect number of child objects");
        assertCountEquals(1, coll1Md, CONTAINERS_COUNT, "Incorrect number of child containers");

        assertEquals(2, rootNode.getChildren().size(), "Both immediate children should return");
        assertNotNull(getChildByPid(rootNode, filterPid), "Folder with subject must be returned");
    }

    @Test
    public void testGetStructureWithSearch() throws Exception {
        // Add a child to coll1 with a subject for filter matching
        PID filterPid = PIDs.get(UUID.randomUUID().toString());
        SolrInputDocument doc = testCorpus.makeContainerDocument(filterPid, "with subj", Folder,
                "2017-01-01", testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);
        doc.setField("title", "target item");
        index(doc);

        // Add title search to state
        SearchState searchState = new SearchState();
        searchState.getSearchFields().put(SearchFieldKey.TITLE.name(), "\"target item\"");

        HierarchicalBrowseRequest browseRequest = makeRequest(testCorpus.coll1Pid);
        browseRequest.setSearchState(searchState);

        HierarchicalBrowseResultResponse resp =
                structureService.getHierarchicalBrowseResults(browseRequest);

        ResultNode rootNode = resp.getRootNode();
        ContentObjectRecord coll1Md = rootNode.getMetadata();
        assertEquals(testCorpus.coll1Pid.getId(), coll1Md.getId(), "Root object must be coll1");

        // Counts should only reflect what matches filters
        assertCountEquals(1, coll1Md, CHILD_COUNT, "Incorrect number of child objects");
        assertCountEquals(1, coll1Md, CONTAINERS_COUNT, "Incorrect number of child containers");

        assertEquals(1, rootNode.getChildren().size(), "Only matching child should return");
        assertNotNull(getChildByPid(rootNode, filterPid), "Folder with title must be returned");
    }

    @Test
    public void testGetStructureIncludeFiles() throws Exception {
        HierarchicalBrowseRequest browseRequest = makeRequest(testCorpus.folder1Pid);
        browseRequest.setIncludeFiles(true);
        browseRequest.getSearchState().setRowsPerPage(10);

        HierarchicalBrowseResultResponse resp =
                structureService.getHierarchicalBrowseResults(browseRequest);

        ResultNode rootNode = resp.getRootNode();
        ContentObjectRecord folder1Md = rootNode.getMetadata();
        assertEquals(testCorpus.folder1Pid.getId(), folder1Md.getId(), "Root object must be folder1");

        assertCountEquals(2, folder1Md, CHILD_COUNT, "Incorrect number of child objects");
        assertCountEquals(0, folder1Md, CONTAINERS_COUNT, "Incorrect number of child containers");

        assertEquals(2, rootNode.getChildren().size(), "2 child works should be present");
        assertTrue(rootNode.getChildren().stream().allMatch(c -> Work.name().equals(c.getMetadata().getResourceType())),
                "All children must be works");
    }

    private HierarchicalBrowseRequest makeRequest(PID rootPid) {
        HierarchicalBrowseRequest browseRequest = new HierarchicalBrowseRequest(1, principals);
        browseRequest.setSearchState(new SearchState());
        browseRequest.setRootPid(rootPid);

        return browseRequest;
    }

    private ResultNode getChildByPid(ResultNode node, PID pid) {
        try {
            return node.getChildren().stream()
                .filter(c -> c.getMetadata().getPid().equals(pid))
                .findFirst().get();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private void assertCountEquals(long count, ContentObjectRecord md, String countType, String message) {
        assertEquals(count, md.getCountMap().get(countType).intValue(), message);
    }
}
