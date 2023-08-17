package edu.unc.lib.boxc.services.camel.thumbnails;

import org.apache.camel.BeanInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Router for processing assigning thumbnails for works
 *
 * @author snluong
 */
public class ThumbnailRouter extends RouteBuilder {
    private static final Logger log = getLogger(ThumbnailRouter.class);
    @BeanInject(value = "thumbnailRequestProcessor")
    private ThumbnailRequestProcessor thumbnailRequestProcessor;

    @Override
    public void configure() throws Exception {
        from("{{cdr.thumbnails.stream.camel}}")
                .routeId("DcrThumbnails")
                .log(DEBUG, log, "Received thumbnail request")
                .bean(thumbnailRequestProcessor);
    }
}
