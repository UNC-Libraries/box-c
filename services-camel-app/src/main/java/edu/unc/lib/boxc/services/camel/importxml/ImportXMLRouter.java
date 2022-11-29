package edu.unc.lib.boxc.services.camel.importxml;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

/**
 * Route which executes requests to perform bulk xml imports.
 *
 * @author bbpennel
 *
 */
public class ImportXMLRouter extends RouteBuilder {
    private static final Logger log = getLogger(ImportXMLRouter.class);

    @BeanInject(value = "importXMLProcessor")
    private ImportXMLProcessor importXmlProcessor;

    @Override
    public void configure() throws Exception {
        from("{{cdr.importxml.stream.camel}}")
            .routeId("CdrImportXML")
            .log(DEBUG, log, "Received import xml message")
            .bean(importXmlProcessor);
    }
}
