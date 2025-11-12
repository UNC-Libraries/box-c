package edu.unc.lib.boxc.services.camel.collectionDisplay;

import org.apache.camel.BeanInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

public class CollectionDisplayPropertiesRouter extends RouteBuilder {
    private static final Logger log = getLogger(CollectionDisplayPropertiesRouter.class);
    @BeanInject("collectionDisplayPropertiesRequestProcessor")
    private CollectionDisplayPropertiesRequestProcessor processor;

    @Override
    public void configure() throws Exception {
        from("{{cdr.collectiondisplayproperties.stream.camel}}")
                .routeId("DcrCollectionDisplayProperties")
                .log(DEBUG, log, "Received collection display properties request")
                .bean(processor);
    }
}
