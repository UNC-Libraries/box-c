package edu.unc.lib.boxc.services.camel.accessSurrogates;

import edu.unc.lib.boxc.services.camel.images.ImageCacheInvalidationProcessor;
import org.apache.camel.BeanInject;
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
    @BeanInject(value = "imageCacheInvalidationProcessor")
    private ImageCacheInvalidationProcessor imageCacheInvalidationProcessor;

    @Override
    public void configure() throws Exception {
        from("{{cdr.access.surrogates.stream.camel}}")
                .routeId("DcrAccessSurrogates")
                .log(DEBUG, log, "Received access surrogate request")
                .bean(processor)
                .bean(imageCacheInvalidationProcessor);
    }
}
