package edu.unc.lib.boxc.services.camel.enhancements;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.services.camel.BinaryEnhancementProcessor;
import edu.unc.lib.boxc.services.camel.BinaryMetadataProcessor;
import edu.unc.lib.boxc.services.camel.NonBinaryEnhancementProcessor;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.EVENT_TYPE;
import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.IDENTIFIER;
import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.RESOURCE_TYPE;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.api.rdf.Ebucore.hasMimeType;
import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.Container;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
public class EnhancementRouterTest extends CamelTestSupport {
    private final static List<String> ENHANCEMENT_TYPES = Arrays.asList("thumbnails", "imageAccessCopy", "extractFulltext");

    @Produce(uri = "direct:repository.enhancements.stream")
    private ProducerTemplate template;
    @EndpointInject(uri = "mock:direct:solrIndexing")
    private MockEndpoint mockSolrEndpoint;
    @EndpointInject(uri = "mock:direct:process.enhancement.thumbnails")
    private MockEndpoint mockThumbnailEndpoint;
    @EndpointInject(uri = "mock:direct:process.enhancement.imageAccessCopy")
    private MockEndpoint mockAccessCopyEndpoint;
    @EndpointInject(uri = "mock:direct:process.enhancement.extractFulltext")
    private MockEndpoint mockFulltextEndpoint;

    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private DerivativeService derivativeService;

    private BinaryMetadataProcessor binaryMetadataProcessor;
    private BinaryEnhancementProcessor binaryEnhancementProcessor;

    private NonBinaryEnhancementProcessor nbh;

