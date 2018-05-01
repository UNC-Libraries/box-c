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
import static edu.unc.lib.dl.test.TestHelpers.setField;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.fcrepo4.GlobalPermissionEvaluator;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.test.BaseEmbeddedSolrTest;
import edu.unc.lib.dl.search.solr.test.TestCorpus;
import edu.unc.lib.dl.search.solr.util.AccessRestrictionUtil;
import edu.unc.lib.dl.search.solr.util.FacetFieldUtil;
import edu.unc.lib.dl.util.ResourceType;

/**
 *
 * @author bbpennel
 */
public class NeighborQueryServiceIT extends BaseEmbeddedSolrTest {
    private static final Logger log = LoggerFactory.getLogger(NeighborQueryServiceIT.class);

    private static final String PRECEDING_PREFIX = "before";
    private static final String SUCCEEDING_PREFIX = "succeeding";

    private static final int NUM_DIGITS_SORT = 6;

    private static final int WINDOW_SIZE = 10;

    private PID targetPid;

    private AccessGroupSet groups;

    private TestCorpus testCorpus;

    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;

    private AccessRestrictionUtil restrictionUtil;

    protected FacetFieldUtil facetFieldUtil;

    private SolrSearchService solrSearchService;

    private NeighborQueryService queryService;

    public NeighborQueryServiceIT() {
        testCorpus = new TestCorpus();
    }

    @Override
    @Before
    public void setUp() throws Exception {
        initMocks(this);

        super.setUp();

        index(testCorpus.populate());

        targetPid = makePid();

        groups = new AccessGroupSet(PUBLIC_PRINC);

        restrictionUtil = new AccessRestrictionUtil();
        restrictionUtil.setDisablePermissionFiltering(true);
        restrictionUtil.setGlobalPermissionEvaluator(globalPermissionEvaluator);
        restrictionUtil.setSearchSettings(searchSettings);

        facetFieldUtil = new FacetFieldUtil();
        facetFieldUtil.setSearchSettings(searchSettings);
        facetFieldUtil.setSolrSettings(solrSettings);

        solrSearchService = new SolrSearchService();
        solrSearchService.setSolrSettings(solrSettings);
        solrSearchService.setAccessRestrictionUtil(restrictionUtil);
        setField(solrSearchService, "solrClient", server);

        queryService = new NeighborQueryService();
        queryService.setSolrSettings(solrSettings);
        queryService.setSearchSettings(searchSettings);
        queryService.setAccessRestrictionUtil(restrictionUtil);
        queryService.setFacetFieldUtil(facetFieldUtil);
        setField(queryService, "solrClient", server);
    }

    @Test
    public void testNeighborsMiddleList() throws Exception {
        populateNeighborhood(ResourceType.File, WINDOW_SIZE, WINDOW_SIZE, ResourceType.File);

        BriefObjectMetadataBean targetMd = getMetadata(targetPid);

        List<BriefObjectMetadataBean> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);

