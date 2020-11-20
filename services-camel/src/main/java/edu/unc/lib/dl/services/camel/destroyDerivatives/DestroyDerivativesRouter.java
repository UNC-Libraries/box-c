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
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

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
    private static final Logger log = getLogger(DestroyDerivativesRouter.class);

    @BeanInject(value = "destroyedMsgProcessor")
    private DestroyedMsgProcessor destroyedMsgProcessor;

    @BeanInject(value = "destroyCollectionSrcImgProcessor")
    private DestroyDerivativesProcessor destroyCollectionSrcImgProcessor;

    @BeanInject(value = "destroySmallThumbnailProcessor")
    private DestroyDerivativesProcessor destroySmallThumbnailProcessor;

    @BeanInject(value = "destroyLargeThumbnailProcessor")
    private DestroyDerivativesProcessor destroyLargeThumbnailProcessor;

    @BeanInject(value = "destroyAccessCopyProcessor")
    private DestroyDerivativesProcessor destroyAccessCopyProcessor;

    @BeanInject(value = "destroyFulltextProcessor")
    private DestroyDerivativesProcessor destroyFulltextProcessor;

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
                .redeliveryDelay("{{error.retryDelay}}")
                .maximumRedeliveries("{{error.maxRedeliveries}}")
                .backOffMultiplier("{{error.backOffMultiplier}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("{{cdr.destroy.derivatives.stream.camel}}")
                .routeId("CdrDestroyDerivatives")
                .startupOrder(204)
                .log(LoggingLevel.DEBUG, log, "Received destroy derivatives message")
                .process(destroyedMsgProcessor)
                .choice()
                    .when(method(ImageDerivativeProcessor.class, "allowedImageType"))
                        .to("direct:image.derivatives.destroy")
                    .when(method(FulltextProcessor.class, "allowedTextType"))
                        .to("direct:fulltext.derivatives.destroy")
                .end();

        from("direct:fulltext.derivatives.destroy")
                .routeId("CdrDestroyFullText")
                .startupOrder(203)
                .log(LoggingLevel.DEBUG, log, "Destroying derivative text files")
                .bean(destroyFulltextProcessor);

        from("direct:image.derivatives.destroy")
                .routeId("CdrDestroyImage")
                .startupOrder(202)
                .log(LoggingLevel.DEBUG, log, "Destroying derivative thumbnails")
                .bean(destroySmallThumbnailProcessor)
                .bean(destroyLargeThumbnailProcessor)
                .choice()
                    .when(simple("${headers['" + CdrObjectType + "']} == '" + Cdr.FileObject.getURI() + "'"))
                        .to("direct:image.access.destroy")
                    .when(simple("${headers['CollectionThumb']} != null"))
                        .to("direct:image.collection.destroy")
                .end();

        from("direct:image.access.destroy")
                .routeId("CdrDestroyAccessCopy")
                .startupOrder(201)
                .log(LoggingLevel.DEBUG, log, "Destroying access copy")
                .bean(destroyAccessCopyProcessor);

        from("direct:image.collection.destroy")
                .routeId("CdrDestroyCollectionUpload")
                .startupOrder(200)
                .log(LoggingLevel.DEBUG, log, "Destroying collection image upload")
                .bean(destroyCollectionSrcImgProcessor);
    }
}
