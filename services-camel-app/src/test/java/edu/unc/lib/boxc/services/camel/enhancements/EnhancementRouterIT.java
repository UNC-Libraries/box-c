package edu.unc.lib.boxc.services.camel.enhancements;

import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.DepositRecord;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.model.fcrepo.test.TestRepositoryDeinitializer;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper;
import edu.unc.lib.boxc.services.camel.fulltext.FulltextProcessor;
import edu.unc.lib.boxc.services.camel.images.AddDerivativeProcessor;
import edu.unc.lib.boxc.services.camel.solr.SolrIngestProcessor;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.commons.io.FileUtils;
import org.fcrepo.client.FcrepoClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.EVENT_TYPE;
import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.IDENTIFIER;
import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.RESOURCE_TYPE;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.Container;
import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getTechnicalMetadataPid;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 *
 * @author bbpennel
 *
 */
public class EnhancementRouterIT extends CamelSpringTestSupport {
    private final static String FILE_CONTENT = "content";

    private final static long ALLOW_WAIT = 5000;

    private AutoCloseable closeable;

    private String baseAddress;

    private RepositoryObjectFactory repoObjectFactory;
    private StorageLocationTestHelper storageLocationTestHelper;
    private FcrepoClient fcrepoClient;

    private CamelContext cdrEnhancements;

    @Produce("{{cdr.enhancement.stream.camel}}")
    private ProducerTemplate template;

    private AddDerivativeProcessor addAccessCopyProcessor;

    private SolrIngestProcessor solrIngestProcessor;

    private FulltextProcessor fulltextProcessor;

    private UpdateDescriptionService updateDescriptionService;

    @TempDir
    public Path tmpFolder;

