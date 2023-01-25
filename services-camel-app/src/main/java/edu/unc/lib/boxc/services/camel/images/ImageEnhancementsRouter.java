package edu.unc.lib.boxc.services.camel.images;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.JavaUuidGenerator;
import org.apache.camel.spi.UuidGenerator;
import org.slf4j.Logger;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;

/**
 * Router which triggers the creation of thumbnails when applicable binaries
 * are written.
 *
 * @author lfarrell
 */
public class ImageEnhancementsRouter extends RouteBuilder {
    private static final Logger log = getLogger(ImageEnhancementsRouter.class);

    @BeanInject(value = "addSmallThumbnailProcessor")
    private AddDerivativeProcessor addSmallThumbnailProcessor;

    @BeanInject(value = "addLargeThumbnailProcessor")
    private AddDerivativeProcessor addLargeThumbProcessor;

    @BeanInject(value = "addAccessCopyProcessor")
    private AddDerivativeProcessor addAccessCopyProcessor;

    private UuidGenerator uuidGenerator;

    /**
     * Configure the thumbnail route workflow.
     */
    @Override
    public void configure() throws Exception {
        ImageDerivativeProcessor imageDerivProcessor = new ImageDerivativeProcessor();

        uuidGenerator = new JavaUuidGenerator();

        onException(RepositoryException.class)
                .redeliveryDelay("{{error.retryDelay}}")
                .maximumRedeliveries("{{error.maxRedeliveries}}")
                .backOffMultiplier("{{error.backOffMultiplier}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("direct:process.enhancement.thumbnails")
            .routeId("ProcessThumbnails")
            .startupOrder(23)
            .log(LoggingLevel.INFO, log, "Thumbs ${headers[CdrBinaryPath]} with ${headers[CdrMimeType]}")
            .filter().method(imageDerivProcessor, "allowedImageType")
                .log(LoggingLevel.INFO, log, "Generating thumbnails for ${headers[org.fcrepo.jms.identifier]}"
                        + " of type ${headers[CdrMimeType]}")
                .bean(imageDerivProcessor)
                // Generate an random identifier to avoid derivative collisions
                .bean(uuidGenerator)
                .multicast()
                .shareUnitOfWork()
                .to("direct:small.thumbnail", "direct:large.thumbnail");

        from("direct:small.thumbnail")
            .routeId("SmallThumbnail")
            .startupOrder(22)
            .log(LoggingLevel.INFO, log, "Creating/Updating Small Thumbnail for ${headers[CdrImagePath]}")
            .filter().method(addSmallThumbnailProcessor, "needsRun")
                .setHeader(CdrFcrepoHeaders.CdrTempPath, simple("${properties:services.tempDirectory}/${body}-small"))
                .doTry()
                    .recipientList(simple("exec:/bin/sh?args=${properties:cdr.enhancement.bin}/convertScaleStage.sh "
                            + "${headers[CdrImagePath]} png 64 64 ${headers[CdrTempPath]}"))
                    .bean(addSmallThumbnailProcessor)
                .endDoTry()
                .doFinally()
                    // Ensure temp files get cleaned up in case of failure
                    .bean(addSmallThumbnailProcessor, "cleanupTempFile")
                .end();


        from("direct:large.thumbnail")
            .routeId("LargeThumbnail")
            .startupOrder(21)
            .log(LoggingLevel.INFO, log, "Creating/Updating Large Thumbnail for ${headers[CdrImagePath]}")
            .filter().method(addLargeThumbProcessor, "needsRun")
                .setHeader(CdrFcrepoHeaders.CdrTempPath, simple("${properties:services.tempDirectory}/${body}-large"))
                .doTry()
                    .recipientList(simple("exec:/bin/sh?args=${properties:cdr.enhancement.bin}/convertScaleStage.sh "
                            + "${headers[CdrImagePath]} png 128 128 ${headers[CdrTempPath]}"))
                    .bean(addLargeThumbProcessor)
                .endDoTry()
                .doFinally()
                    .bean(addLargeThumbProcessor, "cleanupTempFile")
                .end();

        from("direct:process.enhancement.imageAccessCopy")
            .routeId("AccessCopy")
            .startupOrder(20)
            .log(LoggingLevel.DEBUG, log, "Access copy triggered")
            .filter().method(addAccessCopyProcessor, "needsRun")
            .filter().method(imageDerivProcessor, "allowedImageType")
                .bean(imageDerivProcessor)
                .log(LoggingLevel.INFO, log, "Creating/Updating JP2 access copy for ${headers[CdrImagePath]}")
                // Generate an random identifier to avoid derivative collisions
                .bean(uuidGenerator)
                .setHeader(CdrFcrepoHeaders.CdrTempPath, simple("${properties:services.tempDirectory}/${body}-access"))
                .doTry()
                    .recipientList(simple("exec:/bin/sh?args=${properties:cdr.enhancement.bin}/convertJp2.sh "
                            + "${headers[CdrImagePath]} jp2 ${headers[CdrTempPath]}"))
                    .bean(addAccessCopyProcessor)
                .endDoTry()
                .doFinally()
                    .bean(addAccessCopyProcessor, "cleanupTempFile")
                .end();
    }
}
