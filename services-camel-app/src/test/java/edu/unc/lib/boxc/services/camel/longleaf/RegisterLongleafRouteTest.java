package edu.unc.lib.boxc.services.camel.longleaf;

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
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.fcrepo.client.FcrepoClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.ByteArrayInputStream;
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

/**
 * @author bbpennel
 */
//@ExtendWith(MockitoExtension.class)
public class RegisterLongleafRouteTest extends AbstractLongleafRouteTest {
    private static final String TEXT1_BODY = "Some content";
    private static final String TEXT1_SHA1 = DigestUtils.sha1Hex(TEXT1_BODY);
    private static final URI CONTENT_URI = URI.create("file:///path/to/content.txt");

    @Produce(uri = "direct-vm:filter.longleaf")
    private ProducerTemplate template;

    @EndpointInject(uri = "mock:direct:longleaf.dlq")
    private MockEndpoint mockDlq;

    @EndpointInject(uri = "mock:direct:registrationSuccessful")
    private MockEndpoint mockSuccess;

    @TempDir
    public Path tmpFolder;
    private String baseAddress;
    private CamelContext cdrLongleaf;

    private RepositoryObjectFactory repoObjFactory;
    private StorageLocationManager locManager;
    private BinaryTransferService transferService;
    private BinaryTransferSession transferSession;
    private RegisterToLongleafProcessor processor;
    private StorageLocationTestHelper storageLocationTestHelper;
    private FcrepoClient fcrepoClient;

    private String longleafScript;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext(
                "spring-test/cdr-client-container.xml",
                "spring-test/jms-context.xml",
                "register-longleaf-router-context.xml");
    }

    @BeforeEach
    public void init() throws Exception {
        baseAddress = applicationContext.getBean("baseAddress", String.class);
        repoObjFactory = applicationContext.getBean(RepositoryObjectFactory.class);
        storageLocationTestHelper = applicationContext.getBean(StorageLocationTestHelper.class);
        fcrepoClient = applicationContext.getBean(FcrepoClient.class);
        transferService = applicationContext.getBean(BinaryTransferService.class);
        processor = applicationContext.getBean(RegisterToLongleafProcessor.class);
        locManager = applicationContext.getBean(StorageLocationManager.class);

        TestHelper.setContentBase(baseAddress);

        Path tmpPath = tmpFolder.resolve("output_file");
        Files.createFile(tmpPath);
        outputPath = tmpPath.toAbsolutePath().toString();
        output = null;
        longleafScript = LongleafTestHelpers.getLongleafScript(outputPath);
        processor.setLongleafBaseCommand(longleafScript);

        transferSession = transferService.getSession(storageLocationTestHelper.getTestStorageLocation());
    }

    @AfterEach
    void closeService() throws Exception {
        TestRepositoryDeinitializer.cleanup(fcrepoClient);
        storageLocationTestHelper.cleanupStorageLocations();
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

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Register route not satisfied", result1);

        mockDlq.assertIsSatisfied();
        mockSuccess.assertIsSatisfied(1000);

        assertSubmittedPaths(2000, contentUri.toString());
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

        FileObject fileObj = repoObjFactory.createFileObject(null);
        BinaryObject origBin = createOriginalBinary(fileObj, TEXT1_BODY, TEXT1_SHA1, null);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenDone(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(origBin.getPid()));

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

        FileObject fileObj1 = repoObjFactory.createFileObject(null);
        BinaryObject origBin1 = createOriginalBinary(fileObj1, TEXT1_BODY, TEXT1_SHA1, null);
        URI contentUri1 = origBin1.getContentUri();
        FileObject fileObj2 = repoObjFactory.createFileObject(null);
        BinaryObject origBin2 = createOriginalBinary(fileObj2, TEXT1_BODY, TEXT1_SHA1, null);
        URI contentUri2 = origBin2.getContentUri();

        // Append to existing script
        FileUtils.writeStringToFile(new File(longleafScript),
                "\necho \"SUCCESS register " + Paths.get(contentUri1).toString() + "\"" +
                "\necho \"FAILURE register " + Paths.get(contentUri2).toString() + " bad stuff\"" +
                "\nexit 2",
                UTF_8, true);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenDone(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(origBin1.getPid()));
        template.sendBodyAndHeaders("", createEvent(origBin2.getPid()));

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Register route not satisfied", result1);

        assertSubmittedPaths(2000, contentUri1.toString(), contentUri2.toString());

        mockSuccess.assertIsSatisfied(1000);
        mockDlq.assertIsSatisfied(1000);
        List<Exchange> dlqExchanges = mockDlq.getExchanges();

        Exchange failed = dlqExchanges.get(0);
        List<String> failedList = failed.getIn().getBody(List.class);
        assertEquals("Only one uri should be in the failed message body", 1, failedList.size());
        assertTrue("Exchange in DLQ must contain the fcrepo uri of the failed binary",
                failedList.contains(origBin2.getPid().getRepositoryPath()));
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

        FileObject fileObj = repoObjFactory.createFileObject(null);
        BinaryObject origBin = createOriginalBinary(fileObj, TEXT1_BODY, TEXT1_SHA1, null);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenDone(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(origBin.getPid()));

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
                failedList.contains(origBin.getPid().getRepositoryPath()));
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