        assertEquals(WINDOW_SIZE, results.size());
        int indexOfTarget = indexOf(results, targetMd);
        assertEquals(WINDOW_SIZE / 2, indexOfTarget);
        assertPrecedingResults(results, indexOfTarget, WINDOW_SIZE / 2);
        assertSucceedingResults(results, indexOfTarget);
    }

    @Test
    public void testNeighborsStartList() throws Exception {
        populateNeighborhood(ResourceType.File, 0, WINDOW_SIZE, ResourceType.File);

        BriefObjectMetadataBean targetMd = getMetadata(targetPid);

        List<BriefObjectMetadataBean> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);

        assertEquals(WINDOW_SIZE, results.size());
        int indexOfTarget = indexOf(results, targetMd);
        assertEquals(0, indexOfTarget);
        assertSucceedingResults(results, indexOfTarget);
    }

    @Test
    public void testNeighborsEndList() throws Exception {
        populateNeighborhood(ResourceType.File, WINDOW_SIZE, 0, ResourceType.File);

        BriefObjectMetadataBean targetMd = getMetadata(targetPid);

        List<BriefObjectMetadataBean> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);

        assertEquals(WINDOW_SIZE, results.size());
        int indexOfTarget = indexOf(results, targetMd);
        assertEquals(WINDOW_SIZE - 1, indexOfTarget);
        assertPrecedingResults(results, indexOfTarget, 1);
    }

    @Test
    public void testNoNeighbors() throws Exception {
        populateNeighborhood(ResourceType.File, 0, 0, ResourceType.File);

        BriefObjectMetadataBean targetMd = getMetadata(targetPid);

        List<BriefObjectMetadataBean> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);

        assertEquals(1, results.size());
        int indexOfTarget = indexOf(results, targetMd);
        assertEquals(0, indexOfTarget);
    }

    @Test
    public void testFewNeighbors() throws Exception {
        populateNeighborhood(ResourceType.File, 1, 1, ResourceType.File);

        BriefObjectMetadataBean targetMd = getMetadata(targetPid);

        List<BriefObjectMetadataBean> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);

        assertEquals(3, results.size());
        int indexOfTarget = indexOf(results, targetMd);
        assertEquals(1, indexOfTarget);
        assertPrecedingResults(results, indexOfTarget, 0);
        assertSucceedingResults(results, indexOfTarget);
    }

    @Test
    public void testNeighborsNearEnd() throws Exception {
        populateNeighborhood(ResourceType.File, WINDOW_SIZE, 2, ResourceType.File);

        BriefObjectMetadataBean targetMd = getMetadata(targetPid);

        List<BriefObjectMetadataBean> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);

        assertEquals(WINDOW_SIZE, results.size());
        int indexOfTarget = indexOf(results, targetMd);
        assertEquals(WINDOW_SIZE - 3, indexOfTarget);
        assertPrecedingResults(results, indexOfTarget, WINDOW_SIZE / 2 - 2);
        assertSucceedingResults(results, indexOfTarget);
    }

    @Test
    public void testNeighborsBigTest() throws Exception {
        int numNeighbors = 500;
        populateNeighborhood(ResourceType.File, numNeighbors, numNeighbors, ResourceType.File);

        BriefObjectMetadataBean targetMd = getMetadata(targetPid);

        long start = System.nanoTime();
        List<BriefObjectMetadataBean> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);
        log.info("Executed neighbors query in {}", (System.nanoTime() - start));

        assertEquals(WINDOW_SIZE, results.size());
        int indexOfTarget = indexOf(results, targetMd);
        assertEquals(WINDOW_SIZE / 2, indexOfTarget);
        assertPrecedingResults(results, indexOfTarget, numNeighbors - WINDOW_SIZE / 2);
        assertSucceedingResults(results, indexOfTarget);
    }

    @Test
    public void testNeighborsSameTitle() throws Exception {
        List<SolrInputDocument> docs = new ArrayList<>();
        PID basePid = makePid();
        String baseUuid = basePid.getId();
        for (int i = 0; i < 3; i++) {
            PID pid = PIDs.get(baseUuid + i);
            addObject(docs, pid, "title", ResourceType.File,
                    testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);
        }

        PID tpid = PIDs.get(baseUuid + 3);
        addObject(docs, tpid, "title", ResourceType.File,
                testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);

        for (int i = 4; i < 7; i++) {
            PID pid = PIDs.get(baseUuid + i);
            addObject(docs, pid, "title", ResourceType.File,
                    testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);
        }

        server.add(docs);
        server.commit();

        BriefObjectMetadataBean targetMd = getMetadata(tpid);
        List<BriefObjectMetadataBean> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);

        assertEquals(7, results.size());
        int indexOfTarget = indexOf(results, targetMd);
        assertEquals(3, indexOfTarget);

        for (int i = 0; i < results.size(); i++) {
            BriefObjectMetadata result = results.get(i);
            assertEquals(baseUuid + i, result.getId());
        }
    }

    @Test
    public void testNeighborsNoTitle() throws Exception {
        List<SolrInputDocument> docs = new ArrayList<>();
        PID basePid = makePid();
        String baseUuid = basePid.getId();
        PID nPid0 = PIDs.get(baseUuid + "3");
        addObject(docs, nPid0, "title", ResourceType.File,
                testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);
        PID nPid1 = PIDs.get(baseUuid + "0");
        addObject(docs, nPid1, "", ResourceType.File,
                testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);

        PID tpid = PIDs.get(baseUuid + 1);
        addObject(docs, tpid, "", ResourceType.File,
                testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);

        PID nPid2 = PIDs.get(baseUuid + "2");
        addObject(docs, nPid2, "", ResourceType.File, testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);

        server.add(docs);
        server.commit();

        BriefObjectMetadataBean targetMd = getMetadata(tpid);
        List<BriefObjectMetadataBean> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);

        assertEquals(4, results.size());
        int indexOfTarget = indexOf(results, targetMd);
        assertEquals(1, indexOfTarget);

        for (int i = 0; i < results.size(); i++) {
            BriefObjectMetadata result = results.get(i);
            assertEquals(baseUuid + i, result.getId());
        }
    }

    private void populateNeighborhood(ResourceType targetType, int preceding, int succeeding,
            ResourceType neighborType) throws Exception {
        populateNeighborhood(targetType, preceding, succeeding, neighborType,
                testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);
    }

    private void populateNeighborhood(ResourceType targetType, int preceding, int succeeding,
            ResourceType neighborType, PID... ancestors) throws Exception {
        List<SolrInputDocument> docs = new ArrayList<>();

        addNeighbors(docs, preceding, PRECEDING_PREFIX, neighborType, ancestors);

        addObject(docs, targetPid, "middle", targetType, ancestors);

        addNeighbors(docs, succeeding, SUCCEEDING_PREFIX, neighborType, ancestors);

        server.add(docs);
        server.commit();
    }

    private void addNeighbors(List<SolrInputDocument> docs, int count, String prefix, ResourceType type,
            PID... ancestors) {
        for (int i = 0; i < count; i++) {
            PID neighborPid = makePid();
            addObject(docs, neighborPid, prefix + formatSortable(i), type, ancestors);
        }
    }

    private void addObject(List<SolrInputDocument> docs, PID pid, String title, ResourceType type,
            PID... ancestors) {
        SolrInputDocument doc;
        if (type.equals(ResourceType.File)) {
            doc = makeFileDocument(pid, title, ancestors);
        } else {
            doc = makeContainerDocument(pid, title, type, ancestors);
        }
        addAclProperties(doc, "public", "admin");
        docs.add(doc);
    }

    private SolrInputDocument makeContainerDocument(PID pid, String title, ResourceType type, PID... ancestors) {
        SolrInputDocument newDoc = new SolrInputDocument();
        newDoc.addField("title", title);
        newDoc.addField("id", pid.getId());
        newDoc.addField("rollup", pid.getId());
        newDoc.addField("ancestorIds", makeAncestorIds(pid, ancestors));
        newDoc.addField("ancestorPath", makeAncestorPath(ancestors));
        newDoc.addField("resourceType", type.name());
        return newDoc;
    }

    private SolrInputDocument makeFileDocument(PID pid, String title, PID... ancestors) {
        SolrInputDocument newDoc = new SolrInputDocument();
        newDoc.addField("title", title);
        newDoc.addField("id", pid.getId());
        newDoc.addField("rollup", ancestors[ancestors.length - 1].getId());
        newDoc.addField("ancestorIds", makeAncestorIds(null, ancestors));
        newDoc.addField("ancestorPath", makeAncestorPath(ancestors));
        newDoc.addField("resourceType", ResourceType.File.name());
        return newDoc;
    }

    private void addAclProperties(SolrInputDocument doc, String readGroup, String... adminGroups) {
        List<String> adminList = asList(adminGroups);

        List<String> roleGroups = new ArrayList<>();
        roleGroups.addAll(adminList);
        roleGroups.add(readGroup);

        doc.addField("roleGroup", roleGroups);
        doc.addField("readGroup", asList(readGroup));
        doc.addField("adminGroup", adminList);
    }

    private String makeAncestorIds(PID self, PID... pids) {
        String path = "";
        if (pids == null) {
            path = "";
        } else {
                for (PID pid : pids) {
                    if (path.length() > 0) {
                        path += "/";
                    }
                    path += pid.getUUID();
                }
        }
        if (self != null) {
            path += "/" + self.getId();
        }
        return path;
    }

    private List<String> makeAncestorPath(PID... pids) {
        List<String> result = new ArrayList<>();
        int i = 0;
        for (PID pid : pids) {
            i++;
            result.add(i + "," + pid.getId());
        }
        return result;
    }

    private PID makePid() {
            return PIDs.get(UUID.randomUUID().toString());
    }

    private int indexOf(List<BriefObjectMetadataBean> results, BriefObjectMetadata obj) {
        for (int i = 0; i < results.size(); i++) {
            BriefObjectMetadata result = results.get(i);
            if (result.getId().equals(obj.getId())) {
                return i;
            }
        }
        return -1;
    }

    private void assertPrecedingResults(List<BriefObjectMetadataBean> results, int end, int offset) {
        for (int i = 0; i < end; i++) {
            BriefObjectMetadata result = results.get(i);
            assertEquals(PRECEDING_PREFIX + formatSortable(i + offset), result.getTitle());
        }
    }

    private void assertSucceedingResults(List<BriefObjectMetadataBean> results, int start) {
        for (int i = start + 1; i < results.size(); i++) {
            BriefObjectMetadata result = results.get(i);
            assertEquals(SUCCEEDING_PREFIX + formatSortable(i - start - 1), result.getTitle());
        }
    }

    private BriefObjectMetadataBean getMetadata(PID pid) throws Exception {
        return solrSearchService.getObjectById(new SimpleIdRequest(pid, groups));
    }

    private String formatSortable(int number) {
        String formatted = Integer.toString(number);
        int numDigits = formatted.length();
        return StringUtils.repeat("0", NUM_DIGITS_SORT - numDigits) + formatted;
    }
}
