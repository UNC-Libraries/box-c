package edu.unc.lib.boxc.services.camel.audio;

import edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants;
import edu.unc.lib.boxc.services.camel.images.AddDerivativeProcessor;
import org.apache.camel.BeanInject;
import org.apache.camel.CamelExecutionException;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AudioEnhancementsRouterTest extends CamelSpringTestSupport {
    private static final long timestamp = 1428360320168L;
    private static final String userID = "bypassAdmin";
    private static final String userAgent = "curl/7.37.1";
    private static final String fileID = "343b3da4-8876-42f5-8821-7aabb65e0f19";
    private final String eventTypes = FcrepoJmsConstants.EVENT_NS + "ResourceCreation";
    private final String audioAccessCopy = "AudioAccessCopy";

    @PropertyInject(value = "fcrepo.baseUrl")
    private static String baseUri;

    @EndpointInject("mock:fcrepo")
    protected MockEndpoint resultEndpoint;

    @Produce("direct:process.binary.original")
    protected ProducerTemplate template;

    @BeanInject(value = "addAudioAccessCopyProcessor")
    private AddDerivativeProcessor addAudioAccessCopyProcessor;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/audio-context.xml");
    }

    @AfterEach
    public void cleanup() throws IOException {
        FileUtils.deleteDirectory(new File("target/34"));
    }

    @Test
    public void testAudioAccessCopyRouteNoForceNoFileExists() throws Exception {
        when(addAudioAccessCopyProcessor.needsRun(any())).thenReturn(true);
        createContext(audioAccessCopy);

        var shEndpoint = getMockEndpoint("mock:exec:/bin/sh");
        shEndpoint.expectedMessageCount(1);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");

        template.sendBodyAndHeaders("", headers);

        verify(addAudioAccessCopyProcessor).process(any(Exchange.class));
        verify(addAudioAccessCopyProcessor).cleanupTempFile(any(Exchange.class));
        shEndpoint.assertIsSatisfied();
    }

    @Test
    public void testAudioAccessCopyRouteScriptFails() throws Exception {
        when(addAudioAccessCopyProcessor.needsRun(any())).thenReturn(true);
        createContext(audioAccessCopy);

        MockEndpoint shEndpoint = getMockEndpoint("mock:exec:/bin/sh");
        shEndpoint.expectedMessageCount(1);
        shEndpoint.whenAnyExchangeReceived(exchange -> {
            throw new IllegalStateException("Failing run of exec");
        });

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");
        try {
            template.sendBodyAndHeaders("", headers);
            fail("Exception expected to be thrown");
        } catch (CamelExecutionException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        verify(addAudioAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(addAudioAccessCopyProcessor).cleanupTempFile(any(Exchange.class));
        shEndpoint.assertIsSatisfied();
    }

    @Test
    public void testAudioAccessCopyRouteForceNoFileExists() throws Exception {
        when(addAudioAccessCopyProcessor.needsRun(any())).thenReturn(true);
        createContext(audioAccessCopy);

        var shEndpoint = getMockEndpoint("mock:exec:/bin/sh");
        shEndpoint.expectedMessageCount(1);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "true");

        template.sendBodyAndHeaders("", headers);

        verify(addAudioAccessCopyProcessor).process(any(Exchange.class));
        shEndpoint.assertIsSatisfied();
    }

    @Test
    public void testAudioAccessCopyRouteNoForceFileExists() throws Exception {
        String derivativePath = idToPath(fileID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File existingFile = new File("target/" + derivativePath + "/" + fileID + ".m4a");
        FileUtils.writeStringToFile(existingFile, "extracted text", "utf-8");

        createContext(audioAccessCopy);

        var shEndpoint = getMockEndpoint("mock:exec:/bin/sh");
        shEndpoint.expectedMessageCount(0);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");

        template.sendBodyAndHeaders("", headers);

        verify(addAudioAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(addAudioAccessCopyProcessor, never()).cleanupTempFile(any(Exchange.class));
        shEndpoint.assertIsSatisfied();
    }

    @Test
    public void testAudioAccessCopyRouteForceFileExists() throws Exception {
        when(addAudioAccessCopyProcessor.needsRun(any())).thenReturn(true);
        String derivativePath = idToPath(fileID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File existingFile = new File("target/" + derivativePath + "/" + fileID + ".m4a");
        FileUtils.writeStringToFile(existingFile, "extracted text", "utf-8");

        createContext(audioAccessCopy);

        var shEndpoint = getMockEndpoint("mock:exec:/bin/sh");
        shEndpoint.expectedMessageCount(1);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "true");

        template.sendBodyAndHeaders("", headers);

        verify(addAudioAccessCopyProcessor).process(any(Exchange.class));
        shEndpoint.assertIsSatisfied();
    }

    @Test
    public void testAudioAccessCopyRejection() throws Exception {
        createContext(audioAccessCopy);

        when(addAudioAccessCopyProcessor.needsRun(any())).thenReturn(true);
        var audioEndpoint = getMockEndpoint("mock:process.enhancement.audioAccessCopy");
        audioEndpoint.expectedMessageCount(0);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");
        headers.put(CdrBinaryMimeType, "audio/aac");

        template.sendBodyAndHeaders("", headers);

        verify(addAudioAccessCopyProcessor, never()).process(any(Exchange.class));
        audioEndpoint.assertIsSatisfied();
    }

    private void createContext(String routeName) throws Exception {
        AdviceWith.adviceWith(context, routeName, a -> {
            a.replaceFromWith("direct:process.binary.original");
            a.mockEndpointsAndSkip("*");
        });
        context.start();
    }

    private static Map<String, Object> createEvent(final String identifier, final String eventTypes,
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
        headers.put(CdrBinaryMimeType, "audio/wav");
        headers.put("force", force);

        return headers;
    }
}
