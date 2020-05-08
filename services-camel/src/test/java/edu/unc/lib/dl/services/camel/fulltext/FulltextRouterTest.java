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
package edu.unc.lib.dl.services.camel.fulltext;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.idToPath;
import static edu.unc.lib.dl.services.camel.JmsHeaderConstants.EVENT_TYPE;
import static edu.unc.lib.dl.services.camel.JmsHeaderConstants.IDENTIFIER;
import static edu.unc.lib.dl.services.camel.JmsHeaderConstants.RESOURCE_TYPE;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.BeanInject;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.dl.services.camel.images.AddDerivativeProcessor;

public class FulltextRouterTest extends CamelSpringTestSupport {
    private static final String ENHANCEMENT_ROUTE = "CdrServiceFulltextExtraction";
    private static final String EXTRACTION_ROUTE = "ExtractingText";
    private static final String fileID = "343b3da4-8876-42f5-8821-7aabb65e0f19";

    @PropertyInject(value = "fcrepo.baseUri")
    private static String baseUri;

    @EndpointInject(uri = "mock:fcrepo")
    protected MockEndpoint resultEndpoint;

    @BeanInject(value = "fulltextProcessor")
    private FulltextProcessor ftProcessor;

    @BeanInject(value = "addFullTextDerivativeProcessor")
    private AddDerivativeProcessor adProcessor;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/fulltext-context.xml");
    }

    @After
    public void cleanup() throws IOException {
        FileUtils.deleteDirectory(new File("target/34"));
    }

    @Test
    public void testFullTextExtractionFilterValidMimeTypeNoForceNoExistingFile() throws Exception {
        getMockEndpoint("mock:direct:fulltext.extraction").expectedMessageCount(1);
        createContext(ENHANCEMENT_ROUTE);

        template.sendBodyAndHeaders("", createEvent());
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFullTextExtractionFilterValidMimeTypeForceNoExistingFile() throws Exception {
        getMockEndpoint("mock:direct:fulltext.extraction").expectedMessageCount(1);
        createContext(ENHANCEMENT_ROUTE);

        Map<String, Object> headers = createEvent();
        headers.put("force", "true");
        template.sendBodyAndHeaders("", headers);
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFullTextExtractionFilterValidMimeTypeNoForceExistingFile() throws Exception {
        String derivativePath = idToPath(fileID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File existingFile = new File("target/" + derivativePath + "/" + fileID + ".txt");
        FileUtils.writeStringToFile(existingFile, "extracted text", "utf-8");

        getMockEndpoint("mock:direct:fulltext.extraction").expectedMessageCount(0);
        createContext(ENHANCEMENT_ROUTE);

        Map<String, Object> headers = createEvent();
        template.sendBodyAndHeaders("", createEvent());
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFullTextExtractionFilterValidMimeTypeForceExistingFile() throws Exception {
        String derivativePath = idToPath(fileID, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File existingFile = new File("target/" + derivativePath + "/" + fileID + ".txt");
        FileUtils.writeStringToFile(existingFile, "extracted text", "utf-8");

        getMockEndpoint("mock:direct:fulltext.extraction").expectedMessageCount(1);
        createContext(ENHANCEMENT_ROUTE);

        Map<String, Object> headers = createEvent();
        headers.put("force", "true");
        template.sendBodyAndHeaders("", headers);
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFullTextExtractionFilterInvalidMimeType() throws Exception {
        getMockEndpoint("mock:direct:fulltext.extraction").expectedMessageCount(0);

        createContext(ENHANCEMENT_ROUTE);

        Map<String, Object> headers = createEvent();
        headers.put(CdrBinaryMimeType, "image/png");

        template.sendBodyAndHeaders("", headers);

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testTextExtraction() throws Exception {
        createContext(EXTRACTION_ROUTE);

        Map<String, Object> headers = createEvent();
        template.sendBodyAndHeaders("", headers);

        verify(ftProcessor).process(any(Exchange.class));
    }

    private void createContext(String routeName) throws Exception {
        context.getRouteDefinition(routeName).adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("*");
            }
        });

        context.start();
    }

    private static Map<String, Object> createEvent() {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(EVENT_TYPE, "ResourceCreation");
        headers.put(IDENTIFIER, "original_file");
        headers.put(RESOURCE_TYPE, Binary.getURI());
        headers.put(FCREPO_URI, fileID);
        headers.put(CdrBinaryMimeType, "text/plain");
        headers.put("force", "false");

        return headers;
    }
}
