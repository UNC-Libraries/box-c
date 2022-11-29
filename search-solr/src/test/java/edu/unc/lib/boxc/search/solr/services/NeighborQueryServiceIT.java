package edu.unc.lib.boxc.search.solr.services;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.services.NeighborQueryService;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import edu.unc.lib.boxc.search.solr.test.BaseEmbeddedSolrTest;
import edu.unc.lib.boxc.search.solr.test.TestCorpus;
import edu.unc.lib.boxc.search.solr.utils.AccessRestrictionUtil;
import edu.unc.lib.boxc.search.solr.utils.FacetFieldUtil;

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

        groups = new AccessGroupSetImpl(PUBLIC_PRINC);

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

        ContentObjectRecord targetMd = getMetadata(targetPid);

        List<ContentObjectRecord> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);

        assertEquals(WINDOW_SIZE, results.size());
        int indexOfTarget = indexOf(results, targetMd);
        assertEquals(WINDOW_SIZE / 2, indexOfTarget);
        assertPrecedingResults(results, indexOfTarget, WINDOW_SIZE / 2);
        assertSucceedingResults(results, indexOfTarget);
    }

    @Test
    public void testNeighborsStartList() throws Exception {
        populateNeighborhood(ResourceType.File, 0, WINDOW_SIZE, ResourceType.File);

        ContentObjectRecord targetMd = getMetadata(targetPid);

        List<ContentObjectRecord> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);

        assertEquals(WINDOW_SIZE, results.size());
        int indexOfTarget = indexOf(results, targetMd);
        assertEquals(0, indexOfTarget);
        assertSucceedingResults(results, indexOfTarget);
    }

    @Test
    public void testNeighborsEndList() throws Exception {
        populateNeighborhood(ResourceType.File, WINDOW_SIZE, 0, ResourceType.File);

        ContentObjectRecord targetMd = getMetadata(targetPid);

        List<ContentObjectRecord> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);

        assertEquals(WINDOW_SIZE, results.size());
        int indexOfTarget = indexOf(results, targetMd);
        assertEquals(WINDOW_SIZE - 1, indexOfTarget);
        assertPrecedingResults(results, indexOfTarget, 1);
    }

    @Test
    public void testNoNeighbors() throws Exception {
        populateNeighborhood(ResourceType.File, 0, 0, ResourceType.File);

        ContentObjectRecord targetMd = getMetadata(targetPid);

        List<ContentObjectRecord> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);

        assertEquals(1, results.size());
        int indexOfTarget = indexOf(results, targetMd);
        assertEquals(0, indexOfTarget);
    }

    @Test
    public void testFewNeighbors() throws Exception {
        populateNeighborhood(ResourceType.File, 1, 1, ResourceType.File);

        ContentObjectRecord targetMd = getMetadata(targetPid);

        List<ContentObjectRecord> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);

        assertEquals(3, results.size());
        int indexOfTarget = indexOf(results, targetMd);
        assertEquals(1, indexOfTarget);
        assertPrecedingResults(results, indexOfTarget, 0);
        assertSucceedingResults(results, indexOfTarget);
    }

    @Test
    public void testNeighborsNearEnd() throws Exception {
        populateNeighborhood(ResourceType.File, WINDOW_SIZE, 2, ResourceType.File);

        ContentObjectRecord targetMd = getMetadata(targetPid);

        List<ContentObjectRecord> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);

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

        ContentObjectRecord targetMd = getMetadata(targetPid);

        long start = System.nanoTime();
        List<ContentObjectRecord> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);
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
        String idPrefix = "9ef8d1c5-14a1-4ed3-b0c0-6da67fa5f6d";
        for (int i = 0; i < 3; i++) {
            PID pid = PIDs.get(idPrefix + i);
            addObject(docs, pid, "title", ResourceType.File,
                    testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);
        }

        PID tpid = PIDs.get(idPrefix + "3");
        addObject(docs, tpid, "title", ResourceType.File,
                testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);

        for (int i = 4; i < 7; i++) {
            PID pid = PIDs.get(idPrefix + i);
            addObject(docs, pid, "title", ResourceType.File,
                    testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);
        }

        server.add(docs);
        server.commit();

        ContentObjectRecord targetMd = getMetadata(tpid);
        List<ContentObjectRecord> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);

        assertEquals(7, results.size());
        int indexOfTarget = indexOf(results, targetMd);
        assertEquals(3, indexOfTarget);

        for (int i = 0; i < results.size(); i++) {
            ContentObjectRecord result = results.get(i);
            assertEquals(idPrefix + i, result.getId());
        }
    }

    @Test
    public void testNeighborsNoTitle() throws Exception {
        List<SolrInputDocument> docs = new ArrayList<>();
        String idPrefix = "9ef8d1c5-14a1-4ed3-b0c0-6da67fa5f6d";
        PID nPid0 = PIDs.get(idPrefix + "3");
        addObject(docs, nPid0, "title", ResourceType.File,
                testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);
        PID nPid1 = PIDs.get(idPrefix + "0");
        addObject(docs, nPid1, "", ResourceType.File,
                testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);

        PID tpid = PIDs.get(idPrefix + 1);
        addObject(docs, tpid, "", ResourceType.File,
                testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);

        PID nPid2 = PIDs.get(idPrefix + "2");
        addObject(docs, nPid2, "", ResourceType.File, testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);

        server.add(docs);
        server.commit();

        ContentObjectRecord targetMd = getMetadata(tpid);
        List<ContentObjectRecord> results = queryService.getNeighboringItems(targetMd, WINDOW_SIZE, groups);

        assertEquals(4, results.size());
        int indexOfTarget = indexOf(results, targetMd);
        assertEquals(1, indexOfTarget);

        for (int i = 0; i < results.size(); i++) {
            ContentObjectRecord result = results.get(i);
            assertEquals(idPrefix + i, result.getId());
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

    private int indexOf(List<ContentObjectRecord> results, ContentObjectRecord obj) {
        for (int i = 0; i < results.size(); i++) {
            ContentObjectRecord result = results.get(i);
            if (result.getId().equals(obj.getId())) {
                return i;
            }
        }
        return -1;
    }

    private void assertPrecedingResults(List<ContentObjectRecord> results, int end, int offset) {
        for (int i = 0; i < end; i++) {
            ContentObjectRecord result = results.get(i);
            assertEquals(PRECEDING_PREFIX + formatSortable(i + offset), result.getTitle());
        }
    }

    private void assertSucceedingResults(List<ContentObjectRecord> results, int start) {
        for (int i = start + 1; i < results.size(); i++) {
            ContentObjectRecord result = results.get(i);
            assertEquals(SUCCEEDING_PREFIX + formatSortable(i - start - 1), result.getTitle());
        }
    }

    private ContentObjectRecord getMetadata(PID pid) throws Exception {
        return solrSearchService.getObjectById(new SimpleIdRequest(pid, groups));
    }

    private String formatSortable(int number) {
        String formatted = Integer.toString(number);
        int numDigits = formatted.length();
        return StringUtils.repeat("0", NUM_DIGITS_SORT - numDigits) + formatted;
    }
}
