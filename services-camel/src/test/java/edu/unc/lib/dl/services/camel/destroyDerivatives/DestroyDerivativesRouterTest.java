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
package edu.unc.lib.dl.services.camel.destroyDerivatives;

import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPidId;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.BeanInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class DestroyDerivativesRouterTest extends CamelSpringTestSupport {
    private static final String DESTROY_DERIVATIVES_ROUTE = "CdrDestroyDerivatives";
    private static final String DESTROY_FULLTEXT_ROUTE = "CdrDestroyFullText";
    private static final String DESTROY_IMAGE_ROUTE = "CdrDestroyImage";

    @Produce(uri = "direct:start")
    private ProducerTemplate template;

    @BeanInject(value = "binaryInfoProcessor")
    private BinaryInfoProcessor binaryInfoProcessor;

    @BeanInject(value = "destroySmallThumbnailProcessor")
    private DestroyDerivativesProcessor destroySmallThumbnailProcessor;

    @BeanInject(value = "destroyLargeThumbnailProcessor")
    private DestroyDerivativesProcessor destroyLargeThumbnailProcessor;

    @BeanInject(value = "destroyAccessCopyProcessor")
    private DestroyDerivativesProcessor destroyAccessCopyProcessor;

    @BeanInject(value = "destroyFulltextProcessor")
    private DestroyDerivativesProcessor destroyFulltextProcessor;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("/service-context.xml", "/destroy-derivatives-context.xml");
    }

    @Test
    public void routeRequestText() throws Exception {
        createContext(DESTROY_DERIVATIVES_ROUTE);
        template.sendBodyAndHeaders("", createEvent("text/plain"));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void routeRequestImage() throws Exception {
        createContext(DESTROY_DERIVATIVES_ROUTE);
        template.sendBodyAndHeaders("", createEvent("image/png"));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void destroyTextDerivative() throws Exception {
        createContext(DESTROY_FULLTEXT_ROUTE);
        template.sendBodyAndHeaders("", createEvent("text/plain"));
        assertMockEndpointsSatisfied();
    }

    @Test
    public void destroyImageDerivative() throws Exception {
        createContext(DESTROY_IMAGE_ROUTE);
        template.sendBodyAndHeaders("", createEvent("image/png"));
        assertMockEndpointsSatisfied();
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

    private static Map<String, Object> createEvent(String mimetype) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(CdrBinaryMimeType, mimetype);
        headers.put(CdrBinaryPidId, "dee2614c-8a4b-4ac2-baf2-4b4afc11af87");
       // http://localhost:8181/fcrepo/rest/content/de/e2/61/4c/dee2614c-8a4b-4ac2-baf2-4b4afc11af87/datafs/original_file

        return headers;
    }
}
