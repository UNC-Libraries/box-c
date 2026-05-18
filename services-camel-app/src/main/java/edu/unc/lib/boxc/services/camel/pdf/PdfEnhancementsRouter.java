package edu.unc.lib.boxc.services.camel.pdf;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Router for processing requests to create aggregate PDFs with OCR
 * @author krwong
 */
public class PdfEnhancementsRouter extends RouteBuilder {
    private static final Logger log = getLogger(PdfEnhancementsRouter.class);

    @BeanInject("aggregatePdfProcessor")
    private AggregatePdfProcessor aggregatePdfProcessor;

    private String aggregatePdfStreamCamel;

    /**
     * Configure the pdf enhancement route workflow.
     */
    @Override
    public void configure() throws Exception {
        from(aggregatePdfStreamCamel)
                .routeId("AggregatePdf")
                .log(LoggingLevel.DEBUG, log, "Received aggregate PDF with OCR generation request")
                .bean(aggregatePdfProcessor)
                .end();
    }

    public void setAggregatePdfProcessor(AggregatePdfProcessor aggregatePdfProcessor) {
        this.aggregatePdfProcessor = aggregatePdfProcessor;
    }

    @PropertyInject("cdr.enhancement.pdf.stream.camel")
    public void setAggregatePdfStreamCamel(String aggregatePdfStreamCamel) {
        this.aggregatePdfStreamCamel = aggregatePdfStreamCamel;
    }
}
