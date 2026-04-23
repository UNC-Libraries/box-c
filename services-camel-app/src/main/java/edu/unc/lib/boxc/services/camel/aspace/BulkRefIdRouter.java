package edu.unc.lib.boxc.services.camel.aspace;

import org.apache.camel.BeanInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static org.apache.camel.LoggingLevel.INFO;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Router for processing requests to assign Ref IDs to WorkObjects
 *
 * @author snluong
 */
public class BulkRefIdRouter extends RouteBuilder {
    private static final Logger log = getLogger(BulkRefIdRouter.class);
    @BeanInject("bulkRefIdRequestProcessor")
    private BulkRefIdRequestProcessor processor;

    @Override
    public void configure() throws Exception {
        from("{{cdr.bulk.refid.stream.camel}}")
                .routeId("DcrBulkRefId")
                .log(INFO, log, "Received bulk ref ID request")
                .bean(processor);
    }

    public void setProcessor(BulkRefIdRequestProcessor processor) {
        this.processor = processor;
    }
}
