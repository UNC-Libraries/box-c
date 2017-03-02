package edu.unc.lib.cdr.fulltext;

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

public class FulltextRouterTest extends CamelSpringTestSupport {
	private static final String EVENT_NS = "http://fedora.info/definitions/v4/event#";
	private static final String EVENT_TYPE = "org.fcrepo.jms.eventType";
	private static final String IDENTIFIER = "org.fcrepo.jms.identifier";
	private static final String RESOURCE_TYPE = "org.fcrepo.jms.resourceType";
	private static final long timestamp = 1428360320168L;
	private static final String userID = "bypassAdmin";
	private static final String userAgent = "curl/7.37.1";
	private static final String fileID = "/file1";
	private final String eventTypes = EVENT_NS + "ResourceCreation";
	private final String enhancementRoute = "CdrServiceFulltextExtraction";
	private final String extractionRoute = "HasFulltext"; 
	
	@PropertyInject(value = "fcrepo.baseUri")
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
		return new ClassPathXmlApplicationContext("/service-context.xml", "/fulltext-context.xml");
	}
	
	@Test
	public void testRouteStart() throws Exception {
		getMockEndpoint("mock:direct:fulltext.filter").expectedMessageCount(1);
		
		createContext(enhancementRoute);
		
		template.sendBodyAndHeaders("", createEvent(fileID, eventTypes));

		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void testEventTypeFilter() throws Exception {
		getMockEndpoint("mock:direct:fulltext.filter").expectedMessageCount(0);
		
		createContext(enhancementRoute);
		
		Map<String, Object> headers = createEvent(fileID, eventTypes);
		headers.put(EVENT_TYPE, "ResourceDeletion");
		
		template.sendBodyAndHeaders("", headers);

		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void testIdentifierFilter() throws Exception {
		getMockEndpoint("mock:direct:fulltext.filter").expectedMessageCount(0);
		
		createContext(enhancementRoute);

		Map<String, Object> headers = createEvent(fileID, eventTypes);
		headers.put(IDENTIFIER, "container");
		
		template.sendBodyAndHeaders("", headers);

		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void testResourceTypeFilter() throws Exception {
		getMockEndpoint("mock:direct:fulltext.filter").expectedMessageCount(0);
		
		createContext(enhancementRoute);

		Map<String, Object> headers = createEvent(fileID, eventTypes);
		headers.put(RESOURCE_TYPE, createResource( "http://bad.info/definitions/v9/repository#Fake" ).getURI());
		
		template.sendBodyAndHeaders("", headers);

		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void testFullTextExtractionFilterValidMimeType() throws Exception {
		getMockEndpoint("mock:direct:fulltext.extraction").expectedMessageCount(1);
		createContext(extractionRoute);
		
		template.sendBodyAndHeaders("", createEvent(fileID, eventTypes));
		
		assertMockEndpointsSatisfied();
	}
	
	@Test
	public void testFullTextExtractionFilterInvalidMimeType() throws Exception {
		getMockEndpoint("mock:direct:fulltext.extraction").expectedMessageCount(0);
		
		createContext(extractionRoute);
		
		Map<String, Object> headers = createEvent(fileID, eventTypes);
		headers.put(CdrBinaryMimeType, "image/png");
		
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
		headers.put(CdrBinaryMimeType, "text/plain");
		
		return headers;
	}
}
