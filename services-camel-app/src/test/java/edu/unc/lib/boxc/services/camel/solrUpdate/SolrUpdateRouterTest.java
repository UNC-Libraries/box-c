package edu.unc.lib.boxc.services.camel.solrUpdate;

import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPIDMinter;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingPriority;
import edu.unc.lib.boxc.services.camel.util.MessageUtil;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.spring.CamelSpringRunner;
import org.apache.camel.test.spring.CamelTestContextBootstrapper;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageHelper.makeIndexingOperationBody;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author lfarrell
 * @author bbpennel
 *
 */
public class SolrUpdateRouterTest extends CamelSpringTestSupport {
    private static final String USER = "user";

    @Produce(uri = "{{cdr.solrupdate.stream}}")
    private ProducerTemplate template;

    @Produce(uri = "{{cdr.solrupdate.workObject.fileUpdated.individual}}")
    private ProducerTemplate templateWorkFromFile;

    private IndexingMessageSender indexingMessageSender;

    private CamelContext cdrServiceSolrUpdate;

    private SolrUpdateProcessor solrSmallUpdateProcessor;

    private SolrUpdateProcessor solrLargeUpdateProcessor;

    private SolrUpdatePreprocessor solrUpdatePreprocessor;

    private SolrClient solrClient;

    private ArgumentCaptor<Exchange> exchangeCaptor;

    private PIDMinter pidMinter;

    private PID targetPid;

    @BeforeEach
    public void init() {
        pidMinter = new RepositoryPIDMinter();
        targetPid = pidMinter.mintContentPid();
        exchangeCaptor = ArgumentCaptor.forClass(Exchange.class);
        indexingMessageSender = applicationContext.getBean(IndexingMessageSender.class);
        solrSmallUpdateProcessor = applicationContext.getBean("solrSmallUpdateProcessor", SolrUpdateProcessor.class);
        solrLargeUpdateProcessor = applicationContext.getBean("solrLargeUpdateProcessor", SolrUpdateProcessor.class);
        solrUpdatePreprocessor = applicationContext.getBean(SolrUpdatePreprocessor.class);
        solrClient = applicationContext.getBean(SolrClient.class);
        cdrServiceSolrUpdate = applicationContext.getBean(CamelContext.class);
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("spring-test/jms-context.xml", "solr-update-context.xml");
    }

    @Test
    public void indexSingleObject() throws Exception {
        Document msg = makeIndexingOperationBody(USER, targetPid, null, IndexingActionType.ADD);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(1)
                .create();

        template.sendBodyAndHeaders(msg, null);

        notify.matches(5l, TimeUnit.SECONDS);

        verify(solrSmallUpdateProcessor).process(exchangeCaptor.capture());
        List<Exchange> exchanges = exchangeCaptor.getAllValues();
        assertMessage(exchanges, targetPid, IndexingActionType.ADD);
    }

    @Test
    public void indexSmallOneNotFoundRecover() throws Exception {
        doThrow(new NotFoundException("")).doNothing().when(solrSmallUpdateProcessor).process(any(Exchange.class));

        Document msg = makeIndexingOperationBody(USER, targetPid, null, IndexingActionType.ADD);
        Document msg2 = makeIndexingOperationBody(USER, targetPid, null, IndexingActionType.UPDATE_DESCRIPTION);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(2)
                .create();

        template.sendBodyAndHeaders(msg, null);
        template.sendBodyAndHeaders(msg2, null);

        notify.matches(5l, TimeUnit.SECONDS);

        verify(solrSmallUpdateProcessor, times(3)).process(exchangeCaptor.capture());
        List<Exchange> exchanges = exchangeCaptor.getAllValues();
        assertMessage(exchanges, targetPid, IndexingActionType.ADD);
    }

    @Test
    public void indexMultipleSmall() throws Exception {
        PID targetPid2 = pidMinter.mintContentPid();
        Document msg1 = makeIndexingOperationBody(USER, targetPid, null, IndexingActionType.ADD);
        Document msg2 = makeIndexingOperationBody(USER, targetPid2, null, IndexingActionType.UPDATE_DESCRIPTION);
        Document msg3 = makeIndexingOperationBody(USER, targetPid, null, IndexingActionType.UPDATE_ACCESS);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(3)
                .create();

        template.sendBodyAndHeaders(msg1, null);
        template.sendBodyAndHeaders(msg2, null);
        template.sendBodyAndHeaders(msg3, null);

        notify.matches(5l, TimeUnit.SECONDS);

        verify(solrSmallUpdateProcessor, times(3)).process(exchangeCaptor.capture());
        List<Exchange> exchanges = exchangeCaptor.getAllValues();
        assertMessage(exchanges, targetPid, IndexingActionType.ADD);
        assertMessage(exchanges, targetPid2, IndexingActionType.UPDATE_DESCRIPTION);
        assertMessage(exchanges, targetPid, IndexingActionType.UPDATE_ACCESS);
    }

