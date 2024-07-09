package edu.unc.lib.boxc.services.camel.order;

import org.apache.camel.BeanInject;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Router for processing requests to order members of containers
 *
 * @author bbpennel
 */
public class OrderMembersRouter extends RouteBuilder {
    private static final Logger log = getLogger(OrderMembersRouter.class);

    @BeanInject(value = "orderRequestProcessor")
    private OrderRequestProcessor orderRequestProcessor;

    private String orderMembersStream;

    @Override
    public void configure() throws Exception {
        from(orderMembersStream)
                .routeId("DcrOrderMembers")
                .log(DEBUG, log, "Received order members request")
                .bean(orderRequestProcessor);
    }

    public void setOrderRequestProcessor(OrderRequestProcessor orderRequestProcessor) {
        this.orderRequestProcessor = orderRequestProcessor;
    }

    @PropertyInject("cdr.ordermembers.stream.camel")
    public void setOrderMembersStream(String orderMembersStream) {
        this.orderMembersStream = orderMembersStream;
    }
}
