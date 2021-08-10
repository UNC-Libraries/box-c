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

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.test.BaseEmbeddedSolrTest;
import edu.unc.lib.dl.search.solr.test.TestCorpus;
import edu.unc.lib.dl.search.solr.util.AccessRestrictionUtil;

/**
 * @author bbpennel
 */
public class GetCollectionIdServiceIT extends BaseEmbeddedSolrTest {
    private TestCorpus testCorpus;
    private GetCollectionIdService collIdService;
    private SolrSearchService solrSearchService;
    @Mock
    private AccessRestrictionUtil restrictionUtil;

    public GetCollectionIdServiceIT() {
        testCorpus = new TestCorpus();
    }

    @Before
    public void init() throws Exception {
        initMocks(this);

        index(testCorpus.populate());

        solrSearchService = new SolrSearchService();
        solrSearchService.setSolrSettings(solrSettings);
        solrSearchService.setAccessRestrictionUtil(restrictionUtil);
        setField(solrSearchService, "solrClient", server);

        collIdService = new GetCollectionIdService();
        collIdService.setSolrSettings(solrSettings);
        setField(collIdService, "solrClient", server);
    }

    @Test
    public void collectionIdFromAncestorTest() throws Exception {
        ContentObjectRecord mdObj = getObject(testCorpus.work1Pid);

        String collId = collIdService.getCollectionId(mdObj);
        assertEquals(TestCorpus.TEST_COLL_ID, collId);
    }

    @Test
    public void collectionIdFromSelfTest() throws Exception {
        ContentObjectRecord mdObj = getObject(testCorpus.coll1Pid);

        String collId = collIdService.getCollectionId(mdObj);
        assertEquals(TestCorpus.TEST_COLL_ID, collId);
    }

    @Test
    public void noCollectionIdTest() throws Exception {
        ContentObjectRecord mdObj = getObject(testCorpus.work3Pid);

        String collId = collIdService.getCollectionId(mdObj);
        assertNull(collId);
    }

    @Test
    public void noCollectionIdUnitTest() throws Exception {
        ContentObjectRecord mdObj = getObject(testCorpus.unitPid);

        String collId = collIdService.getCollectionId(mdObj);
        assertNull(collId);
    }

    private ContentObjectRecord getObject(PID pid) {
        return solrSearchService.getObjectById(
                new SimpleIdRequest(pid, null));
    }
}
