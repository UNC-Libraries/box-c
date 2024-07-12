package edu.unc.lib.boxc.services.camel.solrUpdate;

import edu.unc.lib.boxc.indexing.solr.action.IndexingAction;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.model.fcrepo.test.TestRepositoryDeinitializer;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.services.TitleRetrievalService;
import edu.unc.lib.boxc.services.camel.solr.AbstractSolrProcessorIT;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.jdom2.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.UPDATE_ACCESS_TREE;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageHelper.makeIndexingOperationBody;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 *
 * @author bbpennel
 *
 */
public class SolrUpdateProcessorIT extends AbstractSolrProcessorIT {
    private AutoCloseable closeable;

    @TempDir
    public Path tmpFolder;

    private SolrUpdateProcessor processor;

    private CamelContext cdrServiceSolrUpdate;
    private SolrClient solrClient;
    private MessageSender updateWorkSender;
    private IndexingMessageSender indexingMessageSender;
    private TitleRetrievalService titleRetrievalService;
    private Map<IndexingActionType, IndexingAction> solrIndexingActionMap;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext(
                "spring-test/cdr-client-container.xml",
                "spring-test/solr-indexing-context.xml",
                "solr-indexing-it-context.xml",
                "spring-test/jms-context.xml",
                "solr-update-processor-it-context.xml");
    }

    @BeforeEach
    public void setUpTest() throws Exception {
        closeable = openMocks(this);
        initCommon();

        TestHelper.setContentBase(baseAddress);
        cdrServiceSolrUpdate = applicationContext.getBean(CamelContext.class);
        solrClient = applicationContext.getBean(SolrClient.class);
        updateWorkSender = applicationContext.getBean("updateWorkSender", MessageSender.class);
        indexingMessageSender = applicationContext.getBean(IndexingMessageSender.class);
        titleRetrievalService = applicationContext.getBean(TitleRetrievalService.class);
        solrIndexingActionMap = applicationContext.getBean("solrIndexingActionMap", Map.class);

        processor = new SolrUpdateProcessor();
        processor.setSolrIndexingActionMap(solrIndexingActionMap);
        processor.setRepositoryObjectLoader(repositoryObjectLoader);
        processor.setUpdateWorkSender(updateWorkSender);
        processor.setTitleRetrievalService(titleRetrievalService);
        processor.setIndexingMessageSender(indexingMessageSender);
        processor.setSolrClient(solrClient);

        when(exchange.getIn()).thenReturn(message);

        generateBaseStructure();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
        TestRepositoryDeinitializer.cleanup(fcrepoClient);
        storageLocationTestHelper.cleanupStorageLocations();
    }

    @Test
    public void testReindexAcls() throws Exception {
        indexObjectsInTripleStore();

        repositoryObjectSolrIndexer.index(unitObj.getPid(), collObj.getPid());

        makeIndexingMessage(unitObj, null, UPDATE_ACCESS_TREE);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(2)
                .create();

        processor.process(exchange);

        assertTrue(notify.matches(6L, TimeUnit.SECONDS));

        server.commit();

        ContentObjectRecord unitMd = getSolrMetadata(unitObj);

        assertEquals(unitMd.getId(), unitMd.getTitle(), "Title should not have updated");

        assertTrue(unitMd.getReadGroup().contains(PUBLIC_PRINC), "Read groups did not contain assigned group");
        assertTrue(unitMd.getAdminGroup().contains("admin"), "Admin groups did not contain assigned group");

        assertNotNull(unitMd.getDateAdded());

        ContentObjectRecord collMd = getSolrMetadata(collObj);

        assertEquals(collMd.getId(), collMd.getTitle(), "Title should not have updated");

        assertTrue(collMd.getReadGroup().contains(AUTHENTICATED_PRINC), "Read groups did not contain assigned group");
        assertTrue(collMd.getAdminGroup().contains("admin"), "Admin groups did not contain assigned group");

        assertNotNull(collMd.getDateAdded());
        assertNotNull(collMd.getDateUpdated());
    }

    private void makeIndexingMessage(RepositoryObject targetObj, Collection<PID> children, IndexingActionType action) {
        setMessageTarget(targetObj);
        Document messageBody = makeIndexingOperationBody("user", targetObj.getPid(), children, action);
        when(message.getBody()).thenReturn(messageBody);
    }

    private ContentObjectRecord getSolrMetadata(RepositoryObject obj) throws Exception {
        SolrQuery solrQuery = new SolrQuery();
        StringBuilder query = new StringBuilder();
        query.append("id:").append(obj.getPid().getId());

        solrQuery.setQuery(query.toString());
        solrQuery.setRows(1);

        QueryResponse resp = solrClient.query(solrQuery);

        List<ContentObjectSolrRecord> results = resp.getBeans(ContentObjectSolrRecord.class);
        if (results != null && results.size() > 0) {
            return results.get(0);
        }
        return null;
    }

}