    @Test
    public void indexLarge() throws Exception {
        Document msg = makeIndexingOperationBody(USER, targetPid, Arrays.asList(targetPid),
                IndexingActionType.UPDATE_ACCESS_TREE);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(1)
                .create();

        template.sendBodyAndHeaders(msg, null);

        notify.matches(5l, TimeUnit.SECONDS);

        verify(solrLargeUpdateProcessor).process(exchangeCaptor.capture());
        List<Exchange> exchanges = exchangeCaptor.getAllValues();
        assertMessage(exchanges, targetPid, IndexingActionType.UPDATE_ACCESS_TREE);
    }

    @Test
    public void indexMixed() throws Exception {
        PID targetPid2 = pidMinter.mintContentPid();
        Document msg1 = makeIndexingOperationBody(USER, targetPid, null, IndexingActionType.UPDATE_DESCRIPTION);
        Document msg2 = makeIndexingOperationBody(USER, targetPid, Arrays.asList(targetPid2),
                IndexingActionType.ADD_SET_TO_PARENT);
        Document msg3 = makeIndexingOperationBody(USER, targetPid2, null, IndexingActionType.ADD);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(3)
                .create();

        template.sendBodyAndHeaders(msg1, null);
        template.sendBodyAndHeaders(msg2, null);
        template.sendBodyAndHeaders(msg3, null);

        notify.matches(5l, TimeUnit.SECONDS);

        verify(solrLargeUpdateProcessor).process(exchangeCaptor.capture());
        List<Exchange> largeExchanges = exchangeCaptor.getAllValues();
        assertMessage(largeExchanges, targetPid, IndexingActionType.ADD_SET_TO_PARENT);

        verify(solrSmallUpdateProcessor, times(2)).process(exchangeCaptor.capture());
        List<Exchange> smallExchanges = exchangeCaptor.getAllValues();
        assertMessage(smallExchanges, targetPid2, IndexingActionType.ADD);
        assertMessage(smallExchanges, targetPid, IndexingActionType.UPDATE_DESCRIPTION);
    }

    @Test
    public void indexUnknownAction() throws Exception {
        Document msg = makeIndexingOperationBody(USER, targetPid, null, IndexingActionType.UNKNOWN);
        template.sendBodyAndHeaders(msg, null);

        verify(solrUpdatePreprocessor, timeout(1000).times(1)).logUnknownSolrUpdate(any());
        verify(solrSmallUpdateProcessor, never()).process(any(Exchange.class));
        verify(solrLargeUpdateProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void indexLowPriority() throws Exception {
        Document msg = makeIndexingOperationBody(USER, targetPid, Arrays.asList(targetPid),
                IndexingActionType.UPDATE_ACCESS_TREE);
        msg.getRootElement()
                .addContent(new Element("category", ATOM_NS).setText(IndexingPriority.low.name()));

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(1)
                .create();

        template.sendBodyAndHeaders(msg, null);

        notify.matches(5l, TimeUnit.SECONDS);

        verify(solrSmallUpdateProcessor).process(exchangeCaptor.capture());
        List<Exchange> exchanges = exchangeCaptor.getAllValues();
        assertMessage(exchanges, targetPid, IndexingActionType.UPDATE_ACCESS_TREE);
    }

    @Test
    public void multipleWorkFromFile() throws Exception {
        when(solrClient.getById(any(String.class))).thenReturn(new SolrDocument());

        PID targetPid1 = pidMinter.mintContentPid();
        PID targetPid2 = pidMinter.mintContentPid();

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(5)
                .create();

        templateWorkFromFile.sendBodyAndHeaders(targetPid1.getId(), null);
        templateWorkFromFile.sendBodyAndHeaders(targetPid2.getId(), null);
        // Repeat first message, should only produce one final message
        templateWorkFromFile.sendBodyAndHeaders(targetPid1.getId(), null);
        templateWorkFromFile.sendBodyAndHeaders(targetPid1.getId(), null);

        notify.matches(3l, TimeUnit.SECONDS);

        verify(solrClient).commit();
        verify(indexingMessageSender).sendIndexingOperation(null, targetPid1, IndexingActionType.UPDATE_WORK_FILES);
        verify(indexingMessageSender).sendIndexingOperation(null, targetPid2, IndexingActionType.UPDATE_WORK_FILES);
    }

    private void assertMessage(List<Exchange> exchanges, PID expectedPid, IndexingActionType expectedAction)
            throws Exception {
        for (Exchange exchange : exchanges) {
            Document msgBody = MessageUtil.getDocumentBody(exchange.getIn());
            Element body = msgBody.getRootElement();

            String action = body.getChild("actionType", ATOM_NS).getTextTrim();
            IndexingActionType actionType = IndexingActionType.getAction(action);

            String pid = body.getChild("pid", ATOM_NS).getTextTrim();

            if (expectedAction.equals(actionType) && expectedPid.getQualifiedId().equals(pid)) {
                return;
            }
        }

        fail("No exchanges contained expected " + expectedPid.getQualifiedId() + " action " + expectedAction
                + ", there were " + exchanges.size() + " exchanges");
    }
}
