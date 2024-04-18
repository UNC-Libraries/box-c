package edu.unc.lib.boxc.services.camel.cdrEvents;

import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrUpdateAction;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.BeanInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions;
import edu.unc.lib.boxc.services.camel.cdrEvents.CdrEventProcessor;
import edu.unc.lib.boxc.services.camel.solr.CdrEventToSolrUpdateProcessor;

/**
 *
 * @author lfarrell
 *
 */
public class CdrEventRouterTest extends CamelSpringTestSupport {
    @Produce(uri = "direct:start")
    private ProducerTemplate template;

    @BeanInject(value = "cdrEventProcessor")
    private CdrEventProcessor cdrEventProcessor;

    @BeanInject(value = "cdrEventToSolrUpdateProcessor")
    private CdrEventToSolrUpdateProcessor cdrEventToSolrUpdateProcessor;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/cdr-events-context.xml");
    }

    @Test
    public void testMoveFilter() throws Exception {
        getMockEndpoint("mock:direct:solr-update").expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.MOVE.toString()));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRemoveFilter() throws Exception {
        getMockEndpoint("mock:direct:solr-update").expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.REMOVE.toString()));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testAddFilter() throws Exception {
        getMockEndpoint("mock:direct:solr-update").expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.ADD.toString()));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testMarkForDeletionFilter() throws Exception {
        getMockEndpoint("mock:direct:solr-update").expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.MARK_FOR_DELETION.toString()));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testEditAccessFilter() throws Exception {
        getMockEndpoint("mock:direct:solr-update").expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.EDIT_ACCESS_CONTROL.toString()));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testEditTypeFilter() throws Exception {
        getMockEndpoint("mock:direct:solr-update").expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.EDIT_TYPE.toString()));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFilterFail() throws Exception {
        getMockEndpoint("mock:direct:solr-update").expectedMessageCount(0);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent("none"));
        assertMockEndpointsSatisfied();
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
