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

import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Container;
import static edu.unc.lib.dl.services.camel.JmsHeaderConstants.EVENT_TYPE;
import static edu.unc.lib.dl.services.camel.JmsHeaderConstants.IDENTIFIER;
import static edu.unc.lib.dl.services.camel.JmsHeaderConstants.RESOURCE_TYPE;
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

import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.services.camel.BinaryMetadataProcessor;
import edu.unc.lib.dl.services.camel.CleanupBinaryProcessor;
import edu.unc.lib.dl.services.camel.GetBinaryProcessor;

/**
 *
 * @author bbpennel
 * @author lfarrell
 *
 */
public class MetaServicesRouterTest extends CamelSpringTestSupport {

    private static final String FILE_ID = "/file1/original_file";
    private static final String FITS_ID = "/file1/techmd_fits";
    private static final String CONTAINER_ID = "/content/43/e2/27/ac/43e227ac-983a-4a18-94c9-c9cff8d28441";

    private static final String META_ROUTE = "CdrMetaServicesRouter";
    private static final String PROCESS_ENHANCEMENT_ROUTE = "ProcessEnhancement";
    private static final String ORIGINAL_BINARY_ROUTE = "ProcessOriginalBinary";
    private static final String ADD_BINARY_ENHANCEMENTS_ROUTE = "AddBinaryEnhancements";
    private static final String SOLR_INGEST_ROUTE = "IngestSolrIndexing";

    @PropertyInject(value = "fcrepo.baseUri")
    private static String baseUri;

    @EndpointInject(uri = "mock:fcrepo")
    private MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    private ProducerTemplate template;

    @BeanInject(value = "binaryMetadataProcessor")
    private BinaryMetadataProcessor mdProcessor;

    @BeanInject(value = "getBinaryProcessor")
    private GetBinaryProcessor getBinaryProcessor;

    @BeanInject(value = "cleanupBinaryProcessor")
    private CleanupBinaryProcessor cleanupBinaryProcessor;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/metaservices-context.xml");
    }

    @Test
    public void testRouteStartContainer() throws Exception {
        getMockEndpoint("mock:direct-vm:index.start").expectedMessageCount(1);
        getMockEndpoint("mock:direct:process.enhancement").expectedMessageCount(1);

        createContext(META_ROUTE);

        template.sendBodyAndHeaders("", createEvent(CONTAINER_ID, Binary.getURI()));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testProcessEnhancementContainer() throws Exception {
        getMockEndpoint("mock:direct:process.binary").expectedMessageCount(1);
        getMockEndpoint("mock:direct:process.solr").expectedMessageCount(1);

        createContext(PROCESS_ENHANCEMENT_ROUTE);

        final Map<String, Object> headers = createEvent(CONTAINER_ID, Binary.getURI());
        headers.put(RESOURCE_TYPE, Container.getURI());
        template.sendBodyAndHeaders("", headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testProcessFits() throws Exception {
        getMockEndpoint("mock:direct-vm:replication").expectedMessageCount(1);
        getMockEndpoint("mock:direct:process.enhancements").expectedMessageCount(0);

        createContext(ORIGINAL_BINARY_ROUTE);

        final Map<String, Object> headers = createEvent(FITS_ID, Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testEventTypeFilter() throws Exception {
        getMockEndpoint("mock:direct:process.binary").expectedMessageCount(0);
        getMockEndpoint("mock:direct:process.solr").expectedMessageCount(0);

        createContext(PROCESS_ENHANCEMENT_ROUTE);

        Map<String, Object> headers = createEvent(FILE_ID, Binary.getURI());
        headers.put(EVENT_TYPE, "ResourceDeletion");

        template.sendBodyAndHeaders("", headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testEventTypeFilterValid() throws Exception {
        getMockEndpoint("mock:direct:process.binary").expectedMessageCount(1);
        getMockEndpoint("mock:direct:process.solr").expectedMessageCount(1);

        createContext(PROCESS_ENHANCEMENT_ROUTE);
        Map<String, Object> headers = createEvent(FILE_ID, Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testEnhancementIdentifierFilterValid() throws Exception {
        getMockEndpoint("mock:direct-vm:imageEnhancements").expectedMessageCount(1);
        getMockEndpoint("mock:direct-vm:extractFulltext").expectedMessageCount(1);

        createContext(ADD_BINARY_ENHANCEMENTS_ROUTE);

        Map<String, Object> headers = createEvent(FILE_ID, Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testProcessBinaryOriginal() throws Exception {
        getMockEndpoint("mock:direct:process.enhancements").expectedMessageCount(1);
        getMockEndpoint("mock:direct-vm:replication").expectedMessageCount(1);

        createContext(ORIGINAL_BINARY_ROUTE);

        Map<String, Object> headers = createEvent(FILE_ID, Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        assertMockEndpointsSatisfied();

        verify(mdProcessor).process(any(Exchange.class));
        verify(getBinaryProcessor).process(any(Exchange.class));
        verify(cleanupBinaryProcessor).process(any(Exchange.class));
    }

    @Test
    public void testProcessBinaryOriginalFail() throws Exception {
        getMockEndpoint("mock:direct-vm:imageEnhancements").expectedMessageCount(0);
        getMockEndpoint("mock:direct-vm:extractFulltext").expectedMessageCount(0);

        createContext(ORIGINAL_BINARY_ROUTE);

        Map<String, Object> headers = createEvent("other_file", Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testProcessSolr() throws Exception {
        getMockEndpoint("mock:direct-vm:solrIndexing").expectedMessageCount(1);

        createContext(SOLR_INGEST_ROUTE);

        Map<String, Object> headers = createEvent(RESOURCE_TYPE, Cdr.Work.getURI());
        template.sendBodyAndHeaders("", headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testProcessSolrFail() throws Exception {
        getMockEndpoint("mock:direct-vm:solrIndexing").expectedMessageCount(0);

        createContext(SOLR_INGEST_ROUTE);

        Map<String, Object> headers = createEvent(RESOURCE_TYPE, Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testProcessFilterOutDescriptiveMDSolr() throws Exception {
        getMockEndpoint("mock:direct-vm:solrIndexing").expectedMessageCount(0);

        createContext(SOLR_INGEST_ROUTE);

        Map<String, Object> headers = createEvent("container",
                Cdr.FileObject.getURI(), Cdr.DescriptiveMetadata.getURI());
        template.sendBodyAndHeaders("", headers);

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
        headers.put(EVENT_TYPE, "ResourceCreation");
        headers.put(IDENTIFIER, identifier);
        headers.put(RESOURCE_TYPE, String.join(",", type));

        return headers;
    }
}
