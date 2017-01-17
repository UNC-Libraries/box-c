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
import static edu.unc.lib.dl.rdf.Premis.hasMessageDigest;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.BeanInject;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.test.spring.UseAdviceWith;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fedora.PID;

@UseAdviceWith
public class ThumbnailRouterTest extends CamelBlueprintTestSupport {

	private static final String EVENT_NS = "http://fedora.info/definitions/v4/event#";
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

	@Override
	protected String getBlueprintDescriptor() {
		return "/OSGI-INF/blueprint/blueprint-test.xml";
	}

	@Override
	protected CamelContext createCamelContext() throws Exception {
		final CamelContext ctx = getOsgiService(CamelContext.class, "(camel.context.name=CdrServiceEnhancements)",
				10000);
		ctx.addComponent("activemq", ctx.getComponent("seda"));
		context = (ModelCamelContext) ctx;

		return context;
	}

	@Before
	public void init() {
		PIDs.setRepository(repo);
		when(repo.getFedoraBase()).thenReturn(baseUri);
	}
	
	@Test
	public void testRoute() throws Exception {
		final String eventTypes = EVENT_NS + "ResourceCreation";
		
		//MockEndpoint mock = getMockEndpoint("direct:fcrepo");
		//resultEndpoint.expectedMessageCount(1);
		getMockEndpoint("mock:fcrepo:{{fcrepo.baseUri}}").expectedMessageCount(1);

		context.getRouteDefinition("SCdrThumbnailEnhancement").adviceWith(context, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
				mockEndpointsAndSkip("fcrepo*");
				weaveById("simpleBinaryMetadataProcessor");
			}
		});
		context.start();

		// Construct a model for the test object
		PID pid = PIDs.get(UUID.randomUUID().toString());
		Model model = ModelFactory.createDefaultModel();
		Resource resc = model.createResource(pid.getRepositoryPath());
		resc.addProperty(hasMimeType, "text/plain");
		resc.addProperty(hasMessageDigest, "123456789");
		
		// Serialize the object as a string so that the processor can receive it
		String body;
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			//RDFDataMgr.write(bos, model, RDFFormat.TURTLE_PRETTY);
			body = new String(bos.toByteArray(), "UTF-8");
		}
		
	
		template.sendBodyAndHeaders("activemq:topic:fedora2",  body, createEvent(fileID, eventTypes));
		
		assertMockEndpointsSatisfied();
	}

	private static Map<String, Object> createEvent(final String identifier, final String eventTypes) {

		final Map<String, Object> headers = new HashMap<>();
//		headers.put(JmsHeaders.BASE_URL, baseUri);
//		headers.put(JmsHeaders.IDENTIFIER, identifier);
//		headers.put(JmsHeaders.TIMESTAMP, timestamp);
//		headers.put(JmsHeaders.USER, userID);
//		headers.put(JmsHeaders.USER_AGENT, userAgent);
//		headers.put(JmsHeaders.EVENT_TYPE, eventTypes);
//		headers.put(FcrepoHeaders.FCREPO_IDENTIFIER, identifier);
//		headers.put(FcrepoHeaders.FCREPO_BASE_URL, baseUri);
		return headers;
	}
}
