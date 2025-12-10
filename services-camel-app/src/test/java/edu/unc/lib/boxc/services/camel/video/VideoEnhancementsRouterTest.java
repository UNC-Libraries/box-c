package edu.unc.lib.boxc.services.camel.video;

import edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants;
import edu.unc.lib.boxc.services.camel.images.AddDerivativeProcessor;
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
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_AGENT;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_DATE_TIME;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VideoEnhancementsRouterTest extends CamelSpringTestSupport {
    private static final long timestamp = 1428360320168L;
    private static final String userID = "bypassAdmin";
    private static final String userAgent = "curl/7.37.1";
    private static final String fileID = "343b3da4-8876-42f5-8821-7aabb65e0f19";
    private final String eventTypes = FcrepoJmsConstants.EVENT_NS + "ResourceCreation";
    private final String videoAccessCopy = "VideoAccessCopy";

    @PropertyInject(value = "fcrepo.baseUrl")
    private String baseUri;

    @EndpointInject("mock:fcrepo")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:process.binary.original")
    protected ProducerTemplate template;

    @BeanInject("addVideoAccessCopyProcessor")
    private AddDerivativeProcessor addVideoAccessCopyProcessor;

    @BeanInject("mp44uVideoProcessor")
    private Mp44uVideoProcessor mp44uVideoProcessor;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/video-context.xml");
    }

    @AfterEach
    public void cleanup() throws IOException {
        FileUtils.deleteDirectory(new File("target/34"));
    }

    @Test
    public void testVideoAccessCopyRouteNoForceNoFileExists() throws Exception {
        when(addVideoAccessCopyProcessor.needsRun(any())).thenReturn(true);
        createContext(videoAccessCopy);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");

        template.sendBodyAndHeaders("", headers);

        verify(mp44uVideoProcessor).process(any(Exchange.class));
        verify(addVideoAccessCopyProcessor).process(any(Exchange.class));
        verify(addVideoAccessCopyProcessor).cleanupTempFile(any(Exchange.class));
    }

    @Test
    public void testVideoAccessCopyRouteForceNoFileExists() throws Exception {
        when(addVideoAccessCopyProcessor.needsRun(any())).thenReturn(true);
        createContext(videoAccessCopy);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "true");

        template.sendBodyAndHeaders("", headers);

        verify(mp44uVideoProcessor).process(any(Exchange.class));
        verify(addVideoAccessCopyProcessor).process(any(Exchange.class));
    }

    @Test
    public void testVideoAccessCopyRouteNoForceFileExists() throws Exception {
        String derivativePath = idToPath(fileID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File existingFile = new File("target/" + derivativePath + "/" + fileID + ".m4a");
        FileUtils.writeStringToFile(existingFile, "video body", "utf-8");

        createContext(videoAccessCopy);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");

        template.sendBodyAndHeaders("", headers);

        verify(mp44uVideoProcessor, never()).process(any(Exchange.class));
        verify(addVideoAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(addVideoAccessCopyProcessor, never()).cleanupTempFile(any(Exchange.class));
    }

    @Test
    public void testVideoAccessCopyRouteForceFileExists() throws Exception {
        when(addVideoAccessCopyProcessor.needsRun(any())).thenReturn(true);
        String derivativePath = idToPath(fileID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File existingFile = new File("target/" + derivativePath + "/" + fileID + ".mp4");
        FileUtils.writeStringToFile(existingFile, "video body", "utf-8");

        createContext(videoAccessCopy);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "true");

        template.sendBodyAndHeaders("", headers);

        verify(mp44uVideoProcessor).process(any(Exchange.class));
        verify(addVideoAccessCopyProcessor).process(any(Exchange.class));
    }

    @Test
    public void testVideoAccessCopyRejection() throws Exception {
        createContext(videoAccessCopy);

        when(addVideoAccessCopyProcessor.needsRun(any())).thenReturn(true);
        var videoEndpoint = getMockEndpoint("mock:process.enhancement.videoAccessCopy");
        videoEndpoint.expectedMessageCount(0);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");
        headers.put(CdrBinaryMimeType, "video/3gpp");

        template.sendBodyAndHeaders("", headers);

        verify(mp44uVideoProcessor, never()).process(any(Exchange.class));
        verify(addVideoAccessCopyProcessor, never()).process(any(Exchange.class));
        videoEndpoint.assertIsSatisfied();
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
        headers.put(FcrepoJmsConstants.EVENT_TYPE, "ResourceCreation");
        headers.put(FcrepoJmsConstants.IDENTIFIER, "original_file");
        headers.put(FcrepoJmsConstants.RESOURCE_TYPE, Binary.getURI());
        headers.put(CdrBinaryMimeType, "video/mp4");
        headers.put("force", force);

        return headers;
    }
}
