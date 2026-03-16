package edu.unc.lib.boxc.services.camel.machineGenerated;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.operations.impl.machineGenerated.MachineGenUpdateService;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.machineGenerated.MachineGenRequest;
import edu.unc.lib.boxc.operations.jms.machineGenerated.MachineGenRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.TestHelper;
import org.apache.camel.Exchange;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private MachineGenDescriptionProcessor processor;
    private AutoCloseable closeable;
    private PID filePid;
    private HttpClientConnectionManager connectionManager;
    @Mock
    private FileObject fileObject;
    @Mock
    private MachineGenUpdateService machineGenUpdateService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private IndexingMessageSender indexingMessageSender;
    @Mock
    private BinaryObject binaryObject;

    @BeforeEach
    public void init() throws IOException {
        closeable = openMocks(this);
        filePid = TestHelper.makePid();
        when(repositoryObjectLoader.getFileObject(eq(filePid))).thenReturn(fileObject);
        when(fileObject.getOriginalFile()).thenReturn(binaryObject);
        when(binaryObject.getFilename()).thenReturn("filename.txt");
        when(binaryObject.getMimetype()).thenReturn("text/plain");
        var path = Paths.get("path/to/file/filename.txt");
        when(binaryObject.getUri()).thenReturn(path.toUri());

        connectionManager = new PoolingHttpClientConnectionManager();

        processor = new MachineGenDescriptionProcessor();

        processor.setBoxctronDescribesBasePath(BOXCTRON_API_BASE_PATH);
        processor.setConnectionManager(connectionManager);
        processor.setIndexingMessageSender(indexingMessageSender);
        processor.setRepositoryObjectLoader(repositoryObjectLoader);
        processor.setMachineGenDescriptionUpdateService(machineGenUpdateService);
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

        processor.process(createExchange());
        verify(machineGenUpdateService).updateMachineGenText(any());
        TestHelper.assertIndexingMessageSent(filePid, indexingMessageSender, "automated");
    }

    @Test
    public void testUpdateMachineGenDescriptionAPIReturnsError() throws Exception {
        stubFor(WireMock.post(urlMatching("/api/v1/describe/uri"))
                .willReturn(aResponse()
                        .withBody(FAIL_RESPONSE)
                        .withHeader("Content-Type", "text/json")
                        .withStatus(HttpStatus.BAD_REQUEST.value())));

        processor.process(createExchange());
        verify(machineGenUpdateService, never()).updateMachineGenText(any());
        TestHelper.assertIndexingMessageNotSent(filePid, indexingMessageSender, "automated");
    }

    private Exchange createExchange() throws IOException {
        var request = new MachineGenRequest();
        request.setPidString(filePid.getId());
        return TestHelper.mockExchange(MachineGenRequestSerializationHelper.toJson(request));
    }
}
