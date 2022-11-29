package edu.unc.lib.boxc.services.camel.binaryCleanup;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Router for tasks to cleanup old binary files
 * @author bbpennel
 */
public class BinaryCleanupRouter extends RouteBuilder {
    private final Logger log = getLogger(BinaryCleanupRouter.class);
    @Autowired
    private BinaryCleanupProcessor binaryCleanupProcessor;

    @Override
    public void configure() throws Exception {
        from("{{cdr.registration.successful.dest}}")
            .routeId("CleanupOldBinaryBatch")
            .log(LoggingLevel.DEBUG, log, "Cleaning up old binaries")
            .startupOrder(119)
            .process(binaryCleanupProcessor);
    }
}
