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
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.getContentRootPid;
import static edu.unc.lib.dl.search.solr.service.ChildrenCountService.CHILD_COUNT;
import static edu.unc.lib.dl.search.solr.service.StructureQueryService.CONTAINERS_COUNT;
import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.SUBJECT;
import static edu.unc.lib.dl.test.TestHelpers.setField;
import static edu.unc.lib.dl.util.ResourceType.Folder;
import static edu.unc.lib.dl.util.ResourceType.Work;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.fcrepo4.GlobalPermissionEvaluator;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.FacetFieldFactory;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseRequest;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse.ResultNode;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.test.BaseEmbeddedSolrTest;
import edu.unc.lib.dl.search.solr.test.TestCorpus;
import edu.unc.lib.dl.search.solr.util.AccessRestrictionUtil;
import edu.unc.lib.dl.search.solr.util.FacetFieldUtil;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;

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

    @Before
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

        principals = new AccessGroupSet(PUBLIC_PRINC);

        rootId = getContentRootPid().getId();
    }

    @Test
    public void testExpandedStructureToContentRoot() throws Exception {
        HierarchicalBrowseRequest browseRequest = makeRequest(testCorpus.rootPid);

        HierarchicalBrowseResultResponse resp = structureService.getExpandedStructurePath(browseRequest);

        BriefObjectMetadata rootMd = resp.getRootNode().getMetadata();
        assertEquals("Root object must be the Collections object", rootId, rootMd.getId());

        assertCountEquals("Incorrect number of child containers", 5, rootMd, CONTAINERS_COUNT);
        assertEquals("Incorrect number of immediate children", 1, resp.getRootNode().getChildren().size());
    }

    @Test
    public void testExpandedStructurePathToCollection() throws Exception {
        HierarchicalBrowseRequest browseRequest = makeRequest(testCorpus.coll1Pid);

        HierarchicalBrowseResultResponse resp = structureService.getExpandedStructurePath(browseRequest);

        ResultNode rootNode = resp.getRootNode();
        assertEquals("Root object must be the Collections object", rootId, rootNode.getMetadata().getId());

        // Verify that the intermediate object between root and chosen is expanded
        ResultNode unitNode = rootNode.getChildren().get(0);
        BriefObjectMetadata unitMd = unitNode.getMetadata();
        assertEquals("Root must contain Unit object", testCorpus.unitPid.getId(), unitMd.getId());
        assertEquals("Incorrect number of immediate children of unit", 2, unitNode.getChildren().size());

        // Verify that the selected object is expanded
        ResultNode coll1Node = getChildByPid(unitNode, testCorpus.coll1Pid);
        BriefObjectMetadata coll1Md = coll1Node.getMetadata();
        assertCountEquals("Incorrect number of child objects", 3, coll1Md, CHILD_COUNT);
        assertCountEquals("Incorrect number of child containers", 1, coll1Md, CONTAINERS_COUNT);
        assertEquals("One immediate child expected", 1, coll1Node.getChildren().size());

        // Verify that its sibling is present but not expanded
        ResultNode coll2Node = getChildByPid(unitNode, testCorpus.coll2Pid);
        BriefObjectMetadata coll2Md = coll2Node.getMetadata();
        assertCountEquals("Incorrect number of child objects", 3, coll2Md, CHILD_COUNT);
        assertEquals("Unexpanded sibling should not return child records", 0, coll2Node.getChildren().size());

        System.out.println(resp.getResultList());
    }

    @Test
    public void getEmptyStructureTest() throws Exception {
        PID emptyPid = PIDs.get(UUID.randomUUID().toString());
        index(testCorpus.makeContainerDocument(emptyPid, "empty", Folder,
                testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid));

        HierarchicalBrowseRequest browseRequest = makeRequest(emptyPid);
        HierarchicalBrowseResultResponse resp = structureService.getExpandedStructurePath(browseRequest);

        ResultNode rootNode = resp.getRootNode();
        ResultNode unitNode = getChildByPid(rootNode, testCorpus.unitPid);
        ResultNode collNode = getChildByPid(unitNode, testCorpus.coll1Pid);
        ResultNode emptyNode = getChildByPid(collNode, emptyPid);

        assertEquals(0, emptyNode.getChildren().size());
        assertCountEquals("Incorrect number of child objects", 0, emptyNode.getMetadata(), CHILD_COUNT);
    }

    @Test
    public void testGetStructure() throws Exception {
        HierarchicalBrowseRequest browseRequest = makeRequest(testCorpus.coll2Pid);

        HierarchicalBrowseResultResponse resp =
                structureService.getHierarchicalBrowseResults(browseRequest);

        ResultNode rootNode = resp.getRootNode();
        BriefObjectMetadata coll2Md = rootNode.getMetadata();
        assertEquals("Root object must be coll2", testCorpus.coll2Pid.getId(), coll2Md.getId());

        assertCountEquals("Incorrect number of child objects", 3, coll2Md, CHILD_COUNT);
        assertCountEquals("Incorrect number of child containers", 1, coll2Md, CONTAINERS_COUNT);

        assertEquals("Only one child container should be present", 1, rootNode.getChildren().size());
    }

    @Test
    public void testGetStructureWithPermissionRestriction() throws Exception {
        // Enable permission filtering
        restrictionUtil.setDisablePermissionFiltering(false);

        HierarchicalBrowseRequest browseRequest = makeRequest(testCorpus.coll2Pid);

        HierarchicalBrowseResultResponse resp =
                structureService.getHierarchicalBrowseResults(browseRequest);

        ResultNode rootNode = resp.getRootNode();
        BriefObjectMetadata coll2Md = rootNode.getMetadata();
        assertEquals("Root object must be coll2", testCorpus.coll2Pid.getId(), coll2Md.getId());

        assertCountEquals("Incorrect number of child objects", 1, coll2Md, CHILD_COUNT);
        assertCountEquals("Incorrect number of child containers", 0, coll2Md, CONTAINERS_COUNT);

        assertEquals("No immediate children expected", 0, rootNode.getChildren().size());
    }

    @Test
    public void testGetStructureWithFilters() throws Exception {
        // Add a child to coll1 with a subject for filter matching
        PID filterPid = PIDs.get(UUID.randomUUID().toString());
        SolrInputDocument doc = testCorpus.makeContainerDocument(filterPid, "with subj", Folder,
                testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);
        doc.setField("subject", "computers");
        index(doc);

        // Add subject filter to search state
        SearchState searchState = new SearchState();
        Map<String, Object> facets = new HashMap<>();
        facets.put(SUBJECT.name(), "computers");
        searchState.setFacets(facets);

        HierarchicalBrowseRequest browseRequest = makeRequest(testCorpus.coll1Pid);
        browseRequest.setSearchState(searchState);

        HierarchicalBrowseResultResponse resp =
                structureService.getHierarchicalBrowseResults(browseRequest);

        ResultNode rootNode = resp.getRootNode();
        BriefObjectMetadata coll1Md = rootNode.getMetadata();
        assertEquals("Root object must be coll1", testCorpus.coll1Pid.getId(), coll1Md.getId());

        // Counts should only reflect what matches filters
        assertCountEquals("Incorrect number of child objects", 1, coll1Md, CHILD_COUNT);
        assertCountEquals("Incorrect number of child containers", 1, coll1Md, CONTAINERS_COUNT);

        assertEquals("Both immediate children should return", 2, rootNode.getChildren().size());
        assertNotNull("Folder with subject must be returned", getChildByPid(rootNode, filterPid));
    }

    @Test
    public void testGetStructureWithSearch() throws Exception {
        // Add a child to coll1 with a subject for filter matching
        PID filterPid = PIDs.get(UUID.randomUUID().toString());
        SolrInputDocument doc = testCorpus.makeContainerDocument(filterPid, "with subj", Folder,
                testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);
        doc.setField("title", "target item");
        index(doc);

        // Add title search to state
        SearchState searchState = new SearchState();
        searchState.getSearchFields().put(SearchFieldKeys.TITLE.name(), "\"target item\"");

        HierarchicalBrowseRequest browseRequest = makeRequest(testCorpus.coll1Pid);
        browseRequest.setSearchState(searchState);

        HierarchicalBrowseResultResponse resp =
                structureService.getHierarchicalBrowseResults(browseRequest);

        ResultNode rootNode = resp.getRootNode();
        BriefObjectMetadata coll1Md = rootNode.getMetadata();
        assertEquals("Root object must be coll1", testCorpus.coll1Pid.getId(), coll1Md.getId());

        // Counts should only reflect what matches filters
        assertCountEquals("Incorrect number of child objects", 1, coll1Md, CHILD_COUNT);
        assertCountEquals("Incorrect number of child containers", 1, coll1Md, CONTAINERS_COUNT);

        assertEquals("Only matching child should return", 1, rootNode.getChildren().size());
        assertNotNull("Folder with title must be returned", getChildByPid(rootNode, filterPid));
    }

    @Test
    public void testGetStructureIncludeFiles() throws Exception {
        HierarchicalBrowseRequest browseRequest = makeRequest(testCorpus.folder1Pid);
        browseRequest.setIncludeFiles(true);
        browseRequest.getSearchState().setRowsPerPage(10);

        HierarchicalBrowseResultResponse resp =
                structureService.getHierarchicalBrowseResults(browseRequest);

        ResultNode rootNode = resp.getRootNode();
        BriefObjectMetadata folder1Md = rootNode.getMetadata();
        assertEquals("Root object must be folder1", testCorpus.folder1Pid.getId(), folder1Md.getId());

        assertCountEquals("Incorrect number of child objects", 2, folder1Md, CHILD_COUNT);
        assertCountEquals("Incorrect number of child containers", 0, folder1Md, CONTAINERS_COUNT);

        assertEquals("2 child works should be present", 2, rootNode.getChildren().size());
        assertTrue("All children must be works", rootNode.getChildren().stream()
                .allMatch(c -> Work.name().equals(c.getMetadata().getResourceType())));
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

    private void assertCountEquals(String message, long count, BriefObjectMetadata md, String countType) {
        assertEquals(message, count, md.getCountMap().get(countType).intValue());
    }
}
