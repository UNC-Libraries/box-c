package edu.unc.lib.boxc.services.camel.destroyDerivatives;

import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrObjectType;
import static org.slf4j.LoggerFactory.getLogger;

import edu.unc.lib.boxc.services.camel.audio.AudioDerivativeProcessor;
import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
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

    @BeanInject(value = "destroyAudioProcessor")
    private DestroyDerivativesProcessor destroyAudioProcessor;

    private String destroyDerivativesStreamCamel;
    private long errorRetryDelay;
    private int errorMaxRedeliveries;
    private int errorBackOffMultiplier;

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
                .redeliveryDelay(errorRetryDelay)
                .maximumRedeliveries(errorMaxRedeliveries)
                .backOffMultiplier(errorBackOffMultiplier)
                .retryAttemptedLogLevel(LoggingLevel.WARN);

        from(destroyDerivativesStreamCamel)
                .routeId("CdrDestroyDerivatives")
                .startupOrder(204)
                .log(LoggingLevel.DEBUG, log, "Received destroy derivatives message")
                .process(destroyedMsgProcessor)
                .choice()
                    .when(method(ImageDerivativeProcessor.class, "allowedImageType"))
                        .to("direct:image.derivatives.destroy")
                    .when(method(FulltextProcessor.class, "allowedTextType"))
                        .to("direct:fulltext.derivatives.destroy")
                    .when(method(AudioDerivativeProcessor.class, "allowedAudioType"))
                        .to("direct:audio.derivatives.destroy")
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

        from("direct:audio.derivatives.destroy")
                .routeId("CdrDestroyAudio")
                .startupOrder(199)
                .log(LoggingLevel.DEBUG, log, "Destroying derivative audio files")
                .bean(destroyAudioProcessor);
    }

    public void setDestroyedMsgProcessor(DestroyedMsgProcessor destroyedMsgProcessor) {
        this.destroyedMsgProcessor = destroyedMsgProcessor;
    }

    public void setDestroyCollectionSrcImgProcessor(DestroyDerivativesProcessor destroyCollectionSrcImgProcessor) {
        this.destroyCollectionSrcImgProcessor = destroyCollectionSrcImgProcessor;
    }

    public void setDestroySmallThumbnailProcessor(DestroyDerivativesProcessor destroySmallThumbnailProcessor) {
        this.destroySmallThumbnailProcessor = destroySmallThumbnailProcessor;
    }

    public void setDestroyLargeThumbnailProcessor(DestroyDerivativesProcessor destroyLargeThumbnailProcessor) {
        this.destroyLargeThumbnailProcessor = destroyLargeThumbnailProcessor;
    }

    public void setDestroyAccessCopyProcessor(DestroyDerivativesProcessor destroyAccessCopyProcessor) {
        this.destroyAccessCopyProcessor = destroyAccessCopyProcessor;
    }

    public void setDestroyFulltextProcessor(DestroyDerivativesProcessor destroyFulltextProcessor) {
        this.destroyFulltextProcessor = destroyFulltextProcessor;
    }

    public void setDestroyAudioProcessor(DestroyDerivativesProcessor destroyAudioProcessor) {
        this.destroyAudioProcessor = destroyAudioProcessor;
    }

    @PropertyInject("cdr.destroy.derivatives.stream.camel")
    public void setDestroyDerivativesStreamCamel(String destroyDerivativesStreamCamel) {
        this.destroyDerivativesStreamCamel = destroyDerivativesStreamCamel;
    }

    @PropertyInject("error.retryDelay:10000")
    public void setErrorRetryDelay(long errorRetryDelay) {
        this.errorRetryDelay = errorRetryDelay;
    }

    @PropertyInject("error.maxRedeliveries:3")
    public void setErrorMaxRedeliveries(int errorMaxRedeliveries) {
        this.errorMaxRedeliveries = errorMaxRedeliveries;
    }

    @PropertyInject("error.backOffMultiplier:2")
    public void setErrorBackOffMultiplier(int errorBackOffMultiplier) {
        this.errorBackOffMultiplier = errorBackOffMultiplier;
    }
}
