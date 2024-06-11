package edu.unc.lib.boxc.services.camel.longleaf;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.codec.digest.DigestUtils;
import org.fcrepo.client.FcrepoClient;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.boxc.persist.impl.transfer.FileSystemTransferHelpers;
import edu.unc.lib.boxc.services.camel.longleaf.RegisterToLongleafProcessor;

/**
 * @author bbpennel
 * @author smithjp
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml")
})
public class RegisterToLongleafProcessorIT {
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

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Autowired
    private String baseAddress;
    @Autowired
    private RepositoryObjectFactory repoObjFactory;
    @javax.annotation.Resource(name = "repositoryObjectLoader")
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private PIDMinter pidMinter;
    @Autowired
    private FcrepoClient fcrepoClient;
    @Autowired
    private StorageLocationManager locManager;
    @Autowired
    private BinaryTransferService transferService;
    private BinaryTransferSession transferSession;

    private String longleafScript;
    private String outputPath;
    private List<String> output;

    private RegisterToLongleafProcessor processor;

    @Before
    public void init() throws Exception {
        TestHelper.setContentBase(baseAddress);
        tmpFolder.create();

        processor = new RegisterToLongleafProcessor();
        processor.setFcrepoClient(fcrepoClient);
        processor.setRepositoryObjectLoader(repoObjLoader);
        outputPath = tmpFolder.newFile().getPath();
        output = null;
        longleafScript = LongleafTestHelpers.getLongleafScript(outputPath);
        processor.setLongleafBaseCommand(longleafScript);

        StorageLocation loc = locManager.getStorageLocationById("loc1");
        transferSession = transferService.getSession(loc);
    }

    @After
    public void tearDown() throws Exception {
        transferSession.close();
    }

    @Test
    public void registerSingleFileWithSha1() throws Exception {
        FileObject fileObj = repoObjFactory.createFileObject(null);
        BinaryObject origBin = createOriginalBinary(fileObj, TEXT1_BODY, TEXT1_SHA1, null);

        Exchange exchange = createBatchExchange(origBin);
        processor.process(exchange);

        output = LongleafTestHelpers.readOutput(outputPath);
        assertRegisterCalled(1);
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

        output = LongleafTestHelpers.readOutput(outputPath);
        assertRegisterCalled(1);
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

        output = LongleafTestHelpers.readOutput(outputPath);
        assertRegisterCalled(1);
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

        output = LongleafTestHelpers.readOutput(outputPath);
        assertRegisterCalled(1);
        assertManifestEntry("sha1", TEXT1_SHA1, origBin.getContentUri());
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

    private void assertRegisterCalled(int expectedCount) {
        int count = 0;
        for (String line : output) {
            if (("register --force -m @-").equals(line)) {
                count++;
            }
        }

        assertEquals(expectedCount, count);
    }

    private void assertManifestEntry(String alg, String expectedDigest, URI storageUri) {
        int algIndex = output.indexOf(alg + ":");
        assertNotEquals("Expected digest algorithm " + alg + " not found in manifest", -1, algIndex);

        Path storagePath = Paths.get(storageUri);
        String expectedBase = FileSystemTransferHelpers.getBaseBinaryPath(storagePath);
        String expectedPath = storagePath.toString();
        for (int i = algIndex + 1; i < output.size(); i++) {
            String line = output.get(i);
            if (line.matches("\\S+:")) {
                break;
            }
            if (line.matches(expectedDigest + " +" + expectedBase + " +" + expectedPath)) {
                return;
            }
        }
        fail("Did not find entry for " + alg + " "
                + expectedDigest + " " + expectedPath + " in manifest:\n" + output);
    }

    private InputStream streamString(String text) {
        return new ByteArrayInputStream(text.getBytes(UTF_8));
    }

    private Exchange createBatchExchange(RepositoryObject... objects) {
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
        return exchange;
    }
}
