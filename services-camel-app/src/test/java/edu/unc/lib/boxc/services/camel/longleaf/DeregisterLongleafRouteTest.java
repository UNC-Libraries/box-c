package edu.unc.lib.boxc.services.camel.longleaf;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.persist.impl.transfer.FileSystemTransferHelpers;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author bbpennel
 */
@WireMockTest
public class DeregisterLongleafRouteTest extends AbstractLongleafRouteTest {
    private static final String FILTER_DEREGISTER_ENDPOINT = "direct:filter.longleaf.deregister";
    private static final String DEREGISTER_PATH = "/api/deregister";

    @EndpointInject("mock:direct:longleaf.dlq")
    private MockEndpoint mockDlq;

    private DeregisterLongleafProcessor deregisterLongleafProcessor;
    private PoolingHttpClientConnectionManager connectionManager;

    @TempDir
    public Path tmpFolder;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext(
                "spring-test/cdr-client-container.xml",
                "spring-test/jms-context.xml",
                "deregister-longleaf-router-context.xml");
    }

    @BeforeEach
    public void setup(WireMockRuntimeInfo wireMockInfo) throws Exception {
        deregisterLongleafProcessor = applicationContext.getBean(DeregisterLongleafProcessor.class);

        connectionManager = new PoolingHttpClientConnectionManager();
        deregisterLongleafProcessor.setLongleafBaseUri(wireMockInfo.getHttpBaseUrl());
        deregisterLongleafProcessor.setHttpClientConnectionManager(connectionManager);

        stubSuccessfulDeregister();
    }

    @AfterEach
    public void tearDown() throws Exception {
        deregisterLongleafProcessor.destroy();
        connectionManager.shutdown();
        WireMock.reset();
    }

    @Test
    public void deregisterSingleBinary() throws Exception {
        // Expecting 1 batch message and 1 individual file message, on different routes
        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(1 + 1)
                .create();

        String contentUri = generateContentUri();
        sendMessages(contentUri);

        boolean result1 = notify.matches(5L, TimeUnit.SECONDS);
        assertTrue(result1, "Deregister route not satisfied");

        assertDeregisterRequestedForPaths(2000, contentUri);
    }

    @Test
    public void deregisterSingleBatch() throws Exception {
        // Expecting 1 batch message and 3 individual file messages, on different routes
        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(1 + 3)
                .create();

        String[] contentUris = generateContentUris(3);
        sendMessages(contentUris);

        boolean result1 = notify.matches(5L, TimeUnit.SECONDS);
        assertTrue(result1, "Deregister route not satisfied");

        assertDeregisterRequestedForPaths(2000, contentUris);
    }

    @Test
    public void deregisterMultipleBatches() throws Exception {
        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(2 + 10)
                .create();

        String[] contentUris = generateContentUris(10);
        sendMessages(contentUris);

        boolean result1 = notify.matches(5L, TimeUnit.SECONDS);
        assertTrue(result1, "Deregister route not satisfied");

        assertDeregisterRequestedForPaths(5000, contentUris);
    }

    // Should process file uris, and absolute paths without file://, but not http uris or relative
    @Test
    public void deregisterMultipleMixOfSchemes() throws Exception {
        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(2 + 9)
                .create();

        String[] contentUris = new String[3*4];
        String[] successUris = new String[3*2];
        for (int i = 0; i < 3; i++) {
            contentUris[i*3] = generateContentUri();
            successUris[i*2] = contentUris[i*3];
            contentUris[i*3+1] = "/path/to/file/" + UUID.randomUUID();
            successUris[i*2+1] = contentUris[i*3+1];
            contentUris[i*3+2] = PIDs.get(UUID.randomUUID().toString()).getRepositoryPath();
            contentUris[i*3+3] = "file/" + UUID.randomUUID();
        }
        sendMessages(contentUris);

        boolean result1 = notify.matches(5L, TimeUnit.SECONDS);
        assertTrue(result1, "Deregister route not satisfied");

        assertDeregisterRequestedForPaths(10000, successUris);
    }

    @Test
    public void deregisterPartialSuccess() throws Exception {
        mockDlq.expectedMessageCount(1);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenDone(3)
                .create();

        String[] contentUris = generateContentUris(3);
        String successPath1 = Paths.get(URI.create(contentUris[0])).toString();
        String successPath3 = Paths.get(URI.create(contentUris[2])).toString();
        String failurePath = Paths.get(URI.create(contentUris[1])).toString();

        stubFor(post(urlPathEqualTo(DEREGISTER_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"event\":\"deregister\","
                                + "\"success\":[\"" + successPath1 + "\",\"" + successPath3 + "\"],"
                                + "\"failure\":[\"" + failurePath + "\"]}")));

        sendMessages(contentUris);

        boolean result1 = notify.matches(20L, TimeUnit.SECONDS);
        assertTrue(result1, "Deregister route not satisfied");

        assertDeregisterRequestedForPaths(5000, contentUris);

        mockDlq.assertIsSatisfied(1000);
        List<Exchange> dlqExchanges = mockDlq.getExchanges();

        Exchange failed = dlqExchanges.get(0);
        var failedList = failed.getIn().getBody(List.class);
        assertEquals(1, failedList.size(), "Only one uri should be in the failed message body");
        assertTrue(failedList.contains(contentUris[1]),
                "Exchange in DLQ must contain the fcrepo uri of the failed binary");
    }

    @Test
    public void deregisterApiError() throws Exception {
        mockDlq.expectedMessageCount(1);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenDone(1)
                .create();

        String[] contentUris = generateContentUris(1);

        stubFor(post(urlPathEqualTo(DEREGISTER_PATH))
                .willReturn(aResponse()
                        .withStatus(500)));

        sendMessages(contentUris);

        boolean result1 = notify.matches(5L, TimeUnit.SECONDS);
        assertTrue(result1, "Deregister route not satisfied");

        mockDlq.assertIsSatisfied(1000);

        List<Exchange> dlqExchanges = mockDlq.getExchanges();
        Exchange failed = dlqExchanges.get(0);
        var failedList = failed.getIn().getBody(List.class);
        assertEquals(1, failedList.size(), "Only one uri should be in the failed message body");

        assertTrue(failedList.contains(contentUris[0]),
                "Exchange in DLQ must contain the fcrepo uri of the unprocessed binary");
    }

    private void stubSuccessfulDeregister() {
        stubFor(post(urlPathEqualTo(DEREGISTER_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"event\":\"deregister\",\"success\":[],\"failure\":[]}")));
    }

    /**
     * Waits up to timeout ms for WireMock to have received a POST request whose body field
     * contains the base path of each provided content URI.
     */
    private void assertDeregisterRequestedForPaths(long timeout, String... contentUris) throws Exception {
        long start = System.currentTimeMillis();
        do {
            try {
                for (String contentUri : contentUris) {
                    URI uri = URI.create(contentUri);
                    Path contentPath = uri.getScheme() == null ? Paths.get(contentUri) : Paths.get(uri);
                    String basePath = FileSystemTransferHelpers.getBaseBinaryPath(contentPath);
                    WireMock.verify(postRequestedFor(urlPathEqualTo(DEREGISTER_PATH))
                            .withRequestBody(matchingJsonPath("$.body", WireMock.containing(basePath))));
                }
                return;
            } catch (AssertionError e) {
                if ((System.currentTimeMillis() - start) > timeout) {
                    throw e;
                }
                Thread.sleep(25);
            }
        } while (true);
    }

    private String generateContentUri() {
        return "file:///path/to/file/" + UUID.randomUUID() + "." + System.nanoTime();
    }

    private String[] generateContentUris(int num) {
        String[] uris = new String[num];
        for (int i = 0; i < num; i++) {
            uris[i] = generateContentUri();
        }
        return uris;
    }

    private void sendMessages(String... contentUris) {
        for (String contentUri : contentUris) {
            template.sendBody(FILTER_DEREGISTER_ENDPOINT, makeDocument(contentUri));
        }
    }

    private String makeDocument(String uri) {
        XMLOutputter out = new XMLOutputter();
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);

        Element obj = new Element("objToDestroy", CDR_MESSAGE_NS);
        Element uriValue = new Element("contentUri", CDR_MESSAGE_NS).setText(uri);
        obj.addContent(uriValue);

        entry.addContent(obj);
        msg.addContent(entry);

        return out.outputString(msg);
    }
}
