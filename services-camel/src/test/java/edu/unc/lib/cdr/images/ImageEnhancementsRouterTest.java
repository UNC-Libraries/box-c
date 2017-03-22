/**
 * Copyright 2016 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.cdr.images;

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_AGENT;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_DATE_TIME;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.BeanInject;
import org.apache.camel.EndpointInject;
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

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;

public class ImageEnhancementsRouterTest extends CamelSpringTestSupport {
	private static final String EVENT_NS = "http://fedora.info/definitions/v4/event#";
	private static final String EVENT_TYPE = "org.fcrepo.jms.eventType";
	private static final String IDENTIFIER = "org.fcrepo.jms.identifier";
	private static final String RESOURCE_TYPE = "org.fcrepo.jms.resourceType";
	private static final long timestamp = 1428360320168L;
	private static final String userID = "bypassAdmin";
	private static final String userAgent = "curl/7.37.1";
	private static final String fileID = "/file1";
	private final String eventTypes = EVENT_NS + "ResourceCreation";
	private final String enhancementRoute = "CdrServiceEnhancements";
	private final String isImageRoute = "IsImage"; 
	
	@PropertyInject(value = "fcrepo.baseUrl")
	private static String baseUri;
	
	@EndpointInject(uri = "mock:fcrepo")
	protected MockEndpoint resultEndpoint;
	
	@BeanInject(value = "repository")
	private Repository repo;
	
	@Produce(uri = "direct:start")
	protected ProducerTemplate template;
	
	@Before
	public void init() {
		PIDs.setRepository(repo);
		when(repo.getBaseUri()).thenReturn(baseUri);
	}
	
	@Override
	protected AbstractApplicationContext createApplicationContext() {
		return new ClassPathXmlApplicationContext("/service-context.xml", "/thumbnail-context.xml");
	}
	
	@Test
	public void testRouteStartSuccess() throws Exception {
		getMockEndpoint("mock:direct:images").expectedMessageCount(1);
		
		createContext(enhancementRoute);
		
		template.sendBodyAndHeaders("", createEvent(fileID, eventTypes));

		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void testEventTypeFilter() throws Exception {
		getMockEndpoint("mock:direct:images").expectedMessageCount(0);
		
		createContext(enhancementRoute);
		
		Map<String, Object> headers = createEvent(fileID, eventTypes);
		headers.put(EVENT_TYPE, "ResourceDeletion");
		
		template.sendBodyAndHeaders("", headers);

		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void testIdentifierFilter() throws Exception {
		getMockEndpoint("mock:direct:images").expectedMessageCount(0);
		
		createContext(enhancementRoute);

		Map<String, Object> headers = createEvent(fileID, eventTypes);
		headers.put(IDENTIFIER, "container");
		
		template.sendBodyAndHeaders("", headers);

		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void testResourceTypeFilter() throws Exception {
		getMockEndpoint("mock:direct:images").expectedMessageCount(0);
		
		createContext(enhancementRoute);

		Map<String, Object> headers = createEvent(fileID, eventTypes);
		headers.put(RESOURCE_TYPE, createResource( "http://bad.info/definitions/v9/repository#Fake" ).getURI());
		
		template.sendBodyAndHeaders("", headers);

		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void testRouteMulticastSuccess() throws Exception {
		createContext(isImageRoute);
		
		getMockEndpoint("mock:direct:small.thumbnail").expectedMessageCount(1);
		getMockEndpoint("mock:direct:large.thumbnail").expectedMessageCount(1);
		getMockEndpoint("mock:direct:accessImage").expectedMessageCount(1);
		template.sendBodyAndHeaders("", createEvent(fileID, eventTypes));
		
		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void testRouteMulticastFilter() throws Exception {
		createContext(isImageRoute);
		
		getMockEndpoint("mock:direct:small.thumbnail").expectedMessageCount(0);
		getMockEndpoint("mock:direct:large.thumbnail").expectedMessageCount(0);
		getMockEndpoint("mock:direct:accessImage").expectedMessageCount(0);
		
		Map<String, Object> headers = createEvent(fileID, eventTypes);
		headers.put(CdrBinaryMimeType, "plain/text");
		
		template.sendBodyAndHeaders("", headers);
		
		assertMockEndpointsSatisfied();
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
	
	private static Map<String, Object> createEvent(final String identifier, final String eventTypes) {
		final Map<String, Object> headers = new HashMap<>();
		headers.put(FCREPO_URI, identifier);
		headers.put(FCREPO_DATE_TIME, timestamp);
		headers.put(FCREPO_AGENT, Arrays.asList(userID, userAgent));
		headers.put(FCREPO_EVENT_TYPE, eventTypes);
		headers.put(FCREPO_BASE_URL, baseUri);
		headers.put(EVENT_TYPE, "ResourceCreation");
		headers.put(IDENTIFIER, "original_file");
		headers.put(RESOURCE_TYPE, Binary.getURI());
		headers.put(CdrBinaryMimeType, "image/png");
		
		return headers;
	}
}
