package edu.unc.lib.boxc.services.camel.streaming;

import org.apache.camel.BeanInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Router for processing requests to update streaming properties of file object
 */
public class StreamingPropertiesRouter  extends RouteBuilder {
    private static final Logger log = getLogger(StreamingPropertiesRouter.class);
    @BeanInject(value = "streamingPropertiesRequestProcessor")
    private StreamingPropertiesRequestProcessor processor;

    @Override
    public void configure() throws Exception {
        from("{{cdr.streamingproperties.stream.camel}}")
                .routeId("DcrStreaming")
                .log(DEBUG, log, "Received streaming properties request")
                .bean(processor);
    }
}
