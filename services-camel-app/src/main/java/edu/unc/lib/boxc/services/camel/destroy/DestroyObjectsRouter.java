package edu.unc.lib.boxc.services.camel.destroy;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

/**
 * Route to execute requests to destroy repository objects
 *
 * @author bbpennel
 *
 */
public class DestroyObjectsRouter extends RouteBuilder {
    private static final Logger log = getLogger(DestroyObjectsRouter.class);

    @BeanInject(value = "destroyObjectsProcessor")
    private DestroyObjectsProcessor destroyObjectsProcessor;

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
                .redeliveryDelay("{{error.retryDelay}}")
                .maximumRedeliveries("{{error.maxRedeliveries}}")
                .backOffMultiplier("{{error.backOffMultiplier}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("{{cdr.destroy.stream.camel}}")
            .routeId("CdrDestroyObjects")
            .log(DEBUG, log, "Received destroy objects message")
            .bean(destroyObjectsProcessor);

        from("{{cdr.destroy.post.stream.camel}}")
            .routeId("CdrDestroyObjectsCleanup")
            .log(DEBUG, log, "Received destroy objects cleanup message")
            .multicast()
            .parallelProcessing()
            .to("activemq://activemq:queue:filter.longleaf.deregister",
                    "{{cdr.destroy.derivatives.stream.camel}}");
    }
}
