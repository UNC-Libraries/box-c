package edu.unc.lib.boxc.services.camel.audio;

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
 * Router which triggers the creation of audio derivatives
 * @author krwong
 */
public class AudioEnhancementsRouter extends RouteBuilder {
    private static final Logger log = getLogger(AudioEnhancementsRouter.class);

    @BeanInject("addAudioAccessCopyProcessor")
    private AddDerivativeProcessor addAudioAccessCopyProcessor;

    private UuidGenerator uuidGenerator;

    /**
     * Configure the audio enhancement route workflow.
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
                .routeId("AudioAccessCopy")
                .startupOrder(25)
                .log(LoggingLevel.DEBUG, log, "Access copy triggered")
                .filter().method(addAudioAccessCopyProcessor, "needsRun")
                .filter().method(audioDerivProcessor, "allowedAudioType")
                    .bean(audioDerivProcessor)
                    .log(LoggingLevel.INFO, log, "Creating/Updating AAC access copy for ${headers[CdrAudioPath]}")
                    // Generate an random identifier to avoid derivative collisions
                    .setBody(exchange -> uuidGenerator.generateUuid())
                    .setHeader(CdrFcrepoHeaders.CdrTempPath, simple("${properties:services.tempDirectory}/${body}-audio"))
                    .doTry()
                        .recipientList(simple("exec:/bin/sh?args=${properties:cdr.enhancement.bin}/convertAudio.sh "
                                + "${headers[CdrAudioPath]} ${headers[CdrTempPath]}"))
                        .bean(addAudioAccessCopyProcessor)
                    .endDoTry()
                    .doFinally()
                        .bean(addAudioAccessCopyProcessor, "cleanupTempFile")
                .end();
    }
}
