package edu.unc.lib.boxc.services.camel.longleaf;

import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.FedoraHeaderConstants;
import org.fcrepo.client.HeadBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
public class RegisterLongleafRouteTest extends AbstractLongleafRouteTest {
    private static final String TEXT1_BODY = "Some content";
    private static final String TEXT1_SHA1 = DigestUtils.sha1Hex(TEXT1_BODY);
    private static final URI CONTENT_URI = URI.create("file:///path/to/content.txt");

    @Produce(uri = "direct-vm:filter.longleaf")
    private ProducerTemplate template;

    @EndpointInject(uri = "mock:direct:registrationSuccessful")
    private MockEndpoint mockSuccess;

    private LongleafAggregationStrategy aggregationStrategy;
    private GetUrisProcessor getUrisProcessor;
    @Mock
    private FcrepoClient fcrepoClient;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private BinaryObject binaryObject;
    @Mock
    private HeadBuilder headBuilder;
    @Mock
    private FcrepoResponse fcrepoResponse;
    private PID binPid;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        PID filePid = TestHelper.makePid();
        binPid = DatastreamPids.getOriginalFilePid(filePid);
        when(repositoryObjectLoader.getBinaryObject(binPid)).thenReturn(binaryObject);
        lenient().when(binaryObject.getSha1Checksum()).thenReturn(TEXT1_SHA1);
        lenient().when(binaryObject.getContentUri()).thenReturn(CONTENT_URI);

        Path tmpPath = tmpFolder.resolve("output_file");
        Files.createFile(tmpPath);
        outputPath = tmpPath.toAbsolutePath().toString();
        output = null;

