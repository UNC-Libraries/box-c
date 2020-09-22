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

import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrObjectType;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.services.camel.fulltext.FulltextProcessor;
import edu.unc.lib.dl.services.camel.images.ImageDerivativeProcessor;

/**
 * Router to process requests to destroy derivatives for an object
 *
 * @author lfarrell
 *
 */
public class DestroyDerivativesRouter extends RouteBuilder {
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

    public void configure() throws Exception {
        onException(Exception.class)
                .redeliveryDelay("{{error.retryDelay}}")
                .maximumRedeliveries("{{error.maxRedeliveries}}")
                .backOffMultiplier("{{error.backOffMultiplier}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("{{cdr.destroy.derivatives.stream.camel}}")
                .routeId("CdrDestroyDerivatives")
                .startupOrder(203)
                .log(LoggingLevel.DEBUG, "Received destroy derivatives message")
                .process(binaryInfoProcessor)
                .choice()
                    .when(method(ImageDerivativeProcessor.class, "allowedImageType"))
                        .to("direct:image.derivatives.destroy")
                    .when(method(FulltextProcessor.class, "allowedTextType"))
                        .to("direct:fulltext.derivatives.destroy")
                .end();

        from("direct:fulltext.derivatives.destroy")
                .routeId("CdrDestroyFullText")
                .startupOrder(202)
                .log(LoggingLevel.DEBUG, "Destroying derivative text files")
                .bean(destroyFulltextProcessor);

        from("direct:image.derivatives.destroy")
                .routeId("CdrDestroyImage")
                .startupOrder(201)
                .log(LoggingLevel.DEBUG, "Destroying derivative thumbnails")
                .bean(destroySmallThumbnailProcessor)
                .bean(destroyLargeThumbnailProcessor)
                .filter(simple("${headers['" + CdrObjectType + "']} == '" + Cdr.FileObject.getURI() + "'"))
                    .to("direct:image.access.destroy");

        from("direct:image.access.destroy")
                .routeId("CdrDestroyAccessCopy")
                .startupOrder(200)
                .log(LoggingLevel.DEBUG, "Destroying access copy")
                .bean(destroyAccessCopyProcessor);
    }
}
