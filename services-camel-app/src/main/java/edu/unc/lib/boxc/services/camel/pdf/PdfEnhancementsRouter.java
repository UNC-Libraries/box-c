package edu.unc.lib.boxc.services.camel.pdf;

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
 * Router which triggers the creation of PDFs with OCR
 * @author krwong
 */
public class PdfEnhancementsRouter extends RouteBuilder {
    private static final Logger log = getLogger(PdfEnhancementsRouter.class);

    @BeanInject(value = "addVideoAccessCopyProcessor")
    private AddDerivativeProcessor addDerivativeProcessor;

    @BeanInject("pdf4uProcessor")
    private Pdf4uProcessor pdf4uProcessor;

    private UuidGenerator uuidGenerator;

    /**
     * Configure the video enhancement route workflow.
     */
    @Override
    public void configure() throws Exception {
        PdfDerivativeProcessor pdfDerivativeProcessor = new PdfDerivativeProcessor();

        uuidGenerator = new DefaultUuidGenerator();

        onException(AddDerivativeProcessor.DerivativeGenerationException.class)
                .handled(true)
                .maximumRedeliveries(0)
                .log(LoggingLevel.ERROR, "${exception.message}");

        onException(RepositoryException.class)
                .redeliveryDelay("{{error.retryDelay}}")
                .maximumRedeliveries("{{error.maxRedeliveries}}")
                .backOffMultiplier("{{error.backOffMultiplier}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("{{cdr.enhancement.video.stream.camel}}")
                .routeId("PdfAccessCopy")
                .startupOrder(21)
                .log(LoggingLevel.DEBUG, log, "Access copy triggered")
                .filter().method(addDerivativeProcessor, "needsRun")
                .filter().method(pdfDerivativeProcessor, "allowedPdfType")
                    .bean(pdfDerivativeProcessor)
                    .log(LoggingLevel.INFO, log, "Creating/Updating PDF access copy for ${headers[CdrPdfPath]}")
                    // Generate an random identifier to avoid derivative collisions
                    .setBody(exchange -> uuidGenerator.generateUuid())
                    .setHeader(CdrFcrepoHeaders.CdrTempPath, simple("${properties:services.tempDirectory}/${body}-pdf"))
                    .doTry()
                        .bean(pdf4uProcessor)
                        .bean(addDerivativeProcessor)
                    .endDoTry()
                    .doFinally()
                    .   bean(addDerivativeProcessor, "cleanupTempFile")
                    .end()
                    .to("direct:solrIndexing")
                .end();
    }
}
