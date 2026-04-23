package edu.unc.lib.boxc.services.camel.longleaf;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.model.fcrepo.test.TestRepositoryDeinitializer;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper;
import edu.unc.lib.boxc.persist.impl.transfer.FileSystemTransferHelpers;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.fcrepo.client.FcrepoClient;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author bbpennel
 * @author smithjp
 */
@WireMockTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration("/spring-test/cdr-client-container.xml")
public class RegisterToLongleafProcessorIT {
    private static final String REGISTER_PATH = "/register";

    private static final String TEXT1_BODY = "Some content";
    private static final String TEXT1_SHA1 = DigestUtils.sha1Hex(TEXT1_BODY);
    private static final String TEXT1_MD5 = DigestUtils.md5Hex(TEXT1_BODY);
    private static final String TEXT2_BODY = "Another content file";
    private static final String TEXT2_SHA1 = DigestUtils.sha1Hex(TEXT2_BODY);
    private static final String TEXT2_MD5 = DigestUtils.md5Hex(TEXT2_BODY);
    private static final String TEXT3_BODY = "Probably some metadata";
    private static final String TEXT3_MD5 = DigestUtils.md5Hex(TEXT3_BODY);
    private static final String TEXT4_BODY = "Maybe some more metadata";
    private static final String TEXT4_MD5 = DigestUtils.md5Hex(TEXT4_BODY);

    @TempDir
    public Path tmpFolder;

    @Autowired
    private String baseAddress;
    @Autowired
    private RepositoryObjectFactory repoObjFactory;
    @jakarta.annotation.Resource(name = "repositoryObjectLoader")
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private PIDMinter pidMinter;
    @Autowired
    private FcrepoClient fcrepoClient;
    @Autowired
    private StorageLocationTestHelper storageLocationTestHelper;
    @Autowired
    private BinaryTransferService transferService;
    private BinaryTransferSession transferSession;

    private PoolingHttpClientConnectionManager connectionManager;

    private RegisterToLongleafProcessor processor;

    @BeforeEach
    public void initTest(WireMockRuntimeInfo wireMockInfo) {
        TestHelper.setContentBase(baseAddress);

        connectionManager = new PoolingHttpClientConnectionManager();

        processor = new RegisterToLongleafProcessor();
        processor.setFcrepoClient(fcrepoClient);
        processor.setRepositoryObjectLoader(repoObjLoader);
        processor.setLongleafBaseUri(wireMockInfo.getHttpBaseUrl());
        processor.setHttpClientConnectionManager(connectionManager);

        transferSession = transferService.getSession(storageLocationTestHelper.getTestStorageLocation());

        stubSuccessfulRegister();
    }

    @AfterEach
    public void tearDown() throws Exception {
        transferSession.close();
        processor.destroy();
        connectionManager.shutdown();
        TestRepositoryDeinitializer.cleanup(fcrepoClient);
        WireMock.reset();
    }

    @Test
    public void registerSingleFileWithSha1() throws Exception {
        FileObject fileObj = repoObjFactory.createFileObject(null);
        BinaryObject origBin = createOriginalBinary(fileObj, TEXT1_BODY, TEXT1_SHA1, null);

        Exchange exchange = createBatchExchange(origBin);
        processor.process(exchange);

        assertRegisterCalled();
        assertManifestEntry("sha1", TEXT1_SHA1, origBin.getContentUri());
    }

