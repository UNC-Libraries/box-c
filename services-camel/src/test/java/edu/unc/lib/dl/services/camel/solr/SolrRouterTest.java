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
package edu.unc.lib.dl.services.camel.solr;

import static edu.unc.lib.dl.services.camel.JmsHeaderConstants.EVENT_TYPE;
import static edu.unc.lib.dl.services.camel.JmsHeaderConstants.IDENTIFIER;
import static edu.unc.lib.dl.services.camel.JmsHeaderConstants.RESOURCE_TYPE;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Container;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.BeanInject;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
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
        headers.put(IDENTIFIER, "original_file");
        headers.put(RESOURCE_TYPE, Container.getURI());

        return headers;
    }

}
