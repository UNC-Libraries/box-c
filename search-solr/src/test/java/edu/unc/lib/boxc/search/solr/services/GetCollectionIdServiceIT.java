package edu.unc.lib.boxc.search.solr.services;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.test.BaseEmbeddedSolrTest;
import edu.unc.lib.boxc.search.solr.test.TestCorpus;
import edu.unc.lib.boxc.search.solr.utils.AccessRestrictionUtil;

/**
 * @author bbpennel
 */
public class GetCollectionIdServiceIT extends BaseEmbeddedSolrTest {
    private TestCorpus testCorpus;
    private GetCollectionIdService collIdService;
    private SolrSearchService solrSearchService;
    private AutoCloseable closeable;
    @Mock
    private AccessRestrictionUtil restrictionUtil;

    public GetCollectionIdServiceIT() {
        testCorpus = new TestCorpus();
    }

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        index(testCorpus.populate());

        solrSearchService = new SolrSearchService();
        solrSearchService.setSolrSettings(solrSettings);
        solrSearchService.setAccessRestrictionUtil(restrictionUtil);
        setField(solrSearchService, "solrClient", server);

        collIdService = new GetCollectionIdService();
        collIdService.setSolrSettings(solrSettings);
        setField(collIdService, "solrClient", server);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
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