        deregisterLongleafProcessor = mock(DeregisterLongleafProcessor.class);
        registerToLongleafProcessor = new RegisterToLongleafProcessor();
        registerToLongleafProcessor.setFcrepoClient(fcrepoClient);
        registerToLongleafProcessor.setRepositoryObjectLoader(repositoryObjectLoader);
        registerToLongleafProcessor.setRegistrationSuccessfulEndpoint("mock:direct:registrationSuccessful");
        longleafScript = LongleafTestHelpers.getLongleafScript(outputPath);
        registerToLongleafProcessor.setLongleafBaseCommand(longleafScript);
        var router = getLongleafRouter();
        return router;
    }

    @Test
    public void registerSingleFileWithSha1() throws Exception {
        mockDlq.expectedMessageCount(0);
        mockSuccess.expectedMessageCount(1);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(binPid));

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Register route not satisfied", result1);

        mockDlq.assertIsSatisfied();
        mockSuccess.assertIsSatisfied(1000);

        assertSubmittedPaths(2000, CONTENT_URI.toString());
    }

    @Test
    public void registerSingleFileWithNoChecksums() throws Exception {
        mockDlq.expectedMessageCount(0);
        mockSuccess.expectedMessageCount(1);

        when(binaryObject.getSha1Checksum()).thenReturn(null);

        when(fcrepoClient.head(binPid.getRepositoryUri())).thenReturn(headBuilder);
        when(headBuilder.addHeader(FedoraHeaderConstants.WANT_DIGEST, "sha")).thenReturn(headBuilder);
        when(headBuilder.perform()).thenReturn(fcrepoResponse);
        when(fcrepoResponse.getHeaderValue(FedoraHeaderConstants.DIGEST)).thenReturn("sha1=" + TEXT1_SHA1);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(binPid));

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Register route not satisfied", result1);

        mockDlq.assertIsSatisfied();
        mockSuccess.assertIsSatisfied(1000);

        assertSubmittedPaths(2000, CONTENT_URI.toString());
    }

    @Test
    public void registerBinaryNotFound() throws Exception {
        mockDlq.expectedMessageCount(0);
        mockSuccess.expectedMessageCount(0);

        when(repositoryObjectLoader.getBinaryObject(binPid)).thenThrow(new NotFoundException("Nope"));

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(binPid));

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Register route not satisfied", result1);

        mockDlq.assertIsSatisfied();
        mockSuccess.assertIsSatisfied();

        assertNoSubmittedPaths();
    }

    @Test
    public void registerBinaryNoContentUri() throws Exception {
        mockDlq.expectedMessageCount(0);
        mockSuccess.expectedMessageCount(0);

        lenient().when(binaryObject.getContentUri()).thenReturn(null);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(binPid));

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Register route not satisfied", result1);

        mockDlq.assertIsSatisfied();
        mockSuccess.assertIsSatisfied();

        assertNoSubmittedPaths();
    }

    @Test
    public void registerExecuteFails() throws Exception {
        mockDlq.expectedMessageCount(1);
        mockSuccess.expectedMessageCount(0);

        FileUtils.writeStringToFile(new File(longleafScript), "exit 1", UTF_8);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenDone(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(binPid));

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Register route not satisfied", result1);

        mockDlq.assertIsSatisfied(1000);
        mockSuccess.assertIsSatisfied();

        assertNoSubmittedPaths();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void registerPartiallySucceeds() throws Exception {
        mockDlq.expectedMessageCount(1);
        mockSuccess.expectedMessageCount(1);

        BinaryObject binaryObject2 = mock(BinaryObject.class);
        PID filePid2 = TestHelper.makePid();
        PID binPid2 = DatastreamPids.getOriginalFilePid(filePid2);
        when(repositoryObjectLoader.getBinaryObject(binPid2)).thenReturn(binaryObject2);
        when(binaryObject2.getSha1Checksum()).thenReturn(TEXT1_SHA1);
        var contentUri2 = URI.create("file:///path/to/other.txt");
        when(binaryObject2.getContentUri()).thenReturn(contentUri2);

        // Append to existing script
        FileUtils.writeStringToFile(new File(longleafScript),
                "\necho \"SUCCESS register " + Paths.get(CONTENT_URI).toString() + "\"" +
                "\necho \"FAILURE register " + Paths.get(contentUri2).toString() + " bad stuff\"" +
                "\nexit 2",
                UTF_8, true);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenDone(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(binPid));
        template.sendBodyAndHeaders("", createEvent(binPid2));

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Register route not satisfied", result1);

        assertSubmittedPaths(2000, CONTENT_URI.toString(), contentUri2.toString());

        mockSuccess.assertIsSatisfied(1000);
        mockDlq.assertIsSatisfied(1000);
        List<Exchange> dlqExchanges = mockDlq.getExchanges();

        Exchange failed = dlqExchanges.get(0);
        List<String> failedList = failed.getIn().getBody(List.class);
        assertEquals("Only one uri should be in the failed message body", 1, failedList.size());
        assertTrue("Exchange in DLQ must contain the fcrepo uri of the failed binary",
                failedList.contains(binPid2.getRepositoryPath()));
    }

    // command fails with usage error, but successful response
    @SuppressWarnings("unchecked")
    @Test
    public void registerCommandError() throws Exception {
        mockDlq.expectedMessageCount(1);
        mockSuccess.expectedMessageCount(0);

        FileUtils.writeStringToFile(new File(longleafScript),
                "\necho 'ERROR: \"longleaf register\" was called with arguments [\"--ohno\"]'",
                UTF_8, true);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenDone(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(binPid));

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Register route not satisfied", result1);

        mockDlq.assertIsSatisfied(1000);
        mockSuccess.assertIsSatisfied();

        List<Exchange> dlqExchanges = mockDlq.getExchanges();
        assertEquals(1, dlqExchanges.size());
        Exchange failed = dlqExchanges.get(0);
        List<String> failedList = failed.getIn().getBody(List.class);
        assertEquals("Only one uri should be in the failed message body", 1, failedList.size());
        assertTrue("Exchange in DLQ must contain the fcrepo uri of the failed binary",
                failedList.contains(binPid.getRepositoryPath()));
    }

    private static Map<String, Object> createEvent(PID pid) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_URI, pid.getRepositoryPath());

        return headers;
    }
}
