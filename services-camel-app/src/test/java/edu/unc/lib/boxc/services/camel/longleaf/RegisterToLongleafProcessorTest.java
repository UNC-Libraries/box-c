package edu.unc.lib.boxc.services.camel.longleaf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPIDMinter;
import edu.unc.lib.boxc.persist.impl.transfer.FileSystemTransferHelpers;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author bbpennel
 */
@WireMockTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RegisterToLongleafProcessorTest {
    private static final String REGISTER_PATH = "/register";

    private PIDMinter pidMinter;
    private PID filePid;

    @Mock
    private RepositoryObjectLoader repoObjLoader;

    private PoolingHttpClientConnectionManager connectionManager;
    private RegisterToLongleafProcessor processor;

    @TempDir
    Path tmpFolder;

    @BeforeEach
    public void setup(WireMockRuntimeInfo wireMockInfo) {
        pidMinter = new RepositoryPIDMinter();
        filePid = pidMinter.mintContentPid();

        connectionManager = new PoolingHttpClientConnectionManager();
        processor = new RegisterToLongleafProcessor();
        processor.setRepositoryObjectLoader(repoObjLoader);
        processor.setLongleafBaseUri(wireMockInfo.getHttpBaseUrl());
        processor.setHttpClientConnectionManager(connectionManager);
    }

    @AfterEach
    public void tearDown() {
        processor.destroy();
        connectionManager.shutdown();
        WireMock.reset();
    }

    @Test
    public void registerableBinaryOriginal() throws Exception {
        PID binPid = DatastreamPids.getOriginalFilePid(filePid);
        Exchange exchange = createIndividualExchange(binPid);
        assertTrue(RegisterToLongleafProcessor.registerableBinary(exchange));
    }

    @Test
    public void registerableBinaryMdDescriptive() throws Exception {
        PID binPid = DatastreamPids.getMdDescriptivePid(filePid);
        Exchange exchange = createIndividualExchange(binPid);
        assertTrue(RegisterToLongleafProcessor.registerableBinary(exchange));
    }

    @Test
    public void registerableBinaryMdDescriptiveHistory() throws Exception {
        PID binPid = DatastreamPids.getMdDescriptivePid(filePid);
        binPid = DatastreamPids.getDatastreamHistoryPid(binPid);
        Exchange exchange = createIndividualExchange(binPid);
        assertTrue(RegisterToLongleafProcessor.registerableBinary(exchange));
    }

    @Test
    public void registerableBinaryMdEvents() throws Exception {
        PID binPid = DatastreamPids.getMdEventsPid(filePid);
        Exchange exchange = createIndividualExchange(binPid);
        assertTrue(RegisterToLongleafProcessor.registerableBinary(exchange));
    }

    @Test
    public void registerableBinaryTechMd() throws Exception {
        PID binPid = DatastreamPids.getTechnicalMetadataPid(filePid);
        Exchange exchange = createIndividualExchange(binPid);
        assertTrue(RegisterToLongleafProcessor.registerableBinary(exchange));
    }

    @Test
    public void registerableBinaryManifest() throws Exception {
        PID depositPid = pidMinter.mintDepositRecordPid();
        PID binPid = DatastreamPids.getDepositManifestPid(depositPid, "mets.xml");
        Exchange exchange = createIndividualExchange(binPid);
        assertTrue(RegisterToLongleafProcessor.registerableBinary(exchange));
    }

    @Test
    public void registerableBinaryJp2() throws Exception {
        PID binPid = PIDs.get(filePid.getRepositoryPath() + "/data/jp2");
        Exchange exchange = createIndividualExchange(binPid);
        assertFalse(RegisterToLongleafProcessor.registerableBinary(exchange));
    }

    @Test
    public void registerableBinaryManifestMetadata() throws Exception {
        PID depositPid = pidMinter.mintDepositRecordPid();
        PID binPid = DatastreamPids.getDepositManifestPid(depositPid, "mets.xml");
        PID mdPid = PIDs.get(binPid.getRepositoryPath() + "/fcr:metadata");
        Exchange exchange = createIndividualExchange(mdPid);
        assertFalse(RegisterToLongleafProcessor.registerableBinary(exchange));
    }

    @Test
    public void processPartialFailure() throws Exception {
        // Two files with sha1, one with md5 only
        PID sha1Pid1 = DatastreamPids.getOriginalFilePid(pidMinter.mintContentPid());
        PID sha1Pid2 = DatastreamPids.getOriginalFilePid(pidMinter.mintContentPid());
        PID md5Pid = DatastreamPids.getMdDescriptivePid(pidMinter.mintContentPid());

        Path sha1File1 = Files.createTempFile(tmpFolder, "sha1file1", ".bin");
        Path sha1File2 = Files.createTempFile(tmpFolder, "sha1file2", ".bin");
        Path md5File = Files.createTempFile(tmpFolder, "md5file", ".bin");

        String sha1Digest1 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        String sha1Digest2 = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        String md5Digest = "cccccccccccccccccccccccccccccccc";

        mockBinaryObject(sha1Pid1, sha1File1.toUri(), sha1Digest1, null);
        mockBinaryObject(sha1Pid2, sha1File2.toUri(), sha1Digest2, null);
        mockBinaryObject(md5Pid, md5File.toUri(), null, md5Digest);

        // sha1 files succeed, md5 file fails
        String successPath1 = sha1File1.toString();
        String successPath2 = sha1File2.toString();
        String failurePath = md5File.toString();
        stubFor(post(urlPathEqualTo(REGISTER_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"event\":\"register\","
                                + "\"success\":[\"" + successPath1 + "\",\"" + successPath2 + "\"],"
                                + "\"failure\":[\"" + failurePath + "\"]}")));

        ProducerTemplate producerTemplate = mock(ProducerTemplate.class);
        Exchange exchange = createBatchExchange(producerTemplate,
                sha1Pid1.getRepositoryPath(), sha1Pid2.getRepositoryPath(), md5Pid.getRepositoryPath());

        assertThrows(ServiceException.class, () -> processor.process(exchange));

        // Capture the body field from the actual request to assert exact content
        var requests = WireMock.findAll(WireMock.postRequestedFor(urlPathEqualTo(REGISTER_PATH)));
        String requestBody = new ObjectMapper().readTree(requests.getFirst().getBodyAsString()).get("body").asText();

        // Assert sha1 section contains both sha1 files and no md5 files
        int sha1SectionStart = requestBody.indexOf("sha1:\n");
        int md5SectionStart = requestBody.indexOf("md5:\n");
        assertTrue(sha1SectionStart >= 0, "Expected sha1 section in manifest");
        assertTrue(md5SectionStart >= 0, "Expected md5 section in manifest");

        String sha1Section = requestBody.substring(sha1SectionStart, md5SectionStart);
        String md5Section = requestBody.substring(md5SectionStart);
        int sha1EntryCount = sha1Section.split("\\n").length - 1;
        int md5EntryCount = md5Section.split("\\n").length - 1;

        String sha1Base1 = FileSystemTransferHelpers.getBaseBinaryPath(sha1File1);
        String sha1Base2 = FileSystemTransferHelpers.getBaseBinaryPath(sha1File2);
        String md5Base = FileSystemTransferHelpers.getBaseBinaryPath(md5File);

        assertTrue(sha1Section.contains(sha1Digest1 + " " + sha1Base1 + " " + sha1File1),
                "sha1 section should contain first sha1 entry");
        assertTrue(sha1Section.contains(sha1Digest2 + " " + sha1Base2 + " " + sha1File2),
                "sha1 section should contain second sha1 entry");
        assertEquals(2, sha1EntryCount);

        assertTrue(md5Section.contains(md5Digest + " " + md5Base + " " + md5File),
                "md5 section should contain md5 entry");
        assertEquals(1, md5EntryCount);

        // Verify only the two successful files were sent downstream
        verify(producerTemplate).sendBody(
                isNull(String.class),
                eq(Map.of(sha1Pid1.getRepositoryPath(), sha1File1.toUri().toString(),
                           sha1Pid2.getRepositoryPath(), sha1File2.toUri().toString())));
    }

    @Test
    public void processServerErrorEmptyArrays() throws Exception {
        // Simulate a misconfigured server that returns 500 with empty success/failure arrays
        stubFor(post(urlPathEqualTo(REGISTER_PATH))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"event\":\"register\",\"success\":[],\"failure\":[]}")));

        PID sha1Pid = DatastreamPids.getOriginalFilePid(pidMinter.mintContentPid());
        Path sha1File = Files.createTempFile(tmpFolder, "sha1file", ".bin");
        mockBinaryObject(sha1Pid, sha1File.toUri(), "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", null);

        ProducerTemplate producerTemplate = mock(ProducerTemplate.class);
        Exchange exchange = createBatchExchange(producerTemplate, sha1Pid.getRepositoryPath());

        ServiceException ex = assertThrows(ServiceException.class, () -> processor.process(exchange));
        assertTrue(ex.getMessage().contains("500"), "Exception message should include the HTTP status code");

        // No success message should be sent downstream since nothing succeeded
        verify(producerTemplate, org.mockito.Mockito.never()).sendBody(
                org.mockito.ArgumentMatchers.any(String.class), org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void processConnectionError() throws Exception {
        stubFor(post(urlPathEqualTo(REGISTER_PATH))
                .willReturn(aResponse()
                        .withFault(CONNECTION_RESET_BY_PEER)));

        PID sha1Pid = DatastreamPids.getOriginalFilePid(pidMinter.mintContentPid());
        Path sha1File = Files.createTempFile(tmpFolder, "sha1file", ".bin");
        mockBinaryObject(sha1Pid, sha1File.toUri(), "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", null);


        Exchange exchange = createBatchExchange(mock(ProducerTemplate.class),
                sha1Pid.getRepositoryPath());
        assertThrows(LongleafConnectionException.class, () -> processor.process(exchange));
    }

    @Test
    public void processBadRequest() throws Exception {
        stubFor(post(urlPathEqualTo(REGISTER_PATH))
                .willReturn(aResponse()
                        .withStatus(400)));

        PID sha1Pid = DatastreamPids.getOriginalFilePid(pidMinter.mintContentPid());
        Path sha1File = Files.createTempFile(tmpFolder, "sha1file", ".bin");
        mockBinaryObject(sha1Pid, sha1File.toUri(), "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", null);

        Exchange exchange = createBatchExchange(mock(ProducerTemplate.class),
                sha1Pid.getRepositoryPath());
        assertThrows(LongleafBadRequestException.class, () -> processor.process(exchange));
    }

    private BinaryObject mockBinaryObject(PID pid, URI storageUri, String sha1, String md5) {
        BinaryObject binObj = mock(BinaryObject.class);
        when(repoObjLoader.getBinaryObject(eq(pid))).thenReturn(binObj);
        when(binObj.getContentUri()).thenReturn(storageUri);
        when(binObj.getSha1Checksum()).thenReturn(sha1 != null ? "urn:sha1:" + sha1 : null);
        when(binObj.getMd5Checksum()).thenReturn(md5 != null ? "urn:md5:" + md5 : null);
        return binObj;
    }

    private Exchange createBatchExchange(ProducerTemplate producerTemplate, String... fcrepoUris) {
        Exchange exchange = mock(Exchange.class);
        Message msg = mock(Message.class);
        CamelContext context = mock(CamelContext.class);
        when(exchange.getContext()).thenReturn(context);
        when(context.createProducerTemplate()).thenReturn(producerTemplate);
        when(exchange.getIn()).thenReturn(msg);
        when(msg.getBody(List.class)).thenReturn(new ArrayList<>(List.of(fcrepoUris)));
        return exchange;
    }

    private Exchange createIndividualExchange(PID pid) throws Exception {
        Exchange exchange = mock(Exchange.class);
        Message msg = mock(Message.class);
        when(exchange.getIn()).thenReturn(msg);
        when(msg.getHeader(FCREPO_URI)).thenReturn(pid.getRepositoryPath());
        return exchange;
    }
}
