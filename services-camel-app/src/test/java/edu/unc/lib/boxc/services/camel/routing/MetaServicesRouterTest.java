package edu.unc.lib.boxc.services.camel.routing;

import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.BASE_URL;
import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.EVENT_TYPE;
import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.IDENTIFIER;
import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.RESOURCE_TYPE;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.FCR_VERSIONS;
import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.Container;
import static edu.unc.lib.boxc.services.camel.util.EventTypes.EVENT_CREATE;
import static edu.unc.lib.boxc.services.camel.util.EventTypes.EVENT_UPDATE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;

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

    @BeforeEach
    public void setup() {
        TestHelper.setContentBase("http://example.com/rest/");
    }

    @Test
    public void testRouteStartContainer() throws Exception {
        var indexStartEndpoint = getMockEndpoint("mock:direct-vm:index.start");
        indexStartEndpoint.expectedMessageCount(1);
        var filterLongleafEndpoint = getMockEndpoint("mock:direct-vm:filter.longleaf");
        filterLongleafEndpoint.expectedMessageCount(0);
        var processEnhEndpoint = getMockEndpoint("mock:direct:process.enhancement");
        processEnhEndpoint.expectedMessageCount(1);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenCompleted(1).create();
        template.sendBodyAndHeaders("", createEvent(CONTAINER_ID, Container.getURI()));
        notify.matches(1l, TimeUnit.SECONDS);

        indexStartEndpoint.assertIsSatisfied();
        filterLongleafEndpoint.assertIsSatisfied();
        processEnhEndpoint.assertIsSatisfied();
    }

    @Test
    public void testRouteStartTimemap() throws Exception {
        var indexStartEndpoint = getMockEndpoint("mock:direct-vm:index.start");
        indexStartEndpoint.expectedMessageCount(0);
        var filterLongleafEndpoint = getMockEndpoint("mock:direct-vm:filter.longleaf");
        filterLongleafEndpoint.expectedMessageCount(0);
        var processEnhEndpoint = getMockEndpoint("mock:direct:process.enhancement");
        processEnhEndpoint.expectedMessageCount(0);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenCompleted(1).create();
        template.sendBodyAndHeaders("", createEvent(CONTAINER_ID + "/" + FCR_VERSIONS,
                Fcrepo4Repository.Container.getURI()));
        // delay so that message has time to reach all routes that expect 0
        Thread.sleep(100);
        notify.matches(1l, TimeUnit.SECONDS);

        indexStartEndpoint.assertIsSatisfied();
        filterLongleafEndpoint.assertIsSatisfied();
        processEnhEndpoint.assertIsSatisfied();
    }

    @Test
    public void testRouteStartDatafs() throws Exception {
        var indexStartEndpoint = getMockEndpoint("mock:direct-vm:index.start");
        indexStartEndpoint.expectedMessageCount(1);
        var filterLongleafEndpoint = getMockEndpoint("mock:direct-vm:filter.longleaf");
        filterLongleafEndpoint.expectedMessageCount(0);
        var processEnhEndpoint = getMockEndpoint("mock:direct:process.enhancement");
        processEnhEndpoint.expectedMessageCount(0);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenCompleted(1).create();
        template.sendBodyAndHeaders("", createEvent(CONTAINER_ID + "/datafs",
                Fcrepo4Repository.Container.getURI()));
        // delay so that message has time to reach all routes that expect 0
        Thread.sleep(100);
        notify.matches(1l, TimeUnit.SECONDS);

        indexStartEndpoint.assertIsSatisfied();
        filterLongleafEndpoint.assertIsSatisfied();
        processEnhEndpoint.assertIsSatisfied();
    }

    @Test
    public void testRouteStartDepositRecord() throws Exception {
        var indexStartEndpoint = getMockEndpoint("mock:direct-vm:index.start");
        indexStartEndpoint.expectedMessageCount(1);
        var filterLongleafEndpoint = getMockEndpoint("mock:direct-vm:filter.longleaf");
        filterLongleafEndpoint.expectedMessageCount(0);
        var processEnhEndpoint = getMockEndpoint("mock:direct:process.enhancement");
        processEnhEndpoint.expectedMessageCount(0);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenCompleted(1).create();
        template.sendBodyAndHeaders("", createEvent(DEPOSIT_ID,
                Fcrepo4Repository.Container.getURI()));
        // delay so that message has time to reach all routes that expect 0
        Thread.sleep(100);
        notify.matches(1l, TimeUnit.SECONDS);

        indexStartEndpoint.assertIsSatisfied();
        filterLongleafEndpoint.assertIsSatisfied();
        processEnhEndpoint.assertIsSatisfied();
    }

    @Test
    public void testRouteStartNotAPid() throws Exception {
        var indexStartEndpoint = getMockEndpoint("mock:direct-vm:index.start");
        indexStartEndpoint.expectedMessageCount(1);
        var filterLongleafEndpoint = getMockEndpoint("mock:direct-vm:filter.longleaf");
        filterLongleafEndpoint.expectedMessageCount(0);
        var processEnhEndpoint = getMockEndpoint("mock:direct:process.enhancement");
        processEnhEndpoint.expectedMessageCount(0);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenCompleted(1).create();
        template.sendBodyAndHeaders("", createEvent("what/is/going/on",
                Fcrepo4Repository.Container.getURI()));
        // delay so that message has time to reach all routes that expect 0
        Thread.sleep(100);
        notify.matches(1l, TimeUnit.SECONDS);

        indexStartEndpoint.assertIsSatisfied();
        filterLongleafEndpoint.assertIsSatisfied();
        processEnhEndpoint.assertIsSatisfied();
    }

    @Test
    public void testRouteStartCollections() throws Exception {
        var indexStartEndpoint = getMockEndpoint("mock:direct-vm:index.start");
        indexStartEndpoint.expectedMessageCount(1);
        var filterLongleafEndpoint = getMockEndpoint("mock:direct-vm:filter.longleaf");
        filterLongleafEndpoint.expectedMessageCount(0);
        var processEnhEndpoint = getMockEndpoint("mock:direct:process.enhancement");
        processEnhEndpoint.expectedMessageCount(1);
        // Enhancements call for root obj to trigger indexing

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenCompleted(1).create();
        template.sendBodyAndHeaders("", createEvent("/content/" + RepositoryPathConstants.CONTENT_ROOT_ID,
                Fcrepo4Repository.Container.getURI()));
        notify.matches(1l, TimeUnit.SECONDS);

        indexStartEndpoint.assertIsSatisfied();
        filterLongleafEndpoint.assertIsSatisfied();
        processEnhEndpoint.assertIsSatisfied();
    }

    @Test
    public void testRouteStartBinaryMetadata() throws Exception {
        var indexStartEndpoint = getMockEndpoint("mock:direct-vm:index.start");
        indexStartEndpoint.expectedMessageCount(1);
        var filterLongleafEndpoint = getMockEndpoint("mock:direct-vm:filter.longleaf");
        filterLongleafEndpoint.expectedMessageCount(0);
        var processEnhEndpoint = getMockEndpoint("mock:direct:process.enhancement");
        processEnhEndpoint.expectedMessageCount(0);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenCompleted(1).create();
        // fcr:metadata nodes come through as Binaries with an internal modeshape path as the identifier
        Map<String, Object> eventMap = createEvent(CONTAINER_ID + "/datafs/original_file/fcr:metadata",
                Fcrepo4Repository.Binary.getURI());
        eventMap.put(IDENTIFIER, CONTAINER_ID + "/datafs/original_file/fedora:metadata");
        template.sendBodyAndHeaders("", eventMap);
        // delay so that message has time to reach all routes that expect 0
        Thread.sleep(100);

        notify.matches(1l, TimeUnit.SECONDS);

        indexStartEndpoint.assertIsSatisfied();
        filterLongleafEndpoint.assertIsSatisfied();
        processEnhEndpoint.assertIsSatisfied();
    }

    @Test
    public void testRouteStartOriginalBinary() throws Exception {
        var indexStartEndpoint = getMockEndpoint("mock:direct-vm:index.start");
        indexStartEndpoint.expectedMessageCount(1);
        var filterLongleafEndpoint = getMockEndpoint("mock:direct-vm:filter.longleaf");
        filterLongleafEndpoint.expectedMessageCount(1);
        var processEnhEndpoint = getMockEndpoint("mock:direct:process.enhancement");
        processEnhEndpoint.expectedMessageCount(1);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenCompleted(1).create();
        template.sendBodyAndHeaders("", createEvent(CONTAINER_ID + "/datafs/original_file",
                Fcrepo4Repository.Binary.getURI()));
        notify.matches(1l, TimeUnit.SECONDS);

        indexStartEndpoint.assertIsSatisfied();
        filterLongleafEndpoint.assertIsSatisfied();
        processEnhEndpoint.assertIsSatisfied();
    }

    @Test
    public void testRouteStartPremisBinary() throws Exception {
        var indexStartEndpoint = getMockEndpoint("mock:direct-vm:index.start");
        indexStartEndpoint.expectedMessageCount(1);
        var filterLongleafEndpoint = getMockEndpoint("mock:direct-vm:filter.longleaf");
        filterLongleafEndpoint.expectedMessageCount(1);
        var processEnhEndpoint = getMockEndpoint("mock:direct:process.enhancement");
        processEnhEndpoint.expectedMessageCount(0);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenCompleted(1).create();
        template.sendBodyAndHeaders("", createEvent(CONTAINER_ID + "/md/event_log",
                Fcrepo4Repository.Binary.getURI()));
        // delay so that message has time to reach all routes that expect 0
        Thread.sleep(100);
        notify.matches(1l, TimeUnit.SECONDS);

        indexStartEndpoint.assertIsSatisfied();
        filterLongleafEndpoint.assertIsSatisfied();
        processEnhEndpoint.assertIsSatisfied();
    }

    @Test
    public void testEventTypeFilter() throws Exception {
        var processCreationEndpoint = getMockEndpoint("mock:direct-vm:process.creation");
        processCreationEndpoint.expectedMessageCount(0);

        createContext(PROCESS_ENHANCEMENT_ROUTE);

        Map<String, Object> headers = createEvent(FILE_ID, Binary.getURI());
        headers.put(EVENT_TYPE, "ResourceDeletion");

        NotifyBuilder notify = new NotifyBuilder(context).whenCompleted(1).create();
        template.sendBodyAndHeaders("", headers);
        notify.matches(1l, TimeUnit.SECONDS);

        processCreationEndpoint.assertIsSatisfied();
    }

    @Test
    public void testEventTypeFilterValid() throws Exception {
        var processEnhEndpoint = getMockEndpoint("mock:direct:process.enhancement");
        processEnhEndpoint.expectedMessageCount(1);

        createContext(META_ROUTE);

        NotifyBuilder notify = new NotifyBuilder(context).whenCompleted(1).create();
        Map<String, Object> headers = createEvent(FILE_ID, Binary.getURI());
        template.sendBodyAndHeaders("", headers);
        notify.matches(1l, TimeUnit.SECONDS);

        processEnhEndpoint.assertIsSatisfied();
    }

    @Test
    public void testUpdateNonBinary() throws Exception {
        var filterLongleafEndpoint = getMockEndpoint("mock:direct-vm:filter.longleaf");
        filterLongleafEndpoint.expectedMessageCount(0);
        var processCreationEndpoint = getMockEndpoint("mock:direct:process.creation");
        processCreationEndpoint.expectedMessageCount(0);

        createContext(PROCESS_ENHANCEMENT_ROUTE);
        Map<String, Object> headers = createEvent("/not/binary", Container.getURI());
        headers.put(EVENT_TYPE, EVENT_UPDATE);

        NotifyBuilder notify = new NotifyBuilder(context).whenCompleted(1).create();
        template.sendBodyAndHeaders("", headers);
        notify.matches(1l, TimeUnit.SECONDS);

        filterLongleafEndpoint.assertIsSatisfied();
        processCreationEndpoint.assertIsSatisfied();
    }

    @Test
    public void testCreationRoute() throws Exception {
        var enhStreamEndpoint = getMockEndpoint("mock:{{cdr.enhancement.stream.camel}}");
        enhStreamEndpoint.expectedMessageCount(1);

        createContext(PROCESS_ENHANCEMENT_ROUTE);
        Map<String, Object> headers = createEvent(FILE_ID, Binary.getURI());

        NotifyBuilder notify = new NotifyBuilder(context).whenCompleted(1).create();
        template.sendBodyAndHeaders("", headers);
        notify.matches(1l, TimeUnit.SECONDS);

        enhStreamEndpoint.assertIsSatisfied();
    }

    private void createContext(String routeName) throws Exception {
        AdviceWith.adviceWith(context, routeName, a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("*");
        });
        context.start();
    }

    private static Map<String, Object> createEvent(final String identifier, final String... type) {

        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_URI, baseUri + identifier);
        headers.put(EVENT_TYPE, EVENT_CREATE);
        headers.put(IDENTIFIER, identifier);
        headers.put(BASE_URL, baseUri);
        headers.put(RESOURCE_TYPE, String.join(",", type));

        return headers;
    }
}
