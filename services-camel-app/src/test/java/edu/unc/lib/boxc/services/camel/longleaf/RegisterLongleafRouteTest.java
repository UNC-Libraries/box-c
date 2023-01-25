package edu.unc.lib.boxc.services.camel.longleaf;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.CamelTestContextBootstrapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.boxc.services.camel.longleaf.RegisterToLongleafProcessor;

/**
 * @author bbpennel
 */
@CamelSpringBootTest
@BootstrapWith(CamelTestContextBootstrapper.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/spring-test/jms-context.xml"),
    @ContextConfiguration("/register-longleaf-router-context.xml")
})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class RegisterLongleafRouteTest extends AbstractLongleafRouteTest {
    private static final String TEXT1_BODY = "Some content";
    private static final String TEXT1_SHA1 = DigestUtils.sha1Hex(TEXT1_BODY);

    @Produce(uri = "direct-vm:filter.longleaf")
    private ProducerTemplate template;

    @EndpointInject(uri = "mock:direct:longleaf.dlq")
    private MockEndpoint mockDlq;

    @EndpointInject(uri = "mock:direct:registrationSuccessful")
    private MockEndpoint mockSuccess;

    @TempDir
    public Path tmpFolder;
    @Autowired
    private String baseAddress;
    @Autowired
    private CamelContext cdrLongleaf;

    @Autowired
    private RepositoryObjectFactory repoObjFactory;

    @Autowired
    private StorageLocationManager locManager;
    @Autowired
    private BinaryTransferService transferService;
    private BinaryTransferSession transferSession;
    @Autowired
    private RegisterToLongleafProcessor processor;

    private String longleafScript;

    @BeforeEach
    public void init() throws Exception {
        TestHelper.setContentBase(baseAddress);

        outputPath = tmpFolder.resolve("output").toString();
        output = null;
        longleafScript = LongleafTestHelpers.getLongleafScript(outputPath);
        processor.setLongleafBaseCommand(longleafScript);

        StorageLocation loc = locManager.getStorageLocationById("loc1");
        transferSession = transferService.getSession(loc);
    }

    @Test
    public void registerSingleFileWithSha1() throws Exception {
        mockDlq.expectedMessageCount(0);
        mockSuccess.expectedMessageCount(1);

        FileObject fileObj = repoObjFactory.createFileObject(null);
        BinaryObject origBin = createOriginalBinary(fileObj, TEXT1_BODY, TEXT1_SHA1, null);
        URI contentUri = origBin.getContentUri();

        NotifyBuilder notify = new NotifyBuilder(cdrLongleaf)
                .whenCompleted(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(origBin.getPid()));

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue(result1, "Register route not satisfied");

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

        NotifyBuilder notify = new NotifyBuilder(cdrLongleaf)
                .whenCompleted(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(binPid));

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue(result1, "Register route not satisfied");

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

        NotifyBuilder notify = new NotifyBuilder(cdrLongleaf)
                .whenDone(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(origBin.getPid()));

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue(result1, "Register route not satisfied");

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

        NotifyBuilder notify = new NotifyBuilder(cdrLongleaf)
                .whenDone(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(origBin1.getPid()));
        template.sendBodyAndHeaders("", createEvent(origBin2.getPid()));

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue(result1, "Register route not satisfied");

        assertSubmittedPaths(2000, contentUri1.toString(), contentUri2.toString());

        mockSuccess.assertIsSatisfied(1000);
        mockDlq.assertIsSatisfied(1000);
        List<Exchange> dlqExchanges = mockDlq.getExchanges();

        Exchange failed = dlqExchanges.get(0);
        List<String> failedList = failed.getIn().getBody(List.class);
        assertEquals(1, failedList.size(), "Only one uri should be in the failed message body");
        assertTrue(failedList.contains(origBin2.getPid().getRepositoryPath()),
                "Exchange in DLQ must contain the fcrepo uri of the failed binary");
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

        NotifyBuilder notify = new NotifyBuilder(cdrLongleaf)
                .whenDone(2)
                .create();

        template.sendBodyAndHeaders("", createEvent(origBin.getPid()));

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
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
