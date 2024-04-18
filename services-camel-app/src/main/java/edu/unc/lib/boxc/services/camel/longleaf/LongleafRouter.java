package edu.unc.lib.boxc.services.camel.longleaf;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import edu.unc.lib.boxc.services.camel.AddFailedRouteProcessor;

/**
 * Router for longleaf operations
 *
 * @author bbpennel
 */
public class LongleafRouter extends RouteBuilder {
    private static final Logger log = getLogger(LongleafRouter.class);

    @BeanInject(value = "getUrisProcessor")
    private GetUrisProcessor getUrisProcessor;

    @BeanInject(value = "registerLongleafProcessor")
    private RegisterToLongleafProcessor registerProcessor;

    @BeanInject(value = "deregisterLongleafProcessor")
    private DeregisterLongleafProcessor deregisterProcessor;

    @Value("${longleaf.maxRedelivieries:3}")
    private int longleafMaxRedelivieries;

    @Value("${longleaf.redeliveryDelay:10000}")
    private long longleafRedeliveryDelay;

    @Override
    public void configure() throws Exception {
        AddFailedRouteProcessor failedRouteProcessor = new AddFailedRouteProcessor();

        errorHandler(deadLetterChannel("{{longleaf.dlq.dest}}")
                .maximumRedeliveries(longleafMaxRedelivieries)
                .redeliveryDelay(longleafRedeliveryDelay)
                .onPrepareFailure(failedRouteProcessor));

        from("direct-vm:filter.longleaf")
            .routeId("RegisterLongleafQueuing")
            .startupOrder(4)
            .filter().method(RegisterToLongleafProcessor.class, "registerableBinary")
            .log(LoggingLevel.DEBUG, log, "Queuing ${headers[CamelFcrepoUri]} for registration to longleaf")
            .to("sjms:register.longleaf?transacted=true");

        from("{{longleaf.register.consumer}}")
            .to("direct:register.longleaf");
        from("activemq://activemq:queue:longleaf.register.batch")
            .to("direct:register.longleaf");
        from("direct:register.longleaf")
            .routeId("RegisterLongleafProcessing")
            .startupOrder(3)
            .log(LoggingLevel.DEBUG, log, "Processing batch of longleaf registrations")
            .bean(registerProcessor);

        from("activemq://activemq:queue:filter.longleaf.deregister")
            .routeId("DeregisterLongleafQueuing")
            .startupOrder(2)
            .log(LoggingLevel.DEBUG, log, "Queuing ${body} for deregistration in longleaf")
            .process(getUrisProcessor)
            .to("sjms:deregister.longleaf?transacted=true");

        from("{{longleaf.deregister.consumer}}")
            .to("direct:deregister.longleaf");
        from("activemq://activemq:queue:longleaf.deregister.batch")
            .to("direct:deregister.longleaf");
        from("direct:deregister.longleaf")
            .routeId("DeregisterLongleafProcessing")
            .startupOrder(1)
            .log(LoggingLevel.DEBUG, log, "Processing batch of longleaf deregistrations")
            .bean(deregisterProcessor);
    }
}
