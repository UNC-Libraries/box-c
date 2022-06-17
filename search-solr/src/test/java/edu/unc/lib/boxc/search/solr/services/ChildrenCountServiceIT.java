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

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static edu.unc.lib.boxc.model.api.ResourceType.File;
import static edu.unc.lib.boxc.search.api.SearchFieldKey.RESOURCE_TYPE;
import static edu.unc.lib.boxc.search.solr.services.ChildrenCountService.CHILD_COUNT;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.services.ChildrenCountService;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import edu.unc.lib.boxc.search.solr.test.BaseEmbeddedSolrTest;
import edu.unc.lib.boxc.search.solr.test.TestCorpus;
import edu.unc.lib.boxc.search.solr.utils.AccessRestrictionUtil;

/**
 *
 * @author bbpennel
 *
 */
public class ChildrenCountServiceIT extends BaseEmbeddedSolrTest {

    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;
    @Mock
    private SearchSettings searchSettings;

    private AccessGroupSet principals;
    private AccessRestrictionUtil restrictionUtil;
    private TestCorpus testCorpus;

    private SolrSearchService solrSearchService;

    private ChildrenCountService countService;

    public ChildrenCountServiceIT() {
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
        when(searchSettings.getAllowPatronAccess()).thenReturn(true);

        solrSearchService = new SolrSearchService();
        solrSearchService.setSolrSettings(solrSettings);
        solrSearchService.setAccessRestrictionUtil(restrictionUtil);
        setField(solrSearchService, "solrClient", server);

        countService = new ChildrenCountService();
        countService.setSolrSettings(solrSettings);
        countService.setAccessRestrictionUtil(restrictionUtil);
        setField(countService, "solrClient", server);

        principals = new AccessGroupSetImpl(PUBLIC_PRINC);
    }

    @Test
    public void testGetChildrenCount() throws Exception {
        ContentObjectRecord bom = getObject(testCorpus.coll1Pid);

        assertEquals(3, countService.getChildrenCount(bom, principals));
    }

    @Test
    public void testGetChildrenCountWithAccessRestriction() throws Exception {
        restrictionUtil.setDisablePermissionFiltering(false);

        ContentObjectRecord bom = getObject(testCorpus.coll2Pid);

        assertEquals(1, countService.getChildrenCount(bom, principals));
    }

    @Test
    public void testGetChildrenCountForWork() throws Exception {
        ContentObjectRecord bom = getObject(testCorpus.work1Pid);

        assertEquals(2, countService.getChildrenCount(bom, principals));
    }

    @Test
    public void testAddChildrenCounts() throws Exception {
        ContentObjectRecord folder1 = getObject(testCorpus.folder1Pid);
        ContentObjectRecord coll2 = getObject(testCorpus.coll2Pid);

        countService.addChildrenCounts(asList(folder1, coll2), principals);

        assertCountEquals(2, folder1, CHILD_COUNT);
        assertCountEquals(3, coll2, CHILD_COUNT);
    }

    @Test
    public void testAddChildrenCountsWithAccessRestriction() throws Exception {
        restrictionUtil.setDisablePermissionFiltering(false);

        ContentObjectRecord folder1 = getObject(testCorpus.folder1Pid);
        ContentObjectRecord coll2 = getObject(testCorpus.coll2Pid);

        countService.addChildrenCounts(asList(folder1, coll2), principals);

        assertCountEquals(2, folder1, CHILD_COUNT);
        assertCountEquals(1, coll2, CHILD_COUNT);
    }

    @Test
    public void testAddChildrenCountsFromBaseQuery() throws Exception {
        ContentObjectRecord folder1 = getObject(testCorpus.folder1Pid);
        ContentObjectRecord coll2 = getObject(testCorpus.coll2Pid);

        SolrQuery baseQuery = new SolrQuery("*:*");
        // Count only files
        baseQuery.addFilterQuery(countService.makeFilter(RESOURCE_TYPE,
                asList(File.name())));

        countService.addChildrenCounts(asList(folder1, coll2), principals, "files", baseQuery);

        assertCountEquals(3, folder1, "files");
        assertCountEquals(2, coll2, "files");
    }

    @Test
    public void testAddChildrenCountsBinarySearch() throws Exception {
        // Add additional objects to reach the point of needing binary search
        List<SolrInputDocument> docs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            PID pid = PIDs.get(UUID.randomUUID().toString());
            SolrInputDocument doc = testCorpus.makeContainerDocument(pid, pid.getId(), ResourceType.Folder,
                    "2017-01-01", testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);
            docs.add(doc);

            // Add a child to the folder so it will have a count
            PID pid2 = PIDs.get(UUID.randomUUID().toString());
            SolrInputDocument doc2 = testCorpus.makeContainerDocument(pid2, pid2.getId(), ResourceType.Folder,
                    "2017-01-01", testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid, pid);
            docs.add(doc2);
        }

        index(docs);

        ContentObjectRecord coll1 = getObject(testCorpus.coll1Pid);

        countService.addChildrenCounts(asList(coll1), principals);

        assertCountEquals(203, coll1, CHILD_COUNT);
    }

    private ContentObjectRecord getObject(PID pid) {
        return solrSearchService.getObjectById(
                new SimpleIdRequest(pid, principals));
    }

    private void assertCountEquals(long count, ContentObjectRecord md, String countType) {
        assertEquals("Incorrect number of children", count, md.getCountMap().get(countType).intValue());
    }
}
