package edu.unc.lib.boxc.indexing.solr.action;

import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.DELETE_SOLR_TREE;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.IOException;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.common.test.TestHelpers;
import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.test.TestCorpus;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import edu.unc.lib.boxc.search.solr.utils.AccessRestrictionUtil;

/**
 *
 * @author bbpennel
 *
 */
public class DeleteSolrTreeTest extends BaseEmbeddedSolrTest {

    protected TestCorpus corpus;
    private AutoCloseable closeable;

    @Mock
    private AccessRestrictionUtil restrictionUtil;

    private SearchSettings searchSettings;
    private SolrSettings solrSettings;
    private SolrSearchService solrSearchService;

    private DeleteSolrTreeAction action;

    @Mock
    private ContentObjectSolrRecord metadata;

    @BeforeEach
    public void setup() throws SolrServerException, IOException {
        closeable = openMocks(this);

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
        action.setAccessGroups(new AccessGroupSetImpl("admin"));
        action.setSolrSearchService(solrSearchService);

        server.add(corpus.populate());
        server.commit();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
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

        assertEquals(7, docListAfter.getNumFound());
    }

    @Test
    public void deleteSimple() throws Exception {
        SolrUpdateRequest updateRequest = new SolrUpdateRequest(corpus.pid6.getId(), DELETE_SOLR_TREE);

        action.performAction(updateRequest);
        server.commit();

        SolrDocumentList docListAfter = getDocumentList();
        assertEquals(5, docListAfter.getNumFound(), "One object should have been removed");

        assertObjectsNotExist(corpus.pid6);
    }

    @Test
    public void deleteEverything() throws Exception {
        SolrUpdateRequest updateRequest = new SolrUpdateRequest(corpus.pid1.getId(), DELETE_SOLR_TREE);

        action.performAction(updateRequest);
        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        assertEquals(0, docListAfter.getNumFound(), "Index should be empty");
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
