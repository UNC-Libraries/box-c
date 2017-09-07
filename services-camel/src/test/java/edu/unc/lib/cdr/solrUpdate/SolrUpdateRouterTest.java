package edu.unc.lib.cdr.solrUpdate;

import static edu.unc.lib.cdr.JmsHeaderConstants.EVENT_TYPE;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.BeanInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.cdr.SolrUpdateProcessor;

public class SolrUpdateRouterTest extends CamelSpringTestSupport {
    private static final String SOLR_UPDATE_ROUTE = "CdrServiceSolrUpdate";

    @Produce(uri = "direct:start")
    private ProducerTemplate template;

    @BeanInject(value = "solrUpdateProcessor")
    private SolrUpdateProcessor solrUpdateProcessor;

    @Before
    public void init() {
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/solr-update-context.xml");
    }

    @Test
    public void testSolrUpdateRoute() throws Exception {
        createContext(SOLR_UPDATE_ROUTE);

        template.sendBodyAndHeaders("", createEvent());

        verify(solrUpdateProcessor).process(any(Exchange.class));
    }

    private void createContext(String routeName) throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("*");
            }
        });

        context.start();
    }

    private static Map<String, Object> createEvent() {

        final Map<String, Object> headers = new HashMap<>();
        headers.put(EVENT_TYPE, "ResourceCreation");

        return headers;
    }
}
