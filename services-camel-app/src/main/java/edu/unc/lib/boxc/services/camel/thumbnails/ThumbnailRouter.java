package edu.unc.lib.boxc.services.camel.thumbnails;

import edu.unc.lib.boxc.model.api.rdf.Cdr;
import org.apache.camel.BeanInject;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Router for processing thumbnails:
 * Importing thumbnails for collections, folders, and admin units
 * Assigning thumbnails for works
 *
 * @author snluong
 */
public class ThumbnailRouter extends RouteBuilder {
    private static final Logger log = getLogger(ThumbnailRouter.class);
    @BeanInject(value = "thumbnailRequestProcessor")
    private ThumbnailRequestProcessor thumbnailRequestProcessor;
    @BeanInject(value = "importThumbnailRequestProcessor")
    private ImportThumbnailRequestProcessor importThumbnailRequestProcessor;

    @Override
    public void configure() throws Exception {
        from("{{cdr.thumbnails.stream.camel}}")
            .routeId("DcrThumbnails")
            .log(DEBUG, log,
                    "Received thumbnail request: assigning thumbnail for ${headers[CamelFcrepoUri]}")
            .bean(thumbnailRequestProcessor);

        from("{{cdr.import.thumbnails.stream.camel}}")
                .routeId("DcrImportThumbnails")
                .process(importThumbnailRequestProcessor)
                .log(DEBUG, log,
                        "Received thumbnail request: importing thumbnail for ${headers[CamelFcrepoUri]}")
                // trigger JP2 generation sequentially followed by indexing
                .to("direct:process.enhancement.imageAccessCopy", "direct:solrIndexing");
    }
}