    @TempDir
    public Path tmpFolder;
    private Path sourceImagesPath;
    private Path uploadedFolderPath;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        sourceImagesPath = tmpFolder.resolve("sourceImages");
        uploadedFolderPath = tmpFolder.resolve("uploaded");
        Files.createDirectory(uploadedFolderPath);
        binaryMetadataProcessor = new BinaryMetadataProcessor();
        binaryMetadataProcessor.setDerivativeService(derivativeService);
        binaryMetadataProcessor.setRepositoryObjectLoader(repoObjLoader);
        nbh = new NonBinaryEnhancementProcessor();
        nbh.setSourceImagesDir(sourceImagesPath.toAbsolutePath().toString());
        binaryEnhancementProcessor = new BinaryEnhancementProcessor();
        binaryEnhancementProcessor.setRepositoryObjectLoader(repoObjLoader);
        var router = new EnhancementRouter();
        router.setEnhancementPerformCamel("direct:repository.enhancements.perform");
        router.setEnhancementStreamCamel("direct:repository.enhancements.stream");
        router.setBinaryEnhancementProcessor(binaryEnhancementProcessor);
        router.setNonBinaryEnhancementProcessor(nbh);
        router.setBinaryMetadataProcessor(binaryMetadataProcessor);
        return router;
    }

    private void mockExternalEndpoints() throws Exception {
        AdviceWith.adviceWith(context, "PerformEnhancementsQueue", routeBuilder -> {
            // Replace "direct:solrIndexing" with the URI of the endpoint you want to mock
            routeBuilder.interceptSendToEndpoint("direct:solrIndexing")
                    .skipSendToOriginalEndpoint()
                    .to("mock:direct:solrIndexing");
        });

        AdviceWith.adviceWith(context, "AddBinaryEnhancements", routeBuilder -> {
            for (String enhancementType : ENHANCEMENT_TYPES) {
                routeBuilder.interceptSendToEndpoint("direct:process.enhancement." + enhancementType)
                        .skipSendToOriginalEndpoint()
                        .to("mock:direct:process.enhancement." + enhancementType);
            }
        });
    }

    @Test
    public void nonBinaryWithSourceImages() throws Exception {
        PID collPid = TestHelper.makePid();
        CollectionObject collObject = mock(CollectionObject.class);
        when(collObject.getPid()).thenReturn(collPid);

        mockExternalEndpoints();

        mockSolrEndpoint.expectedMessageCount(1);
        mockThumbnailEndpoint.expectedMessageCount(1);
        mockAccessCopyEndpoint.expectedMessageCount(0);
        mockFulltextEndpoint.expectedMessageCount(0);

        String uuid = collPid.getUUID();
        String basePath = idToPath(uuid, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        Path uploadedPath = sourceImagesPath.resolve(basePath).resolve(uuid);
        Files.createDirectories(uploadedPath.getParent());
        Files.copy(Paths.get("src/test/resources/uploaded-files/burndown.png"), uploadedPath);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(4)
                .create();

        final Map<String, Object> headers = createEvent(collObject.getPid(),
                Cdr.Collection.getURI(), Container.getURI());
        template.sendBodyAndHeaders("", headers);

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Register route not satisfied", result1);

        mockSolrEndpoint.assertIsSatisfied();
        mockThumbnailEndpoint.assertIsSatisfied();
        mockAccessCopyEndpoint.assertIsSatisfied();
        mockFulltextEndpoint.assertIsSatisfied();
    }

    @Test
    public void nonBinaryNoSourceImages() throws Exception {
        PID collPid = TestHelper.makePid();
        CollectionObject collObject = mock(CollectionObject.class);
        when(collObject.getPid()).thenReturn(collPid);

        mockExternalEndpoints();

        mockSolrEndpoint.expectedMessageCount(1);
        mockThumbnailEndpoint.expectedMessageCount(0);
        mockAccessCopyEndpoint.expectedMessageCount(0);
        mockFulltextEndpoint.expectedMessageCount(0);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(1)
                .create();

        final Map<String, Object> headers = createEvent(collObject.getPid(),
                Cdr.Collection.getURI(), Container.getURI());
        template.sendBodyAndHeaders("", headers);

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Register route not satisfied", result1);

        mockSolrEndpoint.assertIsSatisfied();
        mockThumbnailEndpoint.assertIsSatisfied();
        mockAccessCopyEndpoint.assertIsSatisfied();
        mockFulltextEndpoint.assertIsSatisfied();
    }

    @Test
    public void testBinaryImageFile() throws Exception {
        PID filePid = TestHelper.makePid();
        PID origPid = DatastreamPids.getOriginalFilePid(filePid);
        BinaryObject binaryObject = mock(BinaryObject.class);

        when(binaryObject.getContentUri()).thenReturn(Paths.get("src/test/resources/uploaded-files/burndown.png").toUri());
        when(derivativeService.getDerivativePath(filePid, DatastreamType.ACCESS_SURROGATE))
                .thenReturn(Paths.get("path/to/fake/file12345"));
        when(repoObjLoader.getBinaryObject(origPid)).thenReturn(binaryObject);

        Model model = ModelFactory.createDefaultModel();
        Resource binResc = model.getResource(origPid.getRepositoryPath());
        binResc.addProperty(hasMimeType, "image/png");
        when(binaryObject.getResource()).thenReturn(binResc);

        mockExternalEndpoints();

        mockSolrEndpoint.expectedMessageCount(1);
        mockThumbnailEndpoint.expectedMessageCount(1);
        mockAccessCopyEndpoint.expectedMessageCount(1);
        mockFulltextEndpoint.expectedMessageCount(1);

        // Separate exchanges when multicasting
        NotifyBuilder notify1 = new NotifyBuilder(context)
                .whenCompleted(5)
                .create();

        final Map<String, Object> headers = createEvent(origPid, Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        boolean result1 = notify1.matches(5l, TimeUnit.SECONDS);
        assertTrue("Enhancement route not satisfied", result1);

        mockSolrEndpoint.assertIsSatisfied();
        mockThumbnailEndpoint.assertIsSatisfied();
        mockAccessCopyEndpoint.assertIsSatisfied();
        mockFulltextEndpoint.assertIsSatisfied();
    }

    @Test
    public void testBinaryMetadataFile() throws Exception {
        PID filePid = TestHelper.makePid();
        PID origPid = DatastreamPids.getOriginalFilePid(filePid);

        String mdId = origPid.getRepositoryPath() + "/fcr:metadata";
        PID mdPid = PIDs.get(mdId);

        mockExternalEndpoints();

        mockSolrEndpoint.expectedMessageCount(0);
        mockThumbnailEndpoint.expectedMessageCount(0);
        mockAccessCopyEndpoint.expectedMessageCount(0);
        mockFulltextEndpoint.expectedMessageCount(0);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(1)
                .create();

        final Map<String, Object> headers = createEvent(mdPid, Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        boolean result = notify.matches(5l, TimeUnit.SECONDS);

        assertTrue("Processing message did not match expectations", result);

        mockSolrEndpoint.assertIsSatisfied();
        mockThumbnailEndpoint.assertIsSatisfied();
        mockAccessCopyEndpoint.assertIsSatisfied();
        mockFulltextEndpoint.assertIsSatisfied();
    }

    @Test
    public void testInvalidFile() throws Exception {
        PID filePid = TestHelper.makePid();
        PID binPid = DatastreamPids.getTechnicalMetadataPid(filePid);

        mockExternalEndpoints();

        mockSolrEndpoint.expectedMessageCount(0);
        mockThumbnailEndpoint.expectedMessageCount(0);
        mockAccessCopyEndpoint.expectedMessageCount(0);
        mockFulltextEndpoint.expectedMessageCount(0);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(1)
                .create();

        final Map<String, Object> headers = createEvent(binPid, Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        boolean result = notify.matches(5l, TimeUnit.SECONDS);

        assertTrue("Processing message did not match expectations", result);

        mockSolrEndpoint.assertIsSatisfied();
        mockThumbnailEndpoint.assertIsSatisfied();
        mockAccessCopyEndpoint.assertIsSatisfied();
        mockFulltextEndpoint.assertIsSatisfied();
    }

    @Test
    public void testProcessFilterOutDescriptiveMDSolr() throws Exception {
        PID filePid = TestHelper.makePid();
        PID binPid = DatastreamPids.getMdDescriptivePid(filePid);

        mockExternalEndpoints();

        mockSolrEndpoint.expectedMessageCount(0);
        mockThumbnailEndpoint.expectedMessageCount(0);
        mockAccessCopyEndpoint.expectedMessageCount(0);
        mockFulltextEndpoint.expectedMessageCount(0);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(1)
                .create();

        Map<String, Object> headers = createEvent(binPid,
                Binary.getURI(), Cdr.DescriptiveMetadata.getURI());
        template.sendBodyAndHeaders("", headers);

        boolean result = notify.matches(5l, TimeUnit.SECONDS);

        assertTrue("Processing message did not match expectations", result);

        mockSolrEndpoint.assertIsSatisfied();
        mockThumbnailEndpoint.assertIsSatisfied();
        mockAccessCopyEndpoint.assertIsSatisfied();
        mockFulltextEndpoint.assertIsSatisfied();
    }

    @Test
    public void testDepositManifestFileMetadata() throws Exception {
        PID filePid = TestHelper.makePid();
        PID binPid = DatastreamPids.getDepositManifestPid(filePid, "manifest");

        String mdId = binPid.getRepositoryPath() + "/fcr:metadata";
        PID mdPid = PIDs.get(mdId);

        mockExternalEndpoints();

        mockSolrEndpoint.expectedMessageCount(0);
        mockThumbnailEndpoint.expectedMessageCount(0);
        mockAccessCopyEndpoint.expectedMessageCount(0);
        mockFulltextEndpoint.expectedMessageCount(0);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(1)
                .create();

        final Map<String, Object> headers = createEvent(mdPid, Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        boolean result = notify.matches(5l, TimeUnit.SECONDS);

        assertTrue("Processing message did not match expectations", result);

        mockSolrEndpoint.assertIsSatisfied();
        mockThumbnailEndpoint.assertIsSatisfied();
        mockAccessCopyEndpoint.assertIsSatisfied();
        mockFulltextEndpoint.assertIsSatisfied();
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
