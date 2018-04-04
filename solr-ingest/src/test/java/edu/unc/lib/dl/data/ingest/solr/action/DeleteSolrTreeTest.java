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

import static edu.unc.lib.dl.util.IndexingActionType.DELETE_SOLR_TREE;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.test.TestCorpus;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.search.solr.util.AccessRestrictionUtil;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.util.SolrSettings;
import edu.unc.lib.dl.test.TestHelpers;

/**
 *
 * @author bbpennel
 *
 */
public class DeleteSolrTreeTest extends BaseEmbeddedSolrTest {

    protected TestCorpus corpus;

    @Mock
    private AccessRestrictionUtil restrictionUtil;

    private SearchSettings searchSettings;
    private SolrSettings solrSettings;
    private SolrSearchService solrSearchService;

    private DeleteSolrTreeAction action;

    @Mock
    private BriefObjectMetadataBean metadata;

    @Before
    public void setup() throws SolrServerException, IOException {
        initMocks(this);

        corpus = new TestCorpus();

        Properties solrProps = new Properties();
        solrProps.load(this.getClass().getResourceAsStream("/solr.properties"));
        solrSettings = new SolrSettings();
        solrSettings.setProperties(solrProps);

        Properties searchProps = new Properties();
        searchProps.load(this.getClass().getResourceAsStream("/search.properties"));
        searchSettings = new SearchSettings();
        searchSettings.setProperties(searchProps);

        solrSearchService = new SolrSearchService();
        solrSearchService.setSolrSettings(solrSettings);
        solrSearchService.setSearchSettings(searchSettings);
        solrSearchService.setAccessRestrictionUtil(restrictionUtil);
        TestHelpers.setField(solrSearchService, "solrClient", server);

        action = new DeleteSolrTreeAction();
        action.setSolrUpdateDriver(driver);
        action.setSolrSettings(solrSettings);
        action.setSearchSettings(searchSettings);
        action.setSolrSearchService(solrSearchService);
        action.setAccessGroups(new AccessGroupSet("admin"));
        action.setSolrSearchService(solrSearchService);

        server.add(corpus.populate());
        server.commit();
    }

    @Test
    public void deleteTree() throws Exception {
        SolrUpdateRequest updateRequest = new SolrUpdateRequest(corpus.pid2.getId(), DELETE_SOLR_TREE);

        action.performAction(updateRequest);
        server.commit();

        SolrDocumentList docListAfter = getDocumentList();
        assertEquals(2, docListAfter.getNumFound());

        assertObjectsNotExist(corpus.pid2, corpus.pid4, corpus.pid6, corpus.pid5);
    }

    @Test
    public void deleteNonexistent() throws Exception {
        SolrUpdateRequest updateRequest = new SolrUpdateRequest(corpus.nonExistentPid.getId(), DELETE_SOLR_TREE);

        action.performAction(updateRequest);
        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        assertEquals(6, docListAfter.getNumFound());
    }

    @Test
    public void deleteSimple() throws Exception {
        SolrUpdateRequest updateRequest = new SolrUpdateRequest(corpus.pid6.getId(), DELETE_SOLR_TREE);

        action.performAction(updateRequest);
        server.commit();

        SolrDocumentList docListAfter = getDocumentList();
        assertEquals("One object should have been removed", 5, docListAfter.getNumFound());

        assertObjectsNotExist(corpus.pid6);
    }

    @Test
    public void deleteEverything() throws Exception {
        SolrUpdateRequest updateRequest = new SolrUpdateRequest(corpus.pid1.getId(), DELETE_SOLR_TREE);

        action.performAction(updateRequest);
        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        assertEquals("Index should be empty", 0, docListAfter.getNumFound());
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
