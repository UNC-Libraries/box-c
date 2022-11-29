package edu.unc.lib.boxc.services.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Processor which adds info about what route failed to an exchange
 *
 * @author bbpennel
 */
public class AddFailedRouteProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        String originalEndpoint = exchange.getFromEndpoint().getEndpointUri();
        exchange.getIn().setHeader(Exchange.FAILURE_ROUTE_ID, originalEndpoint);
    }
}
