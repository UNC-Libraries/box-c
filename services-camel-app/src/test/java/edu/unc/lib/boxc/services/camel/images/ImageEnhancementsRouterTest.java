package edu.unc.lib.boxc.services.camel.images;

import org.apache.camel.BeanInject;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrImagePath;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrImagePathCleanup;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrTempPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_AGENT;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_DATE_TIME;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImageEnhancementsRouterTest extends CamelSpringTestSupport {
    private static final String EVENT_NS = "http://fedora.info/definitions/v4/event#";
    private static final String EVENT_TYPE = "org.fcrepo.jms.eventType";
    private static final String IDENTIFIER = "org.fcrepo.jms.identifier";
    private static final String RESOURCE_TYPE = "org.fcrepo.jms.resourceType";
    private static final long timestamp = 1428360320168L;
    private static final String userID = "bypassAdmin";
    private static final String userAgent = "curl/7.37.1";
    private static final String fileID = "343b3da4-8876-42f5-8821-7aabb65e0f19";
    private final String eventTypes = EVENT_NS + "ResourceCreation";
    private final String accessCopyRoute = "AccessCopy";
    private static final String fileName = "de/75/d8/11/de75d811-9e0f-4b1f-8631-2060ab3580cc";
    private static final String derivTmpPath = "tmp/" + fileName;

    @PropertyInject(value = "fcrepo.baseUrl")
    private String baseUri;

    @EndpointInject("mock:fcrepo")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:process.binary.original")
    protected ProducerTemplate template;

    @BeanInject(value = "jp2Processor")
    private Jp2Processor jp2Processor;

    @BeanInject(value = "addAccessCopyProcessor")
    private AddDerivativeProcessor addAccessCopyProcessor;

    @BeanInject(value = "imageCacheInvalidationProcessor")
    private ImageCacheInvalidationProcessor imageCacheInvalidationProcessor;

    @BeanInject(value = "imageDerivativeProcessor")
    private ImageDerivativeProcessor imageDerivativeProcessor;

    @BeanInject(value = "pdfImageProcessor")
    private PdfImageProcessor pdfImageProcessor;

    @TempDir
    public Path tmpFolder;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/images-context.xml");
    }

    public void initDependencies() throws Exception {
        imageDerivativeProcessor.setTempBasePath(tmpFolder.toString());
        // Fake the header setting in the pdf processor
        doAnswer((Answer<Void>) invocation -> {
            Exchange exchange = (Exchange) invocation.getArguments()[0];
            var message = exchange.getIn();
            message.setHeader(CdrImagePath, message.getHeader(CdrBinaryPath, String.class));
            message.setHeader(CdrBinaryMimeType, "image/tiff");
            message.setHeader(CdrImagePathCleanup, true);
            return null;
        }).when(pdfImageProcessor).process(any(Exchange.class));

    }

    @AfterEach
    public void cleanup() throws IOException {
        FileUtils.deleteDirectory(new File("target/34"));
    }

    @Test
    public void testAccessCopyRouteNoForceNoFileExists() throws Exception {
        initDependencies();

        when(addAccessCopyProcessor.needsRun(any())).thenReturn(true);
        createContext(accessCopyRoute);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");

        template.sendBodyAndHeaders("", headers);

        verify(jp2Processor).process(any(Exchange.class));
        verify(addAccessCopyProcessor).process(any(Exchange.class));
        verify(addAccessCopyProcessor).cleanupTempFile(any(Exchange.class));
        verify(imageCacheInvalidationProcessor).process(any());
    }

    @Test
    public void testAccessCopyRouteForceNoFileExists() throws Exception {
        initDependencies();

        when(addAccessCopyProcessor.needsRun(any())).thenReturn(true);
        createContext(accessCopyRoute);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "true");

        template.sendBodyAndHeaders("", headers);

        verify(jp2Processor).process(any(Exchange.class));
        verify(addAccessCopyProcessor).process(any(Exchange.class));
        verify(imageCacheInvalidationProcessor).process(any());
    }

    @Test
    public void testAccessCopyRouteNoForceFileExists() throws Exception {
        initDependencies();

        String derivativePath = idToPath(fileID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File existingFile = new File("target/" + derivativePath + "/" + fileID + ".jp2");
        FileUtils.writeStringToFile(existingFile, "extracted text", "utf-8");

        createContext(accessCopyRoute);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");

        template.sendBodyAndHeaders("", headers);

        verify(jp2Processor, never()).process(any(Exchange.class));
        verify(addAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(addAccessCopyProcessor, never()).cleanupTempFile(any(Exchange.class));
        verify(imageCacheInvalidationProcessor, never()).process(any());
    }

    @Test
    public void testAccessCopyRouteForceFileExists() throws Exception {
        initDependencies();

        when(addAccessCopyProcessor.needsRun(any())).thenReturn(true);
        String derivativePath = idToPath(fileID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File existingFile = new File("target/" + derivativePath + "/" + fileID + ".jp2");
        FileUtils.writeStringToFile(existingFile, "extracted text", "utf-8");

        createContext(accessCopyRoute);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "true");

        template.sendBodyAndHeaders("", headers);

        verify(jp2Processor).process(any(Exchange.class));
        verify(addAccessCopyProcessor).process(any(Exchange.class));
        verify(imageCacheInvalidationProcessor).process(any());
    }

    @Test
    public void testAccessCopyRejection() throws Exception {
        initDependencies();
        createContext(accessCopyRoute);

        when(addAccessCopyProcessor.needsRun(any())).thenReturn(true);
        var imageEndpoint = getMockEndpoint("mock:process.enhancement.imageAccessCopy");
        imageEndpoint.expectedMessageCount(0);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");
        headers.put(CdrBinaryMimeType, "plain/text");

        template.sendBodyAndHeaders("", headers);

        verify(jp2Processor, never()).process(any(Exchange.class));
        verify(addAccessCopyProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void testAccessCopyDisallowedImageType() throws Exception {
        initDependencies();
        createContext(accessCopyRoute);

        when(addAccessCopyProcessor.needsRun(any())).thenReturn(true);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");
        headers.put(CdrBinaryMimeType, "image/vnd.fpx");

        template.sendBodyAndHeaders("", headers);

        verify(jp2Processor, never()).process(any(Exchange.class));
        verify(addAccessCopyProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void testAccessCopyIconFile() throws Exception {
        initDependencies();
        createContext(accessCopyRoute);

        when(addAccessCopyProcessor.needsRun(any())).thenReturn(true);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");
        headers.put(CdrBinaryMimeType, "image/x-icon");

        template.sendBodyAndHeaders("", headers);

        verify(jp2Processor, never()).process(any(Exchange.class));
        verify(addAccessCopyProcessor).needsRun(any(Exchange.class));
        verify(addAccessCopyProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void testAccessCopyRoutePdfBinary() throws Exception {
        when(addAccessCopyProcessor.needsRun(any())).thenReturn(true);

        initDependencies();
        createContext(accessCopyRoute);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "true");
        headers.put(CdrBinaryMimeType, "application/pdf");
        headers.put(CdrBinaryPath, "src/test/resources/boxy.pdf");

        template.sendBodyAndHeaders("", headers);

        verify(pdfImageProcessor).process(any(Exchange.class));
        verify(jp2Processor).process(any(Exchange.class));
        verify(addAccessCopyProcessor).process(any(Exchange.class));
        verify(imageCacheInvalidationProcessor).process(any());
    }

    private void createContext(String routeName) throws Exception {
        AdviceWith.adviceWith(context, routeName, a -> {
            a.replaceFromWith("direct:process.binary.original");
            a.mockEndpointsAndSkip("*");
        });
        context.start();
    }

    private Map<String, Object> createEvent(final String identifier, final String eventTypes,
                                                   final String force) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(FCREPO_URI, identifier);
        headers.put(FCREPO_DATE_TIME, timestamp);
        headers.put(FCREPO_AGENT, Arrays.asList(userID, userAgent));
        headers.put(FCREPO_EVENT_TYPE, eventTypes);
        headers.put(FCREPO_BASE_URL, baseUri);
        headers.put(EVENT_TYPE, "ResourceCreation");
        headers.put(IDENTIFIER, "original_file");
        headers.put(RESOURCE_TYPE, Binary.getURI());
        headers.put(CdrTempPath, derivTmpPath);
        headers.put(CdrBinaryPath, fileName);
        headers.put(CdrBinaryMimeType, "image/png");
        headers.put("force", force);

        return headers;
    }
}
