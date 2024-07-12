package edu.unc.lib.boxc.services.camel.cdrEvents;

import edu.unc.lib.boxc.indexing.solr.ChildSetRequest;
import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.action.IndexingAction;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.services.camel.solrUpdate.SolrUpdateProcessor;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.ADD_SET_TO_PARENT;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.UPDATE_ACCESS_TREE;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 *
 * @author bbpennel
 *
 */
public class CdrEventRoutingTest extends CamelSpringTestSupport {

    private static final String USER_ID = "user";
    private static final String DEPOSIT_ID = "deposit";
    private static final String BASE_URI = "http://example.com/rest/";
    private AutoCloseable closeable;

    private OperationsMessageSender opsMsgSender;
    private SolrUpdateProcessor solrSmallUpdateProcessor;
    private SolrUpdateProcessor solrLargeUpdateProcessor;

    private CamelContext camelContext;

    @Mock
    private Map<IndexingActionType, IndexingAction> mockActionMap;
    @Mock
    private IndexingAction mockIndexingAction;
    @Captor
    private ArgumentCaptor<SolrUpdateRequest> updateRequestCaptor;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        TestHelper.setContentBase(BASE_URI);

        when(mockActionMap.get(any(IndexingActionType.class)))
                .thenReturn(mockIndexingAction);
        camelContext = applicationContext.getBean("cdrServiceCdrEvents", CamelContext.class);
        opsMsgSender = applicationContext.getBean(OperationsMessageSender.class);
        solrSmallUpdateProcessor = applicationContext.getBean("solrSmallUpdateProcessor", SolrUpdateProcessor.class);
        solrLargeUpdateProcessor = applicationContext.getBean("solrLargeUpdateProcessor", SolrUpdateProcessor.class);

        solrSmallUpdateProcessor.setSolrIndexingActionMap(mockActionMap);
        solrLargeUpdateProcessor.setSolrIndexingActionMap(mockActionMap);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("spring-test/jms-context.xml", "cdr-event-routing-it-context.xml");
    }

    @Test
    public void testAddAction() throws Exception {
        List<PID> added = pidList(3);
        List<PID> destinations = pidList(1);

        NotifyBuilder notify = new NotifyBuilder(camelContext)
                .whenCompleted(2)
                .create();

        opsMsgSender.sendAddOperation(USER_ID, destinations, added, emptyList(), DEPOSIT_ID);

        notify.matches(3L, TimeUnit.SECONDS);

        verify(mockIndexingAction).performAction(updateRequestCaptor.capture());

        ChildSetRequest updateRequest = (ChildSetRequest) updateRequestCaptor.getValue();
        assertEquals(ADD_SET_TO_PARENT, updateRequest.getUpdateAction());

        assertTrue(updateRequest.getChildren().containsAll(added));
    }

    @Test
    public void testEditAccessControlAction() throws Exception {
        int numPids = 3;
        List<PID> pids = pidList(numPids);

        NotifyBuilder notify = new NotifyBuilder(camelContext)
                .whenCompleted(2)
                .create();

        opsMsgSender.sendMarkForDeletionOperation(USER_ID, pids);

        notify.matches(3L, TimeUnit.SECONDS);

        verify(mockIndexingAction).performAction(updateRequestCaptor.capture());

        ChildSetRequest updateRequest = (ChildSetRequest) updateRequestCaptor.getValue();
        assertEquals(UPDATE_ACCESS_TREE, updateRequest.getUpdateAction());

        assertTrue(updateRequest.getChildren().containsAll(pids));
    }

    private List<PID> pidList(int count) {
        List<PID> pidList = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            pidList.add(PIDs.get(UUID.randomUUID().toString()));
        }
        return pidList;
    }
}
