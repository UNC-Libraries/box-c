package edu.unc.lib.boxc.services.camel.solr;

import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.EVENT_TYPE;
import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.IDENTIFIER;
import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.RESOURCE_TYPE;
import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.Container;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.BeanInject;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * @author lfarrell
 *
 */
public class SolrRouterTest extends CamelSpringTestSupport {
    @PropertyInject(value = "fcrepo.baseUri")
    private static String baseUri;

    @EndpointInject(uri = "mock:fcrepo")
    private MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    private ProducerTemplate template;

    @BeanInject(value = "solrIngestProcessor")
    private SolrIngestProcessor solrIngestProcessor;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/solr-ingest-context.xml");
    }

    @Test
    public void testSolrIngestValidObjectType() throws Exception {
        createContext("CdrServiceSolr");

        template.sendBodyAndHeaders("", createEvent());

        verify(solrIngestProcessor).process(any(Exchange.class));
    }

    private void createContext(String routeName) throws Exception {
        AdviceWith.adviceWith(context, routeName, a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("*");
        });
        context.start();
    }

    private static Map<String, Object> createEvent() {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(EVENT_TYPE, "ResourceCreation");
        headers.put(IDENTIFIER, "original_file");
        headers.put(RESOURCE_TYPE, Container.getURI());

        return headers;
    }

}
