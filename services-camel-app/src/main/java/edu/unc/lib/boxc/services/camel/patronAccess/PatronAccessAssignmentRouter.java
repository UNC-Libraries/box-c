package edu.unc.lib.boxc.services.camel.patronAccess;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

/**
 * Route which executes requests to update patron access control
 *
 * @author bbpennel
 */
public class PatronAccessAssignmentRouter extends RouteBuilder {
    private static final Logger log = getLogger(PatronAccessAssignmentRouter.class);

    @BeanInject(value = "patronAccessAssignmentProcessor")
    private PatronAccessAssignmentProcessor patronAccessAssignmentProcessor;

    @Override
    public void configure() throws Exception {
        from("{{cdr.patron.access.assignment.stream.camel}}")
            .routeId("CdrUpdatePatronAccess")
            .log(DEBUG, log, "Received patron access assignment message")
            .bean(patronAccessAssignmentProcessor);
    }
}