    private File tempDir;


    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext(
                "spring-test/cdr-client-container.xml",
                "spring-test/jms-context.xml",
                "enhancement-router-it-context.xml");
    }

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);
        baseAddress = applicationContext.getBean("baseAddress", String.class);
        repoObjectFactory = applicationContext.getBean(RepositoryObjectFactory.class);
        storageLocationTestHelper = applicationContext.getBean(StorageLocationTestHelper.class);
        fcrepoClient = applicationContext.getBean(FcrepoClient.class);
        cdrEnhancements = applicationContext.getBean(CamelContext.class);
        addAccessCopyProcessor = applicationContext.getBean("addAccessCopyProcessor", AddDerivativeProcessor.class);
        solrIngestProcessor = applicationContext.getBean("solrIngestProcessor", SolrIngestProcessor.class);
        fulltextProcessor = applicationContext.getBean("fulltextProcessor", FulltextProcessor.class);
        updateDescriptionService = applicationContext.getBean(UpdateDescriptionService.class);

        when(addAccessCopyProcessor.needsRun(any(Exchange.class))).thenReturn(true);
        TestHelper.setContentBase(baseAddress);
        tempDir = Files.createDirectory(tmpFolder.resolve("target")).toFile();

        File jp2ScriptFile = new File("target/convertJp2.sh");
        FileUtils.writeStringToFile(jp2ScriptFile, "exit 0", "utf-8");
        jp2ScriptFile.deleteOnExit();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
        TestRepositoryDeinitializer.cleanup(fcrepoClient);
        storageLocationTestHelper.cleanupStorageLocations();
    }

    @Test
    public void nonBinaryWithSourceImages() throws Exception {
        CollectionObject collObject = repoObjectFactory.createCollectionObject(null);

        String uuid = collObject.getPid().getUUID();
        String basePath = idToPath(uuid, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File uploadedFile = new File(String.valueOf(Paths.get(tempDir.getAbsolutePath(), basePath, uuid)));

        FileInputStream input = new FileInputStream("src/test/resources/uploaded-files/burndown.png");
        FileUtils.copyInputStreamToFile(input, uploadedFile);

        final Map<String, Object> headers = createEvent(collObject.getPid(),
                Cdr.Collection.getURI(), Container.getURI());
        template.sendBodyAndHeaders("", headers);

        verify(solrIngestProcessor, timeout(ALLOW_WAIT)).process(any(Exchange.class));
    }

    @Test
    public void nonBinaryNoSourceImages() throws Exception {
        CollectionObject collObject = repoObjectFactory.createCollectionObject(null);
        final Map<String, Object> headers = createEvent(collObject.getPid(),
                Cdr.Collection.getURI(), Container.getURI());
        template.sendBodyAndHeaders("", headers);

        verify(solrIngestProcessor, timeout(ALLOW_WAIT)).process(any(Exchange.class));
    }

    @Test
    public void testBinaryImageFile() throws Exception {
        FileObject fileObj = repoObjectFactory.createFileObject(null);
        var storageUri = storageLocationTestHelper.makeTestStorageUri(DatastreamPids.getOriginalFilePid(fileObj.getPid()));
        FileUtils.writeStringToFile(new File(storageUri), FILE_CONTENT, "UTF-8");
        BinaryObject binObj = fileObj.addOriginalFile(storageUri,
                null, "image/png", null, null);

        // Separate exchanges when multicasting
        NotifyBuilder notify1 = new NotifyBuilder(cdrEnhancements)
                .whenCompleted(7)
                .create();

        final Map<String, Object> headers = createEvent(binObj.getPid(), Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        boolean result1 = notify1.matches(5L, TimeUnit.SECONDS);
        assertTrue(result1, "Enhancement route not satisfied");

        verify(addAccessCopyProcessor, timeout(ALLOW_WAIT)).process(any(Exchange.class));
        // Indexing triggered for binary parent
        verify(solrIngestProcessor, timeout(ALLOW_WAIT)).process(any(Exchange.class));
    }

    @Test
    public void testBinaryMetadataFile() throws Exception {
        FileObject fileObj = repoObjectFactory.createFileObject(null);
        var storageUri = storageLocationTestHelper.makeTestStorageUri(fileObj.getPid());
        FileUtils.writeStringToFile(new File(storageUri), FILE_CONTENT, "UTF-8");
        BinaryObject binObj = fileObj.addOriginalFile(storageUri,
                null, "image/png", null, null);

        String mdId = binObj.getPid().getRepositoryPath() + "/fcr:metadata";
        PID mdPid = PIDs.get(mdId);

        NotifyBuilder notify = new NotifyBuilder(cdrEnhancements)
                .whenCompleted(1)
                .create();

        final Map<String, Object> headers = createEvent(mdPid, Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        boolean result = notify.matches(5L, TimeUnit.SECONDS);

        assertTrue(result, "Processing message did not match expectations");

        verify(addAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(solrIngestProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void testInvalidFile() throws Exception {
        FileObject fileObj = repoObjectFactory.createFileObject(null);
        PID fitsPid = getTechnicalMetadataPid(fileObj.getPid());
        var storageUri = storageLocationTestHelper.makeTestStorageUri(fitsPid);
        FileUtils.writeStringToFile(new File(storageUri), FILE_CONTENT, "UTF-8");
        BinaryObject binObj = fileObj.addBinary(fitsPid, storageUri,
                "fits.xml", "text/xml", null, null, null);

        NotifyBuilder notify = new NotifyBuilder(cdrEnhancements)
                .whenCompleted(1)
                .create();

        final Map<String, Object> headers = createEvent(binObj.getPid(), Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        boolean result = notify.matches(5L, TimeUnit.SECONDS);

        assertTrue(result, "Processing message did not match expectations");

        verify(fulltextProcessor,  never()).process(any(Exchange.class));
        verify(solrIngestProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void testProcessFilterOutDescriptiveMDSolr() throws Exception {
        FileObject fileObj = repoObjectFactory.createFileObject(null);
        var modsStream = Files.newInputStream(Path.of("src/test/resources/datastreams/simpleMods.xml"));
        BinaryObject descObj = updateDescriptionService.updateDescription(new UpdateDescriptionRequest(
                mock(AgentPrincipalsImpl.class), fileObj.getPid(), modsStream));

        NotifyBuilder notify = new NotifyBuilder(cdrEnhancements)
                .whenCompleted(1)
                .create();

        Map<String, Object> headers = createEvent(descObj.getPid(),
                Binary.getURI(), Cdr.DescriptiveMetadata.getURI());
        template.sendBodyAndHeaders("", headers);

        boolean result = notify.matches(5L, TimeUnit.SECONDS);

        assertTrue(result, "Processing message did not match expectations");

        verify(solrIngestProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void testDepositManifestFileMetadata() throws Exception {
        DepositRecord recObj = repoObjectFactory.createDepositRecord(null);
        var storageUri = storageLocationTestHelper.makeTestStorageUri(recObj.getPid());
        FileUtils.writeStringToFile(new File(storageUri), FILE_CONTENT, "UTF-8");
        BinaryObject manifestBin = recObj.addManifest(storageUri, "manifest", "text/plain", null, null);

        String mdId = manifestBin.getPid().getRepositoryPath() + "/fcr:metadata";
        PID mdPid = PIDs.get(mdId);

        NotifyBuilder notify = new NotifyBuilder(cdrEnhancements)
                .whenCompleted(1)
                .create();

        final Map<String, Object> headers = createEvent(mdPid, Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        boolean result = notify.matches(5L, TimeUnit.SECONDS);

        assertTrue(result, "Processing message did not match expectations");

        verify(addAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(solrIngestProcessor, never()).process(any(Exchange.class));
    }

    private Map<String, Object> createEvent(PID pid, String... type) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(IDENTIFIER, pid.getRepositoryPath());
        headers.put(EVENT_TYPE, "ResourceCreation");
        headers.put("CamelFcrepoUri", pid.getRepositoryPath());
        headers.put(RESOURCE_TYPE, String.join(",", type));

        return headers;
    }
}