    @Test
    public void registerWorkWithMetadataUsingMd5() throws Exception {
        WorkObject workObj = repoObjFactory.createWorkObject(null);
        PID workPid = workObj.getPid();
        BinaryObject modsBin = createBinary(DatastreamPids.getMdDescriptivePid(workPid),
                TEXT2_BODY, null, TEXT2_MD5);
        URI modsStorageUri = modsBin.getContentUri();
        BinaryObject modsHistoryBin = createBinary(DatastreamPids.getDatastreamHistoryPid(modsBin.getPid()),
                TEXT3_BODY, null, TEXT3_MD5);
        URI modsHistoryStorageUri = modsHistoryBin.getContentUri();
        BinaryObject premisBin = createBinary(DatastreamPids.getMdEventsPid(workPid),
                TEXT4_BODY, null, TEXT4_MD5);
        URI premisStorageUri = premisBin.getContentUri();

        PID filePid = pidMinter.mintContentPid();
        PID originalPid = DatastreamPids.getOriginalFilePid(filePid);
        BinaryTransferOutcome outcome = transferSession.transfer(originalPid, streamString(TEXT1_BODY));
        URI storageUri = outcome.getDestinationUri();
        FileObject fileObj = workObj.addDataFile(filePid, storageUri, "original", "text/plain", null, TEXT1_MD5, null);
        BinaryObject origBin = fileObj.getOriginalFile();

        Exchange exchange = createBatchExchange(modsBin, modsHistoryBin, premisBin, origBin);
        processor.process(exchange);

        assertRegisterCalled();
        assertManifestEntry("md5", TEXT1_MD5, storageUri);
        assertManifestEntry("md5", TEXT2_MD5, modsStorageUri);
        assertManifestEntry("md5", TEXT3_MD5, modsHistoryStorageUri);
        assertManifestEntry("md5", TEXT4_MD5, premisStorageUri);
    }

    @Test
    public void registerFileWithFitsAndMultipleDigests() throws Exception {
        FileObject fileObj = repoObjFactory.createFileObject(null);
        BinaryObject origBin = createOriginalBinary(fileObj, TEXT1_BODY, TEXT1_SHA1, TEXT1_MD5);

        BinaryObject techBin = createBinary(DatastreamPids.getTechnicalMetadataPid(fileObj.getPid()),
                TEXT2_BODY, TEXT2_SHA1, TEXT2_MD5);

        Exchange exchange = createBatchExchange(origBin, techBin);
        processor.process(exchange);

        assertRegisterCalled();
        assertManifestEntry("sha1", TEXT1_SHA1, origBin.getContentUri());
        assertManifestEntry("md5", TEXT1_MD5, origBin.getContentUri());
        assertManifestEntry("sha1", TEXT2_SHA1, techBin.getContentUri());
        assertManifestEntry("md5", TEXT2_MD5, techBin.getContentUri());
    }

    @Test
    public void registerSingleFileWithNoDigest() throws Exception {
        FileObject fileObj = repoObjFactory.createFileObject(null);
        BinaryObject origBin = createOriginalBinary(fileObj, TEXT1_BODY, null, null);

        Exchange exchange = createBatchExchange(origBin);
        processor.process(exchange);

        assertRegisterCalled();
        assertManifestEntry("sha1", TEXT1_SHA1, origBin.getContentUri());
    }

    @Test
    public void registerApiReturnsNon200() {
        stubFor(post(urlPathEqualTo(REGISTER_PATH))
                .willReturn(aResponse()
                        .withStatus(500)));

        FileObject fileObj = repoObjFactory.createFileObject(null);
        BinaryObject origBin = createOriginalBinary(fileObj, TEXT1_BODY, TEXT1_SHA1, null);

        Exchange exchange = createBatchExchange(origBin);
        assertThrows(ServiceException.class, () -> processor.process(exchange));

        assertRegisterCalled();
    }

