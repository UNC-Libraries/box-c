package edu.unc.lib.boxc.services.camel.views;

import org.apache.camel.BeanInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Router for processing requests to update view behavior of UV
 *
 * @author snluong
 */
public class ViewBehaviorRouter extends RouteBuilder {
    private static final Logger log = getLogger(ViewBehaviorRouter.class);

    @BeanInject(value = "viewBehaviorRequestProcessor")
    private ViewBehaviorRequestProcessor viewBehaviorRequestProcessor;

    @Override
    public void configure() throws Exception {
        from("{{cdr.viewbehavior.stream.camel}}")
                .routeId("DcrViewBehavior")
                .log(DEBUG, log, "Received view behavior request")
                .bean(viewBehaviorRequestProcessor);
    }
}
