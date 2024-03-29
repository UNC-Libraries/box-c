package edu.unc.lib.boxc.services.camel.destroyDerivatives;

import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrObjectType;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.services.camel.fulltext.FulltextProcessor;
import edu.unc.lib.boxc.services.camel.images.ImageDerivativeProcessor;

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
