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
package edu.unc.lib.dl.data.ingest.solr.action;

import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.getContentRootPid;
import static edu.unc.lib.dl.util.IndexingActionType.RECURSIVE_ADD;

import java.util.Properties;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.fcrepo4.GlobalPermissionEvaluator;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.data.ingest.solr.ProcessingStatus;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.util.SolrSettings;
import edu.unc.lib.dl.test.TestHelpers;

/**
 *
 * @author bbpennel
 *
 */
public class IndexTreeInplaceActionTest extends UpdateTreeActionTest {
    private static final Logger LOG = LoggerFactory.getLogger(IndexTreeInplaceActionTest.class);

    @Mock
    private SearchSettings searchSettings;
    private SolrSettings solrSettings;
    private SolrSearchService solrSearchService;
    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;

    @Before
    public void setupInplace() throws Exception {
        Properties solrProps = new Properties();
        solrProps.load(this.getClass().getResourceAsStream("/solr.properties"));
        solrSettings = new SolrSettings();
        solrSettings.setProperties(solrProps);

        ((IndexTreeInplaceAction) action).setSolrSettings(solrSettings);

        solrSearchService = new SolrSearchService();
        solrSearchService.setDisablePermissionFiltering(true);
        solrSearchService.setSolrSettings(solrSettings);
        solrSearchService.setSearchSettings(searchSettings);
        solrSearchService.setGlobalPermissionEvaluator(globalPermissionEvaluator);
        TestHelpers.setField(solrSearchService, "solrClient", server);

        action.setSolrSearchService(solrSearchService);
        action.setAccessGroups(new AccessGroupSet("admin"));
    }

    @Override
    protected UpdateTreeAction getAction() {
        return new IndexTreeInplaceAction();
    }

    @Test
    public void verifyOrphanCleanup() throws Exception {
        SolrDocumentList docListBefore = getDocumentList();

        SolrUpdateRequest request = new SolrUpdateRequest(corpus.pid2.getId(), RECURSIVE_ADD);
        request.setStatus(ProcessingStatus.ACTIVE);

        action.performAction(request);
        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        // Verify that the number of results has decreased
        assertEquals(6, docListBefore.getNumFound());
        assertEquals(5, docListAfter.getNumFound());

        // Verify that the orphan is not in the new result set
        assertObjectsNotExist(corpus.pid5);
    }

    @Test
    public void testIndexAll() throws Exception {
        SolrDocumentList docListBefore = getDocumentList();

        SolrUpdateRequest request = new SolrUpdateRequest(getContentRootPid().getRepositoryPath(),
                RECURSIVE_ADD);
        request.setStatus(ProcessingStatus.ACTIVE);

        action.performAction(request);
        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        // Verify that the number of results has decreased
        assertEquals(6, docListBefore.getNumFound());
        assertEquals(5, docListAfter.getNumFound());

        // Verify that the orphan is not in the new result set
        assertObjectsNotExist(corpus.pid5);
    }

    @Test(expected = IndexingException.class)
    public void testNoAncestorBean() throws Exception {

        server.deleteById(corpus.pid2.getId());
        server.commit();

        SolrUpdateRequest request = new SolrUpdateRequest(corpus.pid2.getId(), RECURSIVE_ADD);
        request.setStatus(ProcessingStatus.ACTIVE);

        action.performAction(request);
    }

    private void assertObjectsNotExist(PID... pids) throws Exception {
        SolrDocumentList docList = getDocumentList();

        for (SolrDocument docAfter : docList) {
            String id = (String) docAfter.getFieldValue("id");
            for (PID pid : pids) {
                assertFalse(pid.getId().equals(id));
            }
        }
    }
}
