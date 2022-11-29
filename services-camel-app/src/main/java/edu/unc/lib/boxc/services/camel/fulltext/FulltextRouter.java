package edu.unc.lib.boxc.services.camel.fulltext;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import edu.unc.lib.boxc.services.camel.images.AddDerivativeProcessor;

/**
 * Routes ingests with full text available through a pipeline to extract
 * the full text and add it as an object in fedora.
 *
 * @author lfarrell
 *
 */
public class FulltextRouter extends RouteBuilder {
    private static final Logger log = getLogger(FulltextRouter.class);

    @BeanInject(value = "fulltextProcessor")
    private FulltextProcessor ftProcessor;

    @BeanInject(value = "addFullTextDerivativeProcessor")
    private AddDerivativeProcessor adProcessor;

    @Override
    public void configure() throws Exception {

        from("direct:process.enhancement.extractFulltext")
            .routeId("CdrServiceFulltextExtraction")
            .startupOrder(31)
            .log(LoggingLevel.DEBUG, log, "Calling text extraction route for ${headers[CamelFcrepoUri]}")
            .filter().method(adProcessor, "needsRun")
            .filter().method(FulltextProcessor.class, "allowedTextType")
            .log(LoggingLevel.INFO, log, "Extracting text from ${headers[CamelFcrepoUri]}"
                    + " of type ${headers[CdrMimeType]}")
            .to("direct:fulltext.extraction");

        from("direct:fulltext.extraction")
            .routeId("ExtractingText")
            .startupOrder(30)
            .log(LoggingLevel.DEBUG, log, "Extracting full text for ${headers[CdrBinaryPath]}")
            .bean(ftProcessor);
    }
}