    @Test
    public void registerPartialFailure() {
        FileObject fileObj1 = repoObjFactory.createFileObject(null);
        BinaryObject origBin1 = createOriginalBinary(fileObj1, TEXT1_BODY, TEXT1_SHA1, null);
        FileObject fileObj2 = repoObjFactory.createFileObject(null);
        BinaryObject origBin2 = createOriginalBinary(fileObj2, TEXT2_BODY, TEXT2_SHA1, null);
        FileObject fileObj3 = repoObjFactory.createFileObject(null);
        BinaryObject origBin3 = createOriginalBinary(fileObj3, TEXT3_BODY, null, TEXT3_MD5);

        String successPath1 = Paths.get(origBin1.getContentUri()).toString();
        String successPath2 = Paths.get(origBin2.getContentUri()).toString();
        String failurePath = Paths.get(origBin3.getContentUri()).toString();
        stubFor(post(urlPathEqualTo(REGISTER_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"event\":\"register\","
                                + "\"success\":[\"" + successPath1 + "\",\"" + successPath2 + "\"],"
                                + "\"failure\":[\"" + failurePath + "\"]}")));

        var exchangeWithTemplate = createBatchExchangeWithTemplate(origBin1, origBin2, origBin3);
        assertThrows(ServiceException.class, () -> processor.process(exchangeWithTemplate.exchange));

        assertRegisterCalled();
        // The two successful registrations should have been sent downstream
        verify(exchangeWithTemplate.producerTemplate).sendBody(
                isNull(String.class),
                eq(Map.of(origBin1.getPid().getRepositoryPath(), origBin1.getContentUri().toString(),
                          origBin2.getPid().getRepositoryPath(), origBin2.getContentUri().toString())));
    }

    private void stubSuccessfulRegister() {
        stubFor(post(urlPathEqualTo(REGISTER_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"event\":\"register\",\"success\":[],\"failure\":[]}")));
    }

    private void assertRegisterCalled() {
        WireMock.verify(1, postRequestedFor(urlPathEqualTo(REGISTER_PATH)));
    }

    private void assertManifestEntry(String alg, String expectedDigest, URI storageUri) {
        Path storagePath = Paths.get(storageUri);
        String expectedBase = FileSystemTransferHelpers.getBaseBinaryPath(storagePath);
        String expectedPath = storagePath.toString();
        // The manifest line format is: "<digest> <basePath> <fullPath>\n"
        // We check that the "body" field of the JSON request contains the section header and the entry line.
        String expectedLine = expectedDigest + " " + expectedBase + " " + expectedPath;
        WireMock.verify(postRequestedFor(urlPathEqualTo(REGISTER_PATH))
                .withRequestBody(matchingJsonPath("$.body", WireMock.containing(alg + ":\n")))
                .withRequestBody(matchingJsonPath("$.body", WireMock.containing(expectedLine))));
    }

    private BinaryObject createBinary(PID binPid, String content, String sha1, String md5) {
        BinaryTransferOutcome outcome = transferSession.transfer(binPid, streamString(content));
        URI storageUri = outcome.getDestinationUri();
        return repoObjFactory.createOrUpdateBinary(binPid, storageUri, "text.txt", "text/plain", sha1, md5, null);
    }

    private BinaryObject createOriginalBinary(FileObject fileObj, String content, String sha1, String md5) {
        PID originalPid = DatastreamPids.getOriginalFilePid(fileObj.getPid());
        BinaryTransferOutcome outcome = transferSession.transfer(originalPid, streamString(content));
        URI storageUri = outcome.getDestinationUri();
        return fileObj.addOriginalFile(storageUri, "original.txt", "plain/text", sha1, md5);
    }


    private InputStream streamString(String text) {
        return new ByteArrayInputStream(text.getBytes(UTF_8));
    }

    private Exchange createBatchExchange(RepositoryObject... objects) {
        return createBatchExchangeWithTemplate(objects).exchange;
    }

    private ExchangeWithTemplate createBatchExchangeWithTemplate(RepositoryObject... objects) {
        Exchange exchange = mock(Exchange.class);
        Message msg = mock(Message.class);
        CamelContext context = mock(CamelContext.class);
        when(exchange.getContext()).thenReturn(context);
        ProducerTemplate template = mock(ProducerTemplate.class);
        when(context.createProducerTemplate()).thenReturn(template);
        when(exchange.getIn()).thenReturn(msg);
        when(msg.getBody(List.class)).thenReturn(Arrays.stream(objects)
                .map(ro -> ro.getPid().getRepositoryPath())
                .collect(Collectors.toList()));
        return new ExchangeWithTemplate(exchange, template);
    }

    private record ExchangeWithTemplate(Exchange exchange, ProducerTemplate producerTemplate) {
    }
}
