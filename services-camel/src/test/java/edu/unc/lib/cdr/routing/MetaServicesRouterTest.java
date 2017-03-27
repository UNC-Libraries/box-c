/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.cdr.routing;

import static edu.unc.lib.cdr.JmsHeaderConstants.EVENT_TYPE;
import static edu.unc.lib.cdr.JmsHeaderConstants.IDENTIFIER;
import static edu.unc.lib.cdr.JmsHeaderConstants.RESOURCE_TYPE;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Container;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.cdr.BinaryMetadataProcessor;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;

/**
 * 
 * @author bbpennel
 *
 */
public class MetaServicesRouterTest extends CamelSpringTestSupport {

	private static final String FILE_ID = "/file1/original_file";
	private static final String CONTAINER_ID = "/content/43/e2/27/ac/43e227ac-983a-4a18-94c9-c\n" + 
			"9cff8d28441";

	private static final String META_ROUTE = "CdrMetaServicesRouter";
	private static final String ORIGINAL_BINARY_ROUTE = "ProcessOriginalBinary";

	@PropertyInject(value = "fcrepo.baseUri")
	private static String baseUri;

	@EndpointInject(uri = "mock:fcrepo")
	private MockEndpoint resultEndpoint;

	@BeanInject(value = "repository")
	private Repository repo;

	@Produce(uri = "direct:start")
	private ProducerTemplate template;

	@BeanInject(value = "binaryMetadataProcessor")
	private BinaryMetadataProcessor mdProcessor;

	@Before
	public void init() {
		PIDs.setRepository(repo);
		when(repo.getBaseUri()).thenReturn(baseUri);
	}

	@Override
	protected AbstractApplicationContext createApplicationContext() {
		return new ClassPathXmlApplicationContext("/service-context.xml", "/metaservices-context.xml");
	}

	@Test
	public void testRouteStartContainer() throws Exception {
		getMockEndpoint("mock:direct-vm:index.start").expectedMessageCount(1);
		getMockEndpoint("mock:direct:process.binary.original").expectedMessageCount(0);

		createContext(META_ROUTE);

		final Map<String, Object> headers = createEvent(CONTAINER_ID);
		headers.put(RESOURCE_TYPE, Container.getURI());
		template.sendBodyAndHeaders("", headers);

		assertMockEndpointsSatisfied();
	}

	@Test
	public void testRouteStartOriginalBinary() throws Exception {
		getMockEndpoint("mock:direct-vm:index.start").expectedMessageCount(1);
		getMockEndpoint("mock:direct:process.binary.original").expectedMessageCount(1);

		createContext(META_ROUTE);

		final Map<String, Object> headers = createEvent(FILE_ID);
		template.sendBodyAndHeaders("", headers);

		assertMockEndpointsSatisfied();
	}

	@Test
	public void testEventTypeFilter() throws Exception {
		getMockEndpoint("mock:direct-vm:index.start").expectedMessageCount(1);
		getMockEndpoint("mock:direct:process.binary.original").expectedMessageCount(0);

		createContext(META_ROUTE);

		Map<String, Object> headers = createEvent(FILE_ID);
		headers.put(EVENT_TYPE, "ResourceDeletion");

		template.sendBodyAndHeaders("", headers);

		assertMockEndpointsSatisfied();
	}

	@Test
	public void testIdentifierFilter() throws Exception {
		getMockEndpoint("mock:direct-vm:index.start").expectedMessageCount(1);
		getMockEndpoint("mock:direct:process.binary.original").expectedMessageCount(0);

		createContext(META_ROUTE);

		Map<String, Object> headers = createEvent("other_file");
		template.sendBodyAndHeaders("", headers);

		assertMockEndpointsSatisfied();
	}

	@Test
	public void testProcessBinaryOriginal() throws Exception {
		getMockEndpoint("mock:direct-vm:createThumbnail").expectedMessageCount(1);
		getMockEndpoint("mock:direct-vm:extractFulltext").expectedMessageCount(1);

		createContext(ORIGINAL_BINARY_ROUTE);

		Map<String, Object> headers = createEvent("other_file");
		template.sendBodyAndHeaders("", headers);

		assertMockEndpointsSatisfied();

		verify(mdProcessor).process(any(Exchange.class));
	}

	private void createContext(String routeName) throws Exception {
		context.getRouteDefinition(routeName).adviceWith((ModelCamelContext) context, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
				replaceFromWith("direct:start");
				mockEndpointsAndSkip("*");
			}
		});

		context.start();
	}

	private static Map<String, Object> createEvent(final String identifier) {
		final Map<String, Object> headers = new HashMap<>();
		headers.put(EVENT_TYPE, "ResourceCreation");
		headers.put(IDENTIFIER, identifier);
		headers.put(RESOURCE_TYPE, Binary.getURI());

		return headers;
	}
}
