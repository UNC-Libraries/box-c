package edu.unc.lib.boxc.services.camel.collectionDisplay;

import org.apache.camel.BeanInject;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Router for processing requests to update default collection display properties of a collection object
 */
public class CollectionDisplayPropertiesRouter extends RouteBuilder {
    private static final Logger log = getLogger(CollectionDisplayPropertiesRouter.class);

    @BeanInject("collectionDisplayPropertiesRequestProcessor")
    private CollectionDisplayPropertiesRequestProcessor processor;

    private String stream;

    @Override
    public void configure() throws Exception {
        from(stream)
                .routeId("DcrCollectionDisplayProperties")
                .log(DEBUG, log, "Received collection display properties request")
                .bean(processor);
    }

    public void setCollectionDisplayPropertiesRequestProcessor(CollectionDisplayPropertiesRequestProcessor processor) {
        this.processor = processor;
    }

    @PropertyInject("cdr.collectiondisplayproperties.stream.camel")
    public void setCollectionDisplayPropertiesRequestStream(String stream) {
        this.stream = stream;
    }
}
