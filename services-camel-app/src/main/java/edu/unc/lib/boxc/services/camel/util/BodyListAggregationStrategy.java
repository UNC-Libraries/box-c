package edu.unc.lib.boxc.services.camel.util;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.AggregationStrategy;
import org.slf4j.Logger;

/**
 * Message aggregation strategy which puts the bodies of messages into a list
 *
 * @author bbpennel
 */
public class BodyListAggregationStrategy implements AggregationStrategy {
    private final static Logger log = getLogger(BodyListAggregationStrategy.class);

    @SuppressWarnings("unchecked")
    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) {
            log.debug("Starting new batch");
            List<Object> list = new ArrayList<>();
            list.add(newExchange.getIn().getBody());
            newExchange.getIn().setBody(list);
            return newExchange;
        } else {
            List<Object> list = oldExchange.getIn().getBody(List.class);
            list.add(newExchange.getIn().getBody());
            log.debug("Added to batch, now contains {}", list.size());
            return oldExchange;
        }
    }
}
