package edu.unc.lib.boxc.services.camel.longleaf;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.model.fcrepo.test.TestRepositoryDeinitializer;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper;
import edu.unc.lib.boxc.persist.impl.transfer.FileSystemTransferHelpers;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.fcrepo.client.FcrepoClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author bbpennel
 */
@WireMockTest
public class RegisterLongleafRouteTest extends AbstractLongleafRouteTest {
    private static final String REGISTER_PATH = "/register";
    private static final String TEXT1_BODY = "Some content";
    private static final String TEXT1_SHA1 = DigestUtils.sha1Hex(TEXT1_BODY);

    @Produce("direct:filter.longleaf")
    private ProducerTemplate template;

    @EndpointInject("mock:direct:longleaf.dlq")
    private MockEndpoint mockDlq;

    @EndpointInject("mock:direct:registrationSuccessful")
    private MockEndpoint mockSuccess;

    @TempDir
    public Path tmpFolder;
    private String baseAddress;

    private RepositoryObjectFactory repoObjFactory;
    private StorageLocationManager locManager;
    private BinaryTransferService transferService;
    private BinaryTransferSession transferSession;
    private RegisterToLongleafProcessor processor;
    private StorageLocationTestHelper storageLocationTestHelper;
    private FcrepoClient fcrepoClient;
    private PoolingHttpClientConnectionManager connectionManager;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext(
                "spring-test/cdr-client-container.xml",
                "spring-test/jms-context.xml",
                "register-longleaf-router-context.xml");
    }

    @BeforeEach
    public void init(WireMockRuntimeInfo wireMockInfo) throws Exception {
        baseAddress = applicationContext.getBean("baseAddress", String.class);
        repoObjFactory = applicationContext.getBean(RepositoryObjectFactory.class);
        storageLocationTestHelper = applicationContext.getBean(StorageLocationTestHelper.class);
        fcrepoClient = applicationContext.getBean(FcrepoClient.class);
        transferService = applicationContext.getBean(BinaryTransferService.class);
        processor = applicationContext.getBean(RegisterToLongleafProcessor.class);
        locManager = applicationContext.getBean(StorageLocationManager.class);

        TestHelper.setContentBase(baseAddress);

        connectionManager = new PoolingHttpClientConnectionManager();
        processor.setLongleafBaseUri(wireMockInfo.getHttpBaseUrl());
        processor.setHttpClientConnectionManager(connectionManager);

        transferSession = transferService.getSession(storageLocationTestHelper.getTestStorageLocation());

        stubSuccessfulRegister();
    }

    @AfterEach
    void closeService() throws Exception {
        processor.destroy();
        connectionManager.shutdown();
        TestRepositoryDeinitializer.cleanup(fcrepoClient);
        storageLocationTestHelper.cleanupStorageLocations();
        WireMock.reset();
    }

    @Test
    public void registerSingleFileWithSha1() throws Exception {
        mockDlq.expectedMessageCount(0);
        mockSuccess.expectedMessageCount(1);

        FileObject fileObj = repoObjFactory.createFileObject(null);
        BinaryObject origBin = createOriginalBinary(fileObj, TEXT1_BODY, TEXT1_SHA1, null);
        URI contentUri = origBin.getContentUri();

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(origBin.getPid()));

        boolean result1 = notify.matches(5L, TimeUnit.SECONDS);
        assertTrue(result1, "Register route not satisfied");

        mockDlq.assertIsSatisfied();
        mockSuccess.assertIsSatisfied(1000);

        assertRegisterRequestedForPath(2000, contentUri);
    }

    @Test
    public void registerBinaryNotFound() throws Exception {
        mockDlq.expectedMessageCount(0);
        mockSuccess.expectedMessageCount(0);

        FileObject fileObj = repoObjFactory.createFileObject(null);
        PID binPid = DatastreamPids.getOriginalFilePid(fileObj.getPid());

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(binPid));

        boolean result1 = notify.matches(5L, TimeUnit.SECONDS);
        assertTrue(result1, "Register route not satisfied");

        mockDlq.assertIsSatisfied();
        mockSuccess.assertIsSatisfied();

        WireMock.verify(0, postRequestedFor(urlPathEqualTo(REGISTER_PATH)));
    }

    @Test
    public void registerExecuteFails() throws Exception {
        mockDlq.expectedMessageCount(1);
        mockSuccess.expectedMessageCount(0);

        stubFor(post(urlPathEqualTo(REGISTER_PATH))
                .willReturn(aResponse()
                        .withStatus(500)));

        FileObject fileObj = repoObjFactory.createFileObject(null);
        BinaryObject origBin = createOriginalBinary(fileObj, TEXT1_BODY, TEXT1_SHA1, null);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenDone(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(origBin.getPid()));

        boolean result1 = notify.matches(5L, TimeUnit.SECONDS);
        assertTrue(result1, "Register route not satisfied");

        mockDlq.assertIsSatisfied(1000);
        mockSuccess.assertIsSatisfied();

        // 500 is retryable: expect initial attempt + 2 redeliveries = 3 total calls
        WireMock.verify(3, postRequestedFor(urlPathEqualTo(REGISTER_PATH)));
    }

    @Test
    public void registerConnectionError() throws Exception {
        mockDlq.expectedMessageCount(1);
        mockSuccess.expectedMessageCount(0);

        stubFor(post(urlPathEqualTo(REGISTER_PATH))
                .willReturn(aResponse()
                        .withFault(CONNECTION_RESET_BY_PEER)));

        FileObject fileObj = repoObjFactory.createFileObject(null);
        BinaryObject origBin = createOriginalBinary(fileObj, TEXT1_BODY, TEXT1_SHA1, null);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenDone(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(origBin.getPid()));

        boolean result1 = notify.matches(5L, TimeUnit.SECONDS);
        assertTrue(result1, "Register route not satisfied");

        mockDlq.assertIsSatisfied(1000);
        mockSuccess.assertIsSatisfied();

        // Connection errors are not retried — expect exactly 1 call
        WireMock.verify(1, postRequestedFor(urlPathEqualTo(REGISTER_PATH)));
    }

    @Test
    public void registerBadRequest() throws Exception {
        mockDlq.expectedMessageCount(1);
        mockSuccess.expectedMessageCount(0);

        stubFor(post(urlPathEqualTo(REGISTER_PATH))
                .willReturn(aResponse()
                        .withStatus(400)));

        FileObject fileObj = repoObjFactory.createFileObject(null);
        BinaryObject origBin = createOriginalBinary(fileObj, TEXT1_BODY, TEXT1_SHA1, null);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenDone(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(origBin.getPid()));

        boolean result1 = notify.matches(5L, TimeUnit.SECONDS);
        assertTrue(result1, "Register route not satisfied");

        mockDlq.assertIsSatisfied(1000);
        mockSuccess.assertIsSatisfied();

        // Bad requests are not retried — expect exactly 1 call
        WireMock.verify(1, postRequestedFor(urlPathEqualTo(REGISTER_PATH)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void registerPartiallySucceeds() throws Exception {
        mockDlq.expectedMessageCount(1);
        mockSuccess.expectedMessageCount(1);

        FileObject fileObj1 = repoObjFactory.createFileObject(null);
        BinaryObject origBin1 = createOriginalBinary(fileObj1, TEXT1_BODY, TEXT1_SHA1, null);
        URI contentUri1 = origBin1.getContentUri();
        FileObject fileObj2 = repoObjFactory.createFileObject(null);
        BinaryObject origBin2 = createOriginalBinary(fileObj2, TEXT1_BODY, TEXT1_SHA1, null);
        URI contentUri2 = origBin2.getContentUri();

        String successPath = Paths.get(contentUri1).toString();
        String failurePath = Paths.get(contentUri2).toString();
        stubFor(post(urlPathEqualTo(REGISTER_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"event\":\"register\","
                                + "\"success\":[\"" + successPath + "\"],"
                                + "\"failure\":[\"" + failurePath + "\"]}")));

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenDone(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(origBin1.getPid()));
        template.sendBodyAndHeaders("", createEvent(origBin2.getPid()));

        boolean result1 = notify.matches(5L, TimeUnit.SECONDS);
        assertTrue(result1, "Register route not satisfied");

        assertRegisterRequestedForPath(2000, contentUri1, contentUri2);

        mockSuccess.assertIsSatisfied(1000);
        mockDlq.assertIsSatisfied(1000);
        List<Exchange> dlqExchanges = mockDlq.getExchanges();

        Exchange failed = dlqExchanges.get(0);
        List<String> failedList = failed.getIn().getBody(List.class);
        assertEquals(1, failedList.size(), "Only one uri should be in the failed message body");
        assertTrue(failedList.contains(origBin2.getPid().getRepositoryPath()),
                "Exchange in DLQ must contain the fcrepo uri of the failed binary");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void registerApiError() throws Exception {
        mockDlq.expectedMessageCount(1);
        mockSuccess.expectedMessageCount(0);

        stubFor(post(urlPathEqualTo(REGISTER_PATH))
                .willReturn(aResponse()
                        .withStatus(500)));

        FileObject fileObj = repoObjFactory.createFileObject(null);
        BinaryObject origBin = createOriginalBinary(fileObj, TEXT1_BODY, TEXT1_SHA1, null);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenDone(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(origBin.getPid()));

        boolean result1 = notify.matches(5L, TimeUnit.SECONDS);
        assertTrue(result1, "Register route not satisfied");

        mockDlq.assertIsSatisfied(1000);
        mockSuccess.assertIsSatisfied();

        List<Exchange> dlqExchanges = mockDlq.getExchanges();
        assertEquals(1, dlqExchanges.size());
        Exchange failed = dlqExchanges.get(0);
        List<String> failedList = failed.getIn().getBody(List.class);
        assertEquals(1, failedList.size(), "Only one uri should be in the failed message body");
        assertTrue(failedList.contains(origBin.getPid().getRepositoryPath()),
                "Exchange in DLQ must contain the fcrepo uri of the failed binary");
    }

    private void stubSuccessfulRegister() {
        stubFor(post(urlPathEqualTo(REGISTER_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"event\":\"register\",\"success\":[],\"failure\":[]}")));
    }

    /**
     * Waits up to timeout ms for WireMock to have received a POST request whose body field
     * contains the base path of each provided content URI.
     */
    private void assertRegisterRequestedForPath(long timeout, URI... contentUris) throws Exception {
        assertPostRequestedForPaths(timeout, REGISTER_PATH, contentUris);
    }

    private BinaryObject createOriginalBinary(FileObject fileObj, String content, String sha1, String md5) {
        PID originalPid = DatastreamPids.getOriginalFilePid(fileObj.getPid());
        BinaryTransferOutcome outcome = transferSession.transfer(originalPid,
                new ByteArrayInputStream(content.getBytes(UTF_8)));
        URI storageUri = outcome.getDestinationUri();
        return fileObj.addOriginalFile(storageUri, "original.txt", "plain/text", sha1, md5);
    }

    private static Map<String, Object> createEvent(PID pid) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_URI, pid.getRepositoryPath());

        return headers;
    }
}
