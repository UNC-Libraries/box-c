package edu.unc.lib.boxc.services.camel.video;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.services.camel.images.AddDerivativeProcessor;
import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.UuidGenerator;
import org.apache.camel.support.DefaultUuidGenerator;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Router which triggers the creation of video derivatives
 * @author krwong
 */
public class VideoEnhancementsRouter extends RouteBuilder {
    private static final Logger log = getLogger(VideoEnhancementsRouter.class);

    private static final int CACHE_INVALIDATE_THREADS = 5;
    private static final int CACHE_INVALIDATE_REQUESTS_PER_SEC = 10;

    @BeanInject(value = "addVideoAccessCopyProcessor")
    private AddDerivativeProcessor addVideoAccessCopyProcessor;

    @BeanInject("mp44uVideoProcessor")
    private Mp44uVideoProcessor mp44uVideoProcessor;

    private UuidGenerator uuidGenerator;

    /**
     * Configure the video enhancement route workflow.
     */
    @Override
    public void configure() throws Exception {
        VideoDerivativeProcessor videoDerivProcessor = new VideoDerivativeProcessor();

        uuidGenerator = new DefaultUuidGenerator();

        onException(AddDerivativeProcessor.DerivativeGenerationException.class)
                .maximumRedeliveries(0)
                .log(LoggingLevel.ERROR, "${exception.message}");

        onException(RepositoryException.class)
                .redeliveryDelay("{{error.retryDelay}}")
                .maximumRedeliveries("{{error.maxRedeliveries}}")
                .backOffMultiplier("{{error.backOffMultiplier}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("{{cdr.enhancement.video.stream.camel}}")
            .routeId("VideoAccessCopy")
            .startupOrder(21)
            .log(LoggingLevel.DEBUG, log, "Access copy triggered")
            .filter().method(addVideoAccessCopyProcessor, "needsRun")
            .filter().method(videoDerivProcessor, "allowedVideoType")
                .bean(videoDerivProcessor)
                .log(LoggingLevel.INFO, log, "Creating/Updating MP4 access copy for ${headers[CdrVideoPath]}")
                // Generate an random identifier to avoid derivative collisions
                .setBody(exchange -> uuidGenerator.generateUuid())
                .setHeader(CdrFcrepoHeaders.CdrTempPath, simple("${properties:services.tempDirectory}/${body}-video"))
                .doTry()
                    .bean(mp44uVideoProcessor)
                    .bean(addVideoAccessCopyProcessor)
                .endDoTry()
                .doFinally()
                    .bean(addVideoAccessCopyProcessor, "cleanupTempFile")
                .end()
                .to("direct:solrIndexing")
            .end();
    }
}
