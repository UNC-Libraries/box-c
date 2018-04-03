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
package edu.unc.lib.dl.services.camel.cdrEvents;

import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrUpdateAction;
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
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.dl.services.camel.solr.CdrEventToSolrUpdateProcessor;
import edu.unc.lib.dl.util.JMSMessageUtil.CDRActions;

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
        template.sendBodyAndHeaders("", createEvent(CDRActions.MOVE.getName()));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRemoveFilter() throws Exception {
        getMockEndpoint("mock:direct:solr-update").expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.REMOVE.getName()));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testAddFilter() throws Exception {
        getMockEndpoint("mock:direct:solr-update").expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.ADD.getName()));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testReorderFilter() throws Exception {
        getMockEndpoint("mock:direct:solr-update").expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.REORDER.getName()));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testPublishFilter() throws Exception {
        getMockEndpoint("mock:direct:solr-update").expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.PUBLISH.getName()));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testEditTypeFilter() throws Exception {
        getMockEndpoint("mock:direct:solr-update").expectedMessageCount(1);
        createContext("CdrServiceCdrEvents");
        template.sendBodyAndHeaders("", createEvent(CDRActions.EDIT_TYPE.getName()));
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
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("*");
            }
        });

        context.start();
    }

    private static Map<String, Object> createEvent(String action) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(CdrUpdateAction, action);

        return headers;
    }

}
