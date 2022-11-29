package edu.unc.lib.boxc.services.camel.exportxml;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

/**
 * Route which executes requests to perform bulk xml exports.
 *
 * @author bbpennel
 */
public class ExportXMLRouter extends RouteBuilder {

    private static final Logger log = getLogger(ExportXMLRouter.class);

    @BeanInject(value = "exportXMLProcessor")
    private ExportXMLProcessor exportXmlProcessor;

    @Override
    public void configure() throws Exception {
        from("{{cdr.exportxml.stream.camel}}")
            .routeId("CdrExportXML")
            .log(DEBUG, log, "Received export xml request")
            .bean(exportXmlProcessor);
    }
}
