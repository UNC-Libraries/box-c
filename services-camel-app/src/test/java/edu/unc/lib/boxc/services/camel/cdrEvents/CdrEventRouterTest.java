package edu.unc.lib.boxc.services.camel.cdrEvents;

import edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions;
import edu.unc.lib.boxc.services.camel.solr.CdrEventToSolrUpdateProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrUpdateAction;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 *
 * @author lfarrell
 *
 */
@ExtendWith(MockitoExtension.class)
public class CdrEventRouterTest extends CamelTestSupport {
    @Produce(uri = "direct:start")
    private ProducerTemplate template;

    @Mock
    private CdrEventProcessor cdrEventProcessor;

    @Mock
    private CdrEventToSolrUpdateProcessor cdrEventToSolrUpdateProcessor;

    @Override
    protected RouteBuilder createRouteBuilder() {
        var router = new CdrEventRouter();
        router.setCdrEventProcessor(cdrEventProcessor);
        router.setCdrEventToSolrUpdateProcessor(cdrEventToSolrUpdateProcessor);
        router.setCdrStreamCamel("direct:cdr.event.stream");
        router.setErrorRetryDelay(0);
        router.setErrorBackOffMultiplier(1);
        router.setErrorMaxRedeliveries(1);
        return router;
    }

    @Test
    public void testMoveFilter() throws Exception {
        var solrUpdateEndpoint = getMockEndpoint("mock:direct:solr-update");
        solrUpdateEndpoint.expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.MOVE.toString()));
        solrUpdateEndpoint.assertIsSatisfied();
    }

    @Test
    public void testRemoveFilter() throws Exception {
        var solrUpdateEndpoint = getMockEndpoint("mock:direct:solr-update");
        solrUpdateEndpoint.expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.REMOVE.toString()));
        solrUpdateEndpoint.assertIsSatisfied();
    }

    @Test
    public void testAddFilter() throws Exception {
        var solrUpdateEndpoint = getMockEndpoint("mock:direct:solr-update");
        solrUpdateEndpoint.expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.ADD.toString()));
        solrUpdateEndpoint.assertIsSatisfied();
    }

    @Test
    public void testMarkForDeletionFilter() throws Exception {
        var solrUpdateEndpoint = getMockEndpoint("mock:direct:solr-update");
        solrUpdateEndpoint.expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.MARK_FOR_DELETION.toString()));
        solrUpdateEndpoint.assertIsSatisfied();
    }

    @Test
    public void testEditAccessFilter() throws Exception {
        var solrUpdateEndpoint = getMockEndpoint("mock:direct:solr-update");
        solrUpdateEndpoint.expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.EDIT_ACCESS_CONTROL.toString()));
        solrUpdateEndpoint.assertIsSatisfied();
    }

    @Test
    public void testEditTypeFilter() throws Exception {
        var solrUpdateEndpoint = getMockEndpoint("mock:direct:solr-update");
        solrUpdateEndpoint.expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.EDIT_TYPE.toString()));
        solrUpdateEndpoint.assertIsSatisfied();
    }

    @Test
    public void testFilterFail() throws Exception {
        var solrUpdateEndpoint = getMockEndpoint("mock:direct:solr-update");
        solrUpdateEndpoint.expectedMessageCount(0);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent("none"));
        solrUpdateEndpoint.assertIsSatisfied();
    }

    @Test
    public void testCdrEventProcessor() throws Exception {
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(""));
        verify(cdrEventProcessor).process(any(Exchange.class));
    }

    @Test
    public void testSolrUpdateProcessor() throws Exception {
        createContext("CdrServiceCdrEventToSolrUpdateProcessor");
        template.sendBodyAndHeaders("", createEvent("action_placeholder"));
        verify(cdrEventToSolrUpdateProcessor).process(any(Exchange.class));
    }

    private void createContext(String routeName) throws Exception {
        AdviceWith.adviceWith(context, routeName, a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("*");
        });
        context.start();
    }

    private static Map<String, Object> createEvent(String action) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(CdrUpdateAction, action);

        return headers;
    }

}
