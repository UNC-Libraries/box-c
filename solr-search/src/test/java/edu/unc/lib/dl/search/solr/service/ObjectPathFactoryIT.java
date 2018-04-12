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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.ObjectPath;
import edu.unc.lib.dl.search.solr.model.ObjectPathEntry;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.test.BaseEmbeddedSolrTest;
import edu.unc.lib.dl.search.solr.test.TestCorpus;
import edu.unc.lib.dl.search.solr.util.AccessRestrictionUtil;
import edu.unc.lib.dl.test.TestHelpers;

/**
 *
 * @author bbpennel
 *
 */
public class ObjectPathFactoryIT extends BaseEmbeddedSolrTest {
    private TestCorpus testCorpus;
    private SolrSearchService solrSearchService;

    private ObjectPathFactory objPathFactory;

    @Mock
    private AccessRestrictionUtil restrictionUtil;

    public ObjectPathFactoryIT() {
        testCorpus = new TestCorpus();
    }

    @Before
    public void init() throws Exception {
        initMocks(this);

        index(testCorpus.populate());

        solrSearchService = new SolrSearchService();
        solrSearchService.setSolrSettings(solrSettings);
        solrSearchService.setAccessRestrictionUtil(restrictionUtil);
        TestHelpers.setField(solrSearchService, "solrClient", server);

        objPathFactory = new ObjectPathFactory();
        objPathFactory.setSolrSettings(solrSettings);
        objPathFactory.setSearch(solrSearchService);
        objPathFactory.setTimeToLiveMilli(500L);
        objPathFactory.setCacheSize(10);
        objPathFactory.init();
    }

    @Test
    public void testGetWorkPathByPid() throws Exception {
        ObjectPath path = objPathFactory.getPath(testCorpus.work1Pid);

        assertPathPids(path, testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid,
                testCorpus.folder1Pid, testCorpus.work1Pid);
        assertEquals("/Collections/Unit/Collection 1/Folder 1/Work 1", path.toNamePath());
    }

    @Test
    public void testGetFilePathByPid() throws Exception {
        ObjectPath path = objPathFactory.getPath(testCorpus.work3File1Pid);

        assertPathPids(path, testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll2Pid,
                testCorpus.work3Pid, testCorpus.work3File1Pid);
        assertEquals("/Collections/Unit/Collection 2/Work 3/File 1", path.toNamePath());
    }

    @Test
    public void testGetRootPathByPid() throws Exception {
        ObjectPath path = objPathFactory.getPath(testCorpus.rootPid);

        assertPathPids(path, testCorpus.rootPid);
        assertEquals("/Collections", path.toNamePath());
    }

    @Test
    public void testNoAncestorPath() throws Exception {
        BriefObjectMetadata bom = mock(BriefObjectMetadata.class);

        ObjectPath path = objPathFactory.getPath(bom);

        assertNull(path);
    }

    @Test
    public void testGetPathByMetadata() throws Exception {
        BriefObjectMetadata bom = solrSearchService.getObjectById(new SimpleIdRequest(testCorpus.coll1Pid.getId()));

        ObjectPath path = objPathFactory.getPath(bom);

        assertPathPids(path, testCorpus.rootPid, testCorpus.unitPid, testCorpus.coll1Pid);
        assertEquals("/Collections/Unit/Collection 1", path.toNamePath());
    }

    private void assertPathPids(ObjectPath path, PID... pids) {
        List<ObjectPathEntry> pathEntries = path.getEntries();

        for (int i = 0; i < pids.length; i++) {
            assertEquals("Path entry did not contain expected value",
                    pids[i].getId(), pathEntries.get(i).getPid());
        }

        String joinedPath = "/" + Arrays.stream(pids).map(p -> p.getId()).collect(Collectors.joining("/"));
        assertEquals(joinedPath, path.toIdPath());
    }
}
