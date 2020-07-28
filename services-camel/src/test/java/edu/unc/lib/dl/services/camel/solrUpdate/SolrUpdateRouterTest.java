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
package edu.unc.lib.dl.services.camel.solrUpdate;

import static edu.unc.lib.dl.util.IndexingMessageHelper.makeIndexingOperationBody;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.spring.CamelSpringRunner;
import org.apache.camel.test.spring.CamelTestContextBootstrapper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.camel.util.MessageUtil;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 *
 * @author lfarrell
 * @author bbpennel
 *
 */
@RunWith(CamelSpringRunner.class)
@BootstrapWith(CamelTestContextBootstrapper.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/jms-context.xml"),
    @ContextConfiguration("/solr-update-context.xml")
})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class SolrUpdateRouterTest {
    private static final String USER = "user";

    @Produce(uri = "{{cdr.solrupdate.stream}}")
    private ProducerTemplate template;

    @Autowired
    private CamelContext cdrServiceSolrUpdate;

    @Autowired
    private SolrUpdateProcessor solrSmallUpdateProcessor;

    @Autowired
    private SolrUpdateProcessor solrLargeUpdateProcessor;

    @Autowired
    private SolrUpdatePreprocessor solrUpdatePreprocessor;

    private ArgumentCaptor<Exchange> exchangeCaptor;

    private RepositoryPIDMinter pidMinter;

    private PID targetPid;

    @Before
    public void init() {
        pidMinter = new RepositoryPIDMinter();
        targetPid = pidMinter.mintContentPid();
        exchangeCaptor = ArgumentCaptor.forClass(Exchange.class);
    }

    @AfterClass
    public static void after() throws Exception {
//        broker.stop();
    }

    @Test
    public void indexSingleObject() throws Exception {
        Document msg = makeIndexingOperationBody(USER, targetPid, null, IndexingActionType.ADD);
        template.sendBodyAndHeaders(msg, null);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(2)
                .create();

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Message count did not match expectations", result);

        verify(solrSmallUpdateProcessor).process(exchangeCaptor.capture());
        List<Exchange> exchanges = exchangeCaptor.getAllValues();
        assertMessage(exchanges, targetPid, IndexingActionType.ADD);
    }

    @Test
    public void indexSmallOneNotFoundRecover() throws Exception {
        doThrow(new NotFoundException("")).doNothing().when(solrSmallUpdateProcessor).process(any(Exchange.class));

        Document msg = makeIndexingOperationBody(USER, targetPid, null, IndexingActionType.ADD);
        Document msg2 = makeIndexingOperationBody(USER, targetPid, null, IndexingActionType.UPDATE_DESCRIPTION);
        template.sendBodyAndHeaders(msg, null);
        template.sendBodyAndHeaders(msg2, null);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(3)
                .create();

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Message count did not match expectations", result);

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
        template.sendBodyAndHeaders(msg1, null);
        template.sendBodyAndHeaders(msg2, null);
        template.sendBodyAndHeaders(msg3, null);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(4)
                .create();

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Message count did not match expectations", result);

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
        template.sendBodyAndHeaders(msg, null);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(1)
                .create();

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Message count did not match expectations", result);

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

        template.sendBodyAndHeaders(msg1, null);
        template.sendBodyAndHeaders(msg2, null);
        template.sendBodyAndHeaders(msg3, null);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(3)
                .create();

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Message count did not match expectations", result);

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
