/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.services.camel.images;

import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.idToPath;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_AGENT;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_BASE_URL;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_DATE_TIME;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.BeanInject;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

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
    private final String thumbnailRoute = "ProcessThumbnails";
    private final String accessCopyRoute = "AccessCopy";
    private final String smallThumbRoute = "SmallThumbnail";
    private final String largeThumbRoute = "LargeThumbnail";

    @PropertyInject(value = "fcrepo.baseUrl")
    private static String baseUri;

    @EndpointInject(uri = "mock:fcrepo")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:process.binary.original")
    protected ProducerTemplate template;

    @BeanInject(value = "addSmallThumbnailProcessor")
    private AddDerivativeProcessor addSmallThumbnailProcessor;

    @BeanInject(value = "addLargeThumbnailProcessor")
    private AddDerivativeProcessor addLargeThumbnailProcessor;

    @BeanInject(value = "addAccessCopyProcessor")
    private AddDerivativeProcessor addAccessCopyProcessor;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/images-context.xml");
    }

    @Before
    public void setup() throws Exception {
        doNothing().when(addSmallThumbnailProcessor).process(any(Exchange.class));
        doNothing().when(addLargeThumbnailProcessor).process(any(Exchange.class));
        doNothing().when(addAccessCopyProcessor).process(any(Exchange.class));
    }

    @After
    public void cleanup() throws IOException {
        FileUtils.deleteDirectory(new File("target/34"));
    }

    @Test
    public void testThumbnailMulticast() throws Exception {
        createContext(thumbnailRoute);

        getMockEndpoint("mock:direct:small.thumbnail").expectedMessageCount(1);
        getMockEndpoint("mock:direct:large.thumbnail").expectedMessageCount(1);
        template.sendBodyAndHeaders("", createEvent(fileID, eventTypes, "false"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testThumbMulticastFilter() throws Exception {
        createContext(thumbnailRoute);

        getMockEndpoint("mock:direct:small.thumbnail").expectedMessageCount(0);
        getMockEndpoint("mock:direct:large.thumbnail").expectedMessageCount(0);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");
        headers.put(CdrBinaryMimeType, "plain/text");

        template.sendBodyAndHeaders("", headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testThumbDisallowedImageType() throws Exception {
        createContext(thumbnailRoute);

        getMockEndpoint("mock:direct:small.thumbnail").expectedMessageCount(0);
        getMockEndpoint("mock:direct:large.thumbnail").expectedMessageCount(0);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");
        headers.put(CdrBinaryMimeType, "image/vnd.fpx");

        template.sendBodyAndHeaders("", headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testThumbSmallRouteNoForceNoFileExists() throws Exception {
        createContext(smallThumbRoute);

        getMockEndpoint("mock:exec:/bin/sh").expectedMessageCount(1);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");
        template.sendBodyAndHeaders("", headers);

        verify(addSmallThumbnailProcessor).process(any(Exchange.class));
        verify(addSmallThumbnailProcessor).cleanupTempFile(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testThumbSmallRouteScriptFails() throws Exception {
        createContext(smallThumbRoute);

        MockEndpoint shEndpoint = getMockEndpoint("mock:exec:/bin/sh");
        shEndpoint.expectedMessageCount(1);
        shEndpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                throw new IllegalStateException("Failing run of exec");
            }
        });

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");
        try {
            template.sendBodyAndHeaders("", headers);
            fail("Exception expected to be thrown");
        } catch (CamelExecutionException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        verify(addSmallThumbnailProcessor, never()).process(any(Exchange.class));
        verify(addSmallThumbnailProcessor).cleanupTempFile(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testThumbSmallRouteForceNoFileExists() throws Exception {
        createContext(smallThumbRoute);

        getMockEndpoint("mock:exec:/bin/sh").expectedMessageCount(1);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "true");
        template.sendBodyAndHeaders("", headers);

        verify(addSmallThumbnailProcessor).process(any(Exchange.class));
        verify(addSmallThumbnailProcessor).cleanupTempFile(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testThumbSmallRouteNoForceFileExists() throws Exception {
        String derivativePath = idToPath(fileID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File existingFile = new File("target/" + derivativePath + "/" + fileID + ".png");
        FileUtils.writeStringToFile(existingFile, "extracted text", "utf-8");

        createContext(smallThumbRoute);

        getMockEndpoint("mock:exec:/bin/sh").expectedMessageCount(0);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");
        template.sendBodyAndHeaders("", headers);

        verify(addSmallThumbnailProcessor, never()).process(any(Exchange.class));
        verify(addSmallThumbnailProcessor, never()).cleanupTempFile(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testThumbSmallRouteForceFileExists() throws Exception {
        String derivativePath = idToPath(fileID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File existingFile = new File("target/" + derivativePath + "/" + fileID + ".png");
        FileUtils.writeStringToFile(existingFile, "extracted text", "utf-8");

        createContext(smallThumbRoute);

        getMockEndpoint("mock:exec:/bin/sh").expectedMessageCount(1);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "true");
        template.sendBodyAndHeaders("", headers);

        verify(addSmallThumbnailProcessor).process(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testThumbLargeRouteNoForceNoFileExists() throws Exception {
        createContext(largeThumbRoute);

        getMockEndpoint("mock:exec:/bin/sh").expectedMessageCount(1);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");

        template.sendBodyAndHeaders("", headers);

        doNothing().when(addLargeThumbnailProcessor).process(any(Exchange.class));

        verify(addLargeThumbnailProcessor).process(any(Exchange.class));
        verify(addLargeThumbnailProcessor).cleanupTempFile(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testThumbLargeRouteScriptFails() throws Exception {
        createContext(largeThumbRoute);

        MockEndpoint shEndpoint = getMockEndpoint("mock:exec:/bin/sh");
        shEndpoint.expectedMessageCount(1);
        shEndpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                throw new IllegalStateException("Failing run of exec");
            }
        });

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");
        try {
            template.sendBodyAndHeaders("", headers);
            fail("Exception expected to be thrown");
        } catch (CamelExecutionException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        verify(addLargeThumbnailProcessor, never()).process(any(Exchange.class));
        verify(addLargeThumbnailProcessor).cleanupTempFile(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testThumbLargeRouteForceNoFileExists() throws Exception {
        createContext(largeThumbRoute);

        getMockEndpoint("mock:exec:/bin/sh").expectedMessageCount(1);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "true");

        template.sendBodyAndHeaders("", headers);

        doNothing().when(addLargeThumbnailProcessor).process(any(Exchange.class));

        verify(addLargeThumbnailProcessor).process(any(Exchange.class));
        verify(addLargeThumbnailProcessor).cleanupTempFile(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testThumbLargeRouteNoForceFileExists() throws Exception {
        String derivativePath = idToPath(fileID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File existingFile = new File("target/" + derivativePath + "/" + fileID + ".png");
        FileUtils.writeStringToFile(existingFile, "extracted text", "utf-8");
        createContext(largeThumbRoute);

        getMockEndpoint("mock:exec:/bin/sh").expectedMessageCount(0);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");

        template.sendBodyAndHeaders("", headers);

        doNothing().when(addLargeThumbnailProcessor).process(any(Exchange.class));

        verify(addLargeThumbnailProcessor, never()).process(any(Exchange.class));
        verify(addLargeThumbnailProcessor, never()).cleanupTempFile(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testThumbLargeRouteForceFileExists() throws Exception {
        String derivativePath = idToPath(fileID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File existingFile = new File("target/" + derivativePath + "/" + fileID + ".png");
        FileUtils.writeStringToFile(existingFile, "extracted text", "utf-8");
        createContext(largeThumbRoute);

        getMockEndpoint("mock:exec:/bin/sh").expectedMessageCount(1);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "true");

        template.sendBodyAndHeaders("", headers);

        doThrow(Exception.class).when(addLargeThumbnailProcessor).process(any(Exchange.class));

        verify(addLargeThumbnailProcessor).process(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testAccessCopyRouteNoForceNoFileExists() throws Exception {
        createContext(accessCopyRoute);

        getMockEndpoint("mock:exec:/bin/sh").expectedMessageCount(1);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");

        template.sendBodyAndHeaders("", headers);

        verify(addAccessCopyProcessor).process(any(Exchange.class));
        verify(addAccessCopyProcessor).cleanupTempFile(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testAccessCopyRouteScriptFails() throws Exception {
        createContext(accessCopyRoute);

        MockEndpoint shEndpoint = getMockEndpoint("mock:exec:/bin/sh");
        shEndpoint.expectedMessageCount(1);
        shEndpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                throw new IllegalStateException("Failing run of exec");
            }
        });

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");
        try {
            template.sendBodyAndHeaders("", headers);
            fail("Exception expected to be thrown");
        } catch (CamelExecutionException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        verify(addAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(addAccessCopyProcessor).cleanupTempFile(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testAccessCopyRouteForceNoFileExists() throws Exception {
        createContext(accessCopyRoute);

        getMockEndpoint("mock:exec:/bin/sh").expectedMessageCount(1);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "true");

        template.sendBodyAndHeaders("", headers);

        verify(addAccessCopyProcessor).process(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testAccessCopyRouteNoForceFileExists() throws Exception {
        String derivativePath = idToPath(fileID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File existingFile = new File("target/" + derivativePath + "/" + fileID + ".jp2");
        FileUtils.writeStringToFile(existingFile, "extracted text", "utf-8");

        createContext(accessCopyRoute);

        getMockEndpoint("mock:exec:/bin/sh").expectedMessageCount(0);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");

        template.sendBodyAndHeaders("", headers);

        verify(addAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(addAccessCopyProcessor, never()).cleanupTempFile(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testAccessCopyRouteForceFileExists() throws Exception {
        String derivativePath = idToPath(fileID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File existingFile = new File("target/" + derivativePath + "/" + fileID + ".jp2");
        FileUtils.writeStringToFile(existingFile, "extracted text", "utf-8");

        createContext(accessCopyRoute);

        getMockEndpoint("mock:exec:/bin/sh").expectedMessageCount(1);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "true");

        template.sendBodyAndHeaders("", headers);

        verify(addAccessCopyProcessor).process(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testAccessCopyRejection() throws Exception {
        createContext(accessCopyRoute);

        getMockEndpoint("mock:process.enhancement.imageAccessCopy").expectedMessageCount(0);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");
        headers.put(CdrBinaryMimeType, "plain/text");

        template.sendBodyAndHeaders("", headers);

        verify(addAccessCopyProcessor, never()).process(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testAccessCopyDisallowedImageType() throws Exception {
        createContext(accessCopyRoute);

        getMockEndpoint("mock:exec:/bin/sh").expectedMessageCount(0);

        Map<String, Object> headers = createEvent(fileID, eventTypes, "false");
        headers.put(CdrBinaryMimeType, "image/vnd.fpx");

        template.sendBodyAndHeaders("", headers);

        verify(addAccessCopyProcessor, never()).process(any(Exchange.class));
        assertMockEndpointsSatisfied();
    }

    private void createContext(String routeName) throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:process.binary.original");
                mockEndpointsAndSkip("*");
            }
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
        headers.put(EVENT_TYPE, "ResourceCreation");
        headers.put(IDENTIFIER, "original_file");
        headers.put(RESOURCE_TYPE, Binary.getURI());
        headers.put(CdrBinaryMimeType, "image/png");
        headers.put("force", force);

        return headers;
    }
}
