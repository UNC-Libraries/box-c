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
package edu.unc.lib.cdr.thumbnail;

import static edu.unc.lib.dl.rdf.Ebucore.hasMimeType;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.dl.rdf.Premis.hasMessageDigest;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_AGENT;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_DATE_TIME;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.BeanInject;
import org.apache.camel.EndpointInject;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fedora.PID;

public class ThumbnailRouterTest extends CamelSpringTestSupport {
	private static final String EVENT_NS = "http://fedora.info/definitions/v4/event#";
	private static final String EVENT_TYPE = "org.fcrepo.jms.eventType";
	private static final String IDENTIFIER = "org.fcrepo.jms.identifier";
	private static final String RESOURCE_TYPE = "org.fcrepo.jms.resourceType";
	private static final long timestamp = 1428360320168L;
	private static final String userID = "bypassAdmin";
	private static final String userAgent = "curl/7.37.1";
	private static final String fileID = "/file1";
	
	@PropertyInject(value = "fcrepo.baseUri")
	private static String baseUri;
	
	@EndpointInject(uri = "mock:fcrepo")
	protected MockEndpoint resultEndpoint;
	
	@BeanInject(value = "repository")
	private Repository repo;
	
	@Before
	public void init() {
		PIDs.setRepository(repo);
		when(repo.getFedoraBase()).thenReturn(baseUri);
	}
	
	@Override
	protected AbstractApplicationContext createApplicationContext() {
		return new ClassPathXmlApplicationContext("/service-context.xml");
	}
	
	@Test
	public void testRoute() throws Exception {
		final String eventTypes = EVENT_NS + "ResourceCreation";

		getMockEndpoint("mock:fcrepo:{{fcrepo.baseUri}}").expectedMessageCount(1);
		
		context.getRouteDefinitions().get(0).adviceWith((ModelCamelContext) context, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
				mockEndpointsAndSkip("fcrepo*");
				weaveById("simpleBinaryMetadataProcessor");
			}
		});
		
		context.start();
		
		PID pid = PIDs.get(UUID.randomUUID().toString());
		Model model = ModelFactory.createDefaultModel();
		Resource resc = model.createResource(pid.getRepositoryPath());
		resc.addProperty(hasMimeType, "application/octet-stream");
		resc.addProperty(hasMessageDigest, "123456789");
		
		// Serialize the object as a string so that the processor can receive it
		String body = null;
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			//RDFDataMgr.write(bos, model, RDFFormat.TURTLE_PRETTY);
			body = new String(bos.toByteArray(), "UTF-8");
		}
		
		template.sendBodyAndHeaders("direct-vm:createThumbnail", body, createEvent(fileID, eventTypes));

		assertMockEndpointsSatisfied();
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
		
		return headers;
	}
}
