package edu.unc.lib.boxc.services.camel.images;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.UuidGenerator;
import org.apache.camel.support.DefaultUuidGenerator;
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

    private static final int CACHE_INVALIDATE_THREADS = 5;
    private static final int CACHE_INVALIDATE_REQUESTS_PER_SEC = 10;

    @BeanInject(value = "addAccessCopyProcessor")
    private AddDerivativeProcessor addAccessCopyProcessor;

    @BeanInject(value = "imageCacheInvalidationProcessor")
    private ImageCacheInvalidationProcessor imageCacheInvalidationProcessor;

    @BeanInject(value = "jp2Processor")
    private Jp2Processor jp2Processor;

    @BeanInject(value = "pdfImageProcessor")
    private PdfImageProcessor pdfImageProcessor;

    @BeanInject(value = "imageDerivativeProcessor")
    private ImageDerivativeProcessor imageDerivativeProcessor;

    private UuidGenerator uuidGenerator;

    /**
     * Configure the thumbnail route workflow.
     */
    @Override
    public void configure() throws Exception {
        uuidGenerator = new DefaultUuidGenerator();

        onException(AddDerivativeProcessor.DerivativeGenerationException.class)
                .maximumRedeliveries(0)
                .log(LoggingLevel.ERROR, "${exception.message}");

        onException(RepositoryException.class)
                .redeliveryDelay("{{error.retryDelay}}")
                .maximumRedeliveries("{{error.maxRedeliveries}}")
                .backOffMultiplier("{{error.backOffMultiplier}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("direct:process.enhancement.imageAccessCopy")
            .routeId("AccessCopy")
            .startupOrder(20)
            .log(LoggingLevel.DEBUG, log, "Access copy triggered ${headers[CdrMimeType]} for ${headers[CdrBinaryPath]}")
            .filter().method(addAccessCopyProcessor, "needsRun")
                .choice()
                    .when().method(PdfImageProcessor.class, "isPdfFile")
                        .log(LoggingLevel.DEBUG, log, "Binary is a PDF, generating image of first page")
                        .bean(pdfImageProcessor)
                    .when().method(ImageDerivativeProcessor.class, "allowedImageType")
                        .log(LoggingLevel.DEBUG, log, "Binary is an image")
                        .bean(imageDerivativeProcessor)
                .end()
                .filter(simple("${headers[CdrImagePath]} != null"))
                    .log(LoggingLevel.INFO, log, "Creating/Updating JP2 access copy for ${headers[CdrImagePath]}")
                    // Generate an random identifier to avoid derivative collisions
                    .setBody(exchange -> uuidGenerator.generateUuid())
                    .setHeader(CdrFcrepoHeaders.CdrTempPath, simple("${properties:services.tempDirectory}/${body}-access"))
                    .doTry()
                        .bean(jp2Processor)
                        .bean(addAccessCopyProcessor)
                        // Process cache invalidation asynchronously with a limited number of threads
                        .threads(CACHE_INVALIDATE_THREADS)
                            // Limit the max number of requests per second
                            .throttle(CACHE_INVALIDATE_REQUESTS_PER_SEC)
                            .bean(imageCacheInvalidationProcessor)
                        .end()
                    .endDoTry()
                    .doFinally()
                        .bean(addAccessCopyProcessor, "cleanupTempFile")
                        .bean(imageDerivativeProcessor, "cleanupTempImage")
                    .end()
                .end()
            .end();
    }
}
