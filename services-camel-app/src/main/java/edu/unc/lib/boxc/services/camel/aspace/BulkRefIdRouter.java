package edu.unc.lib.boxc.services.camel.aspace;

import org.apache.camel.BeanInject;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

public class BulkRefIdRouter extends RouteBuilder {
    private static final Logger log = getLogger(BulkRefIdRouter.class);
    @BeanInject(value = "bulkRefIdRequestProcessor")
    private BulkRefIdRequestProcessor processor;

    @Override
    public void configure() throws Exception {
        from("{{cdr.bulk.refid.stream.camel}}")
                .routeId("DcrBulkRefId")
                .log(DEBUG, log, "Received bulk ref ID request")
                .bean(processor);
    }

    public void setProcessor(BulkRefIdRequestProcessor processor) {
        this.processor = processor;
    }
}
