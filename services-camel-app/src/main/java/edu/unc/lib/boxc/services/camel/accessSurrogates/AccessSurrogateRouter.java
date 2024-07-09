package edu.unc.lib.boxc.services.camel.accessSurrogates;

import org.apache.camel.BeanInject;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Router for processing requests to update access surrogates
 */
public class AccessSurrogateRouter extends RouteBuilder {
    private static final Logger log = getLogger(AccessSurrogateRouter.class);

    @BeanInject(value = "accessSurrogateRequestProcessor")
    private AccessSurrogateRequestProcessor processor;

    private String accessSurrogatesStreamCamel;

    @Override
    public void configure() throws Exception {
        from(accessSurrogatesStreamCamel)
                .routeId("DcrAccessSurrogates")
                .log(DEBUG, log, "Received access surrogate request")
                .bean(processor);
    }

    public void setAccessSurrogateRequestProcessor(AccessSurrogateRequestProcessor processor) {
        this.processor = processor;
    }

    @PropertyInject("cdr.access.surrogates.stream.camel")
    public void setAccessSurrogatesStreamCamel(String accessSurrogatesStreamCamel) {
        this.accessSurrogatesStreamCamel = accessSurrogatesStreamCamel;
    }
}
