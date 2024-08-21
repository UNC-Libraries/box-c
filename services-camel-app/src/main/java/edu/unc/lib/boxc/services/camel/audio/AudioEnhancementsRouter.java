package edu.unc.lib.boxc.services.camel.audio;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.services.camel.images.AddDerivativeProcessor;
import edu.unc.lib.boxc.services.camel.thumbnails.ThumbnailRequestProcessor;
import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.UuidGenerator;
import org.apache.camel.support.DefaultUuidGenerator;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class AudioEnhancementsRouter extends RouteBuilder {
    private static final Logger log = getLogger(AudioEnhancementsRouter.class);

    @BeanInject(value = "addAccessCopyProcessor")
    private AddDerivativeProcessor addAccessCopyProcessor;

    private UuidGenerator uuidGenerator;

    /**
     * Configure the thumbnail route workflow.
     */
    @Override
    public void configure() throws Exception {
        AudioDerivativeProcessor audioDerivProcessor = new AudioDerivativeProcessor();

        uuidGenerator = new DefaultUuidGenerator();

        onException(RepositoryException.class)
                .redeliveryDelay("{{error.retryDelay}}")
                .maximumRedeliveries("{{error.maxRedeliveries}}")
                .backOffMultiplier("{{error.backOffMultiplier}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("direct:process.enhancement.audioAccessCopy")
                .routeId("AccessCopy")
                .startupOrder(20)
                .log(LoggingLevel.DEBUG, log, "Access copy triggered")
                .filter().method(addAccessCopyProcessor, "needsRun")
                .filter().method(audioDerivProcessor, "allowedAudioType")
                    .bean(audioDerivProcessor)
                    .log(LoggingLevel.INFO, log, "Creating/Updating MP3 access copy for ${headers[CdrAudioPath]}")
                    // Generate an random identifier to avoid derivative collisions
                    .setBody(exchange -> uuidGenerator.generateUuid())
                    .setHeader(CdrFcrepoHeaders.CdrTempPath, simple("${properties:services.tempDirectory}/${body}-access"))
                    .doTry()
                        .recipientList(simple("exec:/bin/sh?args=${properties:cdr.enhancement.bin}/convertWav.sh "
                                + "${headers[CdrAudioPath]} ${headers[CdrTempPath]}"))
                        .bean(addAccessCopyProcessor)
                    .endDoTry()
                    .doFinally()
                        .bean(addAccessCopyProcessor, "cleanupTempFile")
                .end();
    }
}
