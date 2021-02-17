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
package edu.unc.lib.dl.services.camel.routing;

import static edu.unc.lib.dl.fcrepo4.FcrepoJmsConstants.BASE_URL;
import static edu.unc.lib.dl.fcrepo4.FcrepoJmsConstants.EVENT_TYPE;
import static edu.unc.lib.dl.fcrepo4.FcrepoJmsConstants.IDENTIFIER;
import static edu.unc.lib.dl.fcrepo4.FcrepoJmsConstants.RESOURCE_TYPE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.FCR_VERSIONS;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Container;
import static edu.unc.lib.dl.services.camel.util.EventTypes.EVENT_CREATE;
import static edu.unc.lib.dl.services.camel.util.EventTypes.EVENT_UPDATE;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.test.TestHelper;

/**
 *
 * @author bbpennel
 * @author lfarrell
 *
 */
public class MetaServicesRouterTest extends CamelSpringTestSupport {

    private static final String CONTAINER_ID = "/content/43/e2/27/ac/43e227ac-983a-4a18-94c9-c9cff8d28441";
    private static final String FILE_ID = CONTAINER_ID + "/file1/original_file";
    private static final String DEPOSIT_ID = "/deposit/43/e2/27/ac/43e227ac-983a-4a18-94c9-c9cff8d28441";

    private static final String META_ROUTE = "CdrMetaServicesRouter";
    private static final String PROCESS_ENHANCEMENT_ROUTE = "ProcessEnhancement";

    @PropertyInject(value = "fcrepo.baseUri")
    private static String baseUri;

