package edu.unc.lib.boxc.services.camel.machineGenerated;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.machineGenerated.MachineGenUpdateService;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.services.camel.TestHelper;
import edu.unc.lib.boxc.services.camel.images.AddDerivativeProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static edu.unc.lib.boxc.services.camel.TestHelper.FILENAME;
import static edu.unc.lib.boxc.services.camel.TestHelper.RESC_ID;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@WireMockTest(httpPort = 46887)
public class MachineGenDescriptionProcessorTest {
    private static final String BOXCTRON_API_BASE_PATH = "http://localhost:46887";
    private static final String SUCCESS_RESPONSE = "{\"filename\":\"photo.jpg\",\"processing_time_ms\":1250.5,\"result\"" +
            ":{\"alt_text\":\"Mountainlandscapewithsnow-coveredpeaks\",\"full_description\":\"" +
            "Ascenicmountainlandscapewithsnow-cappedpeaksrisingaboveaforestedvalley\",\"review_assessment\":" +
            "{\"biased_language\":\"NO\"},\"safety_assessment\":{\"atrocities_depicted\":\"NO\",\"text_characteristics\":" +
            "{\"legibility\":\"N/A\"}},\"transcript\":\"\",\"version\":{\"models\":{\"full_desc\":\"gpt-4o-2024-08-06\"" +
            "},\"timestamp\":\"2024-08-15T10:30:00Z\",\"version\":\"0.1.0\"}},\"success\":true}";
    private static final String FAIL_RESPONSE = "{\"detail\": \"string\"}";
    private static final String API_KEY = "api key";
    private MachineGenDescriptionProcessor processor;
    private AutoCloseable closeable;
    private PID pid = PIDs.get(RESC_ID);
    private HttpClientConnectionManager connectionManager;
    private Path derivativePath;
    @Mock
    private MachineGenUpdateService machineGenUpdateService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private IndexingMessageSender indexingMessageSender;
    @Mock
    private BinaryObject binaryObject;
    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void init() throws IOException {
        closeable = openMocks(this);
        when(repositoryObjectLoader.getBinaryObject(eq(pid))).thenReturn(binaryObject);
        when(binaryObject.getFilename()).thenReturn("filename.txt");
        when(binaryObject.getMimetype()).thenReturn("text/plain");
        derivativePath = tmpFolder.resolve(FILENAME);
        when(binaryObject.getContentUri()).thenReturn(derivativePath.toUri());

        connectionManager = new PoolingHttpClientConnectionManager();

        processor = new MachineGenDescriptionProcessor();

        processor.setBoxctronDescribesBasePath(BOXCTRON_API_BASE_PATH);
        processor.setConnectionManager(connectionManager);
        processor.setIndexingMessageSender(indexingMessageSender);
        processor.setRepositoryObjectLoader(repositoryObjectLoader);
        processor.setMachineGenDescriptionUpdateService(machineGenUpdateService);
        processor.setApiKey(API_KEY);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
        connectionManager.shutdown();
    }

    @Test
    public void testUpdateMachineGenDescriptionSuccess() throws Exception {
        stubFor(WireMock.post(urlMatching("/api/v1/describe/uri"))
                .willReturn(aResponse()
                        .withBody(SUCCESS_RESPONSE)
                        .withHeader("Content-Type", "text/json")
                        .withStatus(HttpStatus.OK.value())));

        processor.process(mockMachineGenExchange(false));
        verify(machineGenUpdateService).updateMachineGenText(any());
        TestHelper.assertIndexingMessageSent(pid, indexingMessageSender, "automated");
    }

    @Test
    public void testUpdateMachineGenDescriptionAPIReturnsError() {
        assertThrows(AddDerivativeProcessor.DerivativeGenerationException.class, () -> {
            stubFor(WireMock.post(urlMatching("/api/v1/describe/uri"))
                    .willReturn(aResponse()
                            .withBody(FAIL_RESPONSE)
                            .withHeader("Content-Type", "text/json")
                            .withStatus(HttpStatus.BAD_REQUEST.value())));

            processor.process(mockMachineGenExchange(false));
            verify(machineGenUpdateService, never()).updateMachineGenText(any());
            TestHelper.assertIndexingMessageNotSent(pid, indexingMessageSender, "automated");
        });
    }

    @Test
    public void testNeedsRunNewDerivative() {
        when(machineGenUpdateService.getMachineGenDerivativePath(any())).thenReturn(derivativePath);
        assertTrue(processor.needsRun(mockMachineGenExchange(false)));
    }

    @Test
    public void testNeedsRunDerivativeExists() throws IOException {
        Files.createDirectories(derivativePath.getParent());
        Files.createFile(derivativePath);
        when(machineGenUpdateService.getMachineGenDerivativePath(any())).thenReturn(derivativePath);
        assertFalse(processor.needsRun(mockMachineGenExchange(false)));
    }

    @Test
    public void testNeedsRunDerivativeExistsForceIsTrue() throws IOException {
        Files.createDirectories(derivativePath.getParent());
        Files.createFile(derivativePath);
        when(machineGenUpdateService.getMachineGenDerivativePath(any())).thenReturn(derivativePath);
        assertTrue(processor.needsRun(mockMachineGenExchange(true)));
    }

    private Exchange mockMachineGenExchange(boolean force) {
        var exchange = mock(Exchange.class);
        var message = mock(Message.class);
        when(exchange.getIn()).thenReturn(message);
        when(message.getHeader(eq(FCREPO_URI))).thenReturn(RESC_ID);
        if (force) {
            when(message.getHeader(eq("force"))).thenReturn("true");
        }
        return exchange;
    }
}
