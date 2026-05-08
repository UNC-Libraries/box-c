package edu.unc.lib.boxc.services.camel.pdf;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Router for processing requests to create of PDFs with OCR
 * @author krwong
 */
public class PdfEnhancementsRouter extends RouteBuilder {
    private static final Logger log = getLogger(PdfEnhancementsRouter.class);

    @BeanInject("pdfDerivativeProcessor")
    private PdfDerivativeProcessor pdfDerivativeProcessor;

    private String pdfDerivativesStreamCamel;

    /**
     * Configure the pdf enhancement route workflow.
     */
    @Override
    public void configure() throws Exception {
        from(pdfDerivativesStreamCamel)
                .routeId("PdfAccessCopy")
                .log(LoggingLevel.DEBUG, log, "Received PDF with OCR generation request")
                .bean(pdfDerivativeProcessor)
                .end();
    }

    public void setPdfDerivativeProcessor(PdfDerivativeProcessor pdfDerivativeProcessor) {
        this.pdfDerivativeProcessor = pdfDerivativeProcessor;
    }

    @PropertyInject("cdr.enhancement.pdf.stream.camel")
    public void setPdfDerivativesStreamCamel(String pdfDerivativesStreamCamel) {
        this.pdfDerivativesStreamCamel = pdfDerivativesStreamCamel;
    }
}