    @EndpointInject(uri = "mock:fcrepo")
    private MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    private ProducerTemplate template;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/metaservices-context.xml");
    }

    @Before
    public void setup() {
        TestHelper.setContentBase("http://example.com/rest/");
    }

    @Test
    public void testRouteStartContainer() throws Exception {
        getMockEndpoint("mock:direct-vm:index.start").expectedMessageCount(1);
        getMockEndpoint("mock:direct-vm:filter.longleaf").expectedMessageCount(0);
        getMockEndpoint("mock:direct:process.enhancement").expectedMessageCount(1);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenDone(1).create();
        template.sendBodyAndHeaders("", createEvent(CONTAINER_ID, Container.getURI()));
        notify.matches(1l, TimeUnit.SECONDS);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRouteStartTimemap() throws Exception {
        getMockEndpoint("mock:direct-vm:index.start").expectedMessageCount(0);
        getMockEndpoint("mock:direct-vm:filter.longleaf").expectedMessageCount(0);
        getMockEndpoint("mock:direct:process.enhancement").expectedMessageCount(0);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenDone(1).create();
        template.sendBodyAndHeaders("", createEvent(CONTAINER_ID + "/" + FCR_VERSIONS,
                Fcrepo4Repository.Container.getURI()));
        notify.matches(1l, TimeUnit.SECONDS);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRouteStartDatafs() throws Exception {
        getMockEndpoint("mock:direct-vm:index.start").expectedMessageCount(1);
        getMockEndpoint("mock:direct-vm:filter.longleaf").expectedMessageCount(0);
        getMockEndpoint("mock:direct:process.enhancement").expectedMessageCount(0);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenDone(1).create();
        template.sendBodyAndHeaders("", createEvent(CONTAINER_ID + "/datafs",
                Fcrepo4Repository.Container.getURI()));
        notify.matches(1l, TimeUnit.SECONDS);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRouteStartDepositRecord() throws Exception {
        getMockEndpoint("mock:direct-vm:index.start").expectedMessageCount(1);
        getMockEndpoint("mock:direct-vm:filter.longleaf").expectedMessageCount(0);
        getMockEndpoint("mock:direct:process.enhancement").expectedMessageCount(0);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenDone(1).create();
        template.sendBodyAndHeaders("", createEvent(DEPOSIT_ID,
                Fcrepo4Repository.Container.getURI()));
        notify.matches(1l, TimeUnit.SECONDS);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRouteStartNotAPid() throws Exception {
        getMockEndpoint("mock:direct-vm:index.start").expectedMessageCount(1);
        getMockEndpoint("mock:direct-vm:filter.longleaf").expectedMessageCount(0);
        getMockEndpoint("mock:direct:process.enhancement").expectedMessageCount(0);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenDone(1).create();
        template.sendBodyAndHeaders("", createEvent("what/is/going/on",
                Fcrepo4Repository.Container.getURI()));
        notify.matches(1l, TimeUnit.SECONDS);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRouteStartCollections() throws Exception {
        getMockEndpoint("mock:direct-vm:index.start").expectedMessageCount(1);
        getMockEndpoint("mock:direct-vm:filter.longleaf").expectedMessageCount(0);
        getMockEndpoint("mock:direct:process.enhancement").expectedMessageCount(0);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenDone(1).create();
        template.sendBodyAndHeaders("", createEvent("/content/" + RepositoryPathConstants.CONTENT_ROOT_ID,
                Fcrepo4Repository.Container.getURI()));
        notify.matches(1l, TimeUnit.SECONDS);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRouteStartBinaryMetadata() throws Exception {
        getMockEndpoint("mock:direct-vm:index.start").expectedMessageCount(1);
        getMockEndpoint("mock:direct-vm:filter.longleaf").expectedMessageCount(0);
        getMockEndpoint("mock:direct:process.enhancement").expectedMessageCount(0);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenDone(1).create();
        template.sendBodyAndHeaders("", createEvent(CONTAINER_ID + "/datafs/original_file/fcr:metadata",
                Fcrepo4Repository.NonRdfSourceDescription.getURI()));
        notify.matches(1l, TimeUnit.SECONDS);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRouteStartOriginalBinary() throws Exception {
        getMockEndpoint("mock:direct-vm:index.start").expectedMessageCount(1);
        getMockEndpoint("mock:direct-vm:filter.longleaf").expectedMessageCount(1);
        getMockEndpoint("mock:direct:process.enhancement").expectedMessageCount(1);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenDone(1).create();
        template.sendBodyAndHeaders("", createEvent(CONTAINER_ID + "/datafs/original_file",
                Fcrepo4Repository.Binary.getURI()));
        notify.matches(1l, TimeUnit.SECONDS);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testRouteStartPremisBinary() throws Exception {
        getMockEndpoint("mock:direct-vm:index.start").expectedMessageCount(1);
        getMockEndpoint("mock:direct-vm:filter.longleaf").expectedMessageCount(1);
        getMockEndpoint("mock:direct:process.enhancement").expectedMessageCount(0);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenDone(1).create();
        template.sendBodyAndHeaders("", createEvent(CONTAINER_ID + "/md/event_log",
                Fcrepo4Repository.Binary.getURI()));
        notify.matches(1l, TimeUnit.SECONDS);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testEventTypeFilter() throws Exception {
        getMockEndpoint("mock:direct-vm:process.creation").expectedMessageCount(0);

        createContext(PROCESS_ENHANCEMENT_ROUTE);

        Map<String, Object> headers = createEvent(FILE_ID, Binary.getURI());
        headers.put(EVENT_TYPE, "ResourceDeletion");

        NotifyBuilder notify = new NotifyBuilder(context).whenDone(1).create();
        template.sendBodyAndHeaders("", headers);
        notify.matches(1l, TimeUnit.SECONDS);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testEventTypeFilterValid() throws Exception {
        getMockEndpoint("mock:direct:process.enhancement").expectedMessageCount(1);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenDone(1).create();
        Map<String, Object> headers = createEvent(FILE_ID, Binary.getURI());
        template.sendBodyAndHeaders("", headers);
        notify.matches(1l, TimeUnit.SECONDS);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testUpdateNonBinary() throws Exception {
        getMockEndpoint("mock:direct-vm:filter.longleaf").expectedMessageCount(0);
        getMockEndpoint("mock:direct-vm:process.creation").expectedMessageCount(0);

        createContext(PROCESS_ENHANCEMENT_ROUTE);
        Map<String, Object> headers = createEvent("/not/binary", Container.getURI());
        headers.put(EVENT_TYPE, EVENT_UPDATE);

        NotifyBuilder notify = new NotifyBuilder(context).whenDone(1).create();
        template.sendBodyAndHeaders("", headers);
        notify.matches(1l, TimeUnit.SECONDS);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testCreationRoute() throws Exception {
        getMockEndpoint("mock:{{cdr.enhancement.stream.camel}}").expectedMessageCount(1);

        createContext(PROCESS_ENHANCEMENT_ROUTE);
        Map<String, Object> headers = createEvent(FILE_ID, Binary.getURI());

        NotifyBuilder notify = new NotifyBuilder(context).whenDone(1).create();
        template.sendBodyAndHeaders("", headers);
        notify.matches(1l, TimeUnit.SECONDS);

        assertMockEndpointsSatisfied();
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

    private static Map<String, Object> createEvent(final String identifier, final String... type) {

        final Map<String, Object> headers = new HashMap<>();
        headers.put(EVENT_TYPE, EVENT_CREATE);
        headers.put(IDENTIFIER, identifier);
        headers.put(BASE_URL, baseUri);
        headers.put(RESOURCE_TYPE, String.join(",", type));

        return headers;
    }
}
