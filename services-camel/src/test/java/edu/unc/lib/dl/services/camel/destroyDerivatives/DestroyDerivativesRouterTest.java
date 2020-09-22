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
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrObjectType;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.BeanInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.dl.rdf.Cdr;

public class DestroyDerivativesRouterTest extends CamelSpringTestSupport {
    private static final String DESTROY_DERIVATIVES_ROUTE = "CdrDestroyDerivatives";
    private static final String DESTROY_FULLTEXT_ROUTE = "CdrDestroyFullText";
    private static final String DESTROY_IMAGE_ROUTE = "CdrDestroyImage";
    private static final String DESTROY_ACCESS_COPY_ROUTE = "CdrDestroyAccessCopy";

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
        getMockEndpoint("mock:direct:image.derivatives.destroy").expectedMessageCount(0);
        getMockEndpoint("mock:direct:fulltext.derivatives.destroy").expectedMessageCount(1);

        createContext(DESTROY_DERIVATIVES_ROUTE);
        template.sendBodyAndHeaders("", createEvent("text/plain", Cdr.FileObject.getURI()));
        verify(binaryInfoProcessor).process(any(Exchange.class));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void routeRequestImage() throws Exception {
        getMockEndpoint("mock:direct:image.derivatives.destroy").expectedMessageCount(1);
        getMockEndpoint("mock:direct:fulltext.derivatives.destroy").expectedMessageCount(0);

        createContext(DESTROY_DERIVATIVES_ROUTE);
        template.sendBodyAndHeaders("", createEvent("image/png", Cdr.FileObject.getURI()));
        verify(binaryInfoProcessor).process(any(Exchange.class));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void routeNonBinary() throws Exception {
        getMockEndpoint("mock:direct:image.derivatives.destroy").expectedMessageCount(0);
        getMockEndpoint("mock:direct:fulltext.derivatives.destroy").expectedMessageCount(0);

        createContext(DESTROY_DERIVATIVES_ROUTE);
        template.sendBodyAndHeaders("", createEvent("application", Cdr.FileObject.getURI()));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void destroyTextDerivative() throws Exception {
        createContext(DESTROY_FULLTEXT_ROUTE);

        template.sendBodyAndHeaders("", createEvent("text/plain", Cdr.FileObject.getURI()));

        verify(destroyFulltextProcessor).process(any(Exchange.class));
        verify(destroySmallThumbnailProcessor, never()).process(any(Exchange.class));
        verify(destroyLargeThumbnailProcessor, never()).process(any(Exchange.class));
        verify(destroyAccessCopyProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void destroyImageThumbnailDerivative() throws Exception {
        createContext(DESTROY_IMAGE_ROUTE);

        template.sendBodyAndHeaders("", createEvent("image/png", Cdr.FileObject.getURI()));

        verify(destroySmallThumbnailProcessor).process(any(Exchange.class));
        verify(destroyLargeThumbnailProcessor).process(any(Exchange.class));
        verify(destroyAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(destroyFulltextProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void destroyImageThumbnailDerivativeCollection() throws Exception {
        createContext(DESTROY_IMAGE_ROUTE);

        template.sendBodyAndHeaders("", createEvent("image/png", Cdr.Collection.getURI()));

        verify(destroySmallThumbnailProcessor).process(any(Exchange.class));
        verify(destroyLargeThumbnailProcessor).process(any(Exchange.class));
        verify(destroyAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(destroyFulltextProcessor, never()).process(any(Exchange.class));
    }

    // See if any messages are routed for object with no mimetype
    @Test
    public void destroyImageThumbnailNoDerivativeCollection() throws Exception {
        getMockEndpoint("mock:direct:image.derivatives.destroy").expectedMessageCount(0);
        getMockEndpoint("mock:direct:fulltext.derivatives.destroy").expectedMessageCount(0);

        createContext(DESTROY_DERIVATIVES_ROUTE);
        template.sendBodyAndHeaders("", createEvent("", Cdr.Collection.getURI()));
        verify(binaryInfoProcessor).process(any(Exchange.class));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void destroyImageAccessDerivative() throws Exception {
        createContext(DESTROY_ACCESS_COPY_ROUTE);

        template.sendBodyAndHeaders("", createEvent("image/png", Cdr.FileObject.getURI()));

        verify(destroySmallThumbnailProcessor, never()).process(any(Exchange.class));
        verify(destroyLargeThumbnailProcessor, never()).process(any(Exchange.class));
        verify(destroyAccessCopyProcessor).process(any(Exchange.class));
        verify(destroyFulltextProcessor, never()).process(any(Exchange.class));
    }

    // See if any message is routed from direct:image.access.destroy
    @Test
    public void destroyImageAccessDerivativeCollection() throws Exception {
        getMockEndpoint("mock:direct:image.access.destroy").expectedMessageCount(0);

        createContext(DESTROY_IMAGE_ROUTE);
        template.sendBodyAndHeaders("", createEvent("image/png", Cdr.Collection.getURI()));

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

    private static Map<String, Object> createEvent(String mimetype, String objType) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(CdrBinaryMimeType, mimetype);
        headers.put(CdrObjectType, objType);
        headers.put(CdrBinaryPidId, "dee2614c-8a4b-4ac2-baf2-4b4afc11af87");

        return headers;
    }
}
