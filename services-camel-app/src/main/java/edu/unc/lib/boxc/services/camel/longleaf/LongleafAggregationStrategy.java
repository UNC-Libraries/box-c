package edu.unc.lib.boxc.services.camel.longleaf;

import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.unc.lib.boxc.services.camel.util.MessageUtil;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aggregation strategy for combining messages into a batch for longleaf processing
 *
 * @author bbpennel
 */
public class LongleafAggregationStrategy implements AggregationStrategy {
    private static final Logger log = LoggerFactory.getLogger(LongleafAggregationStrategy.class);

    @SuppressWarnings("unchecked")
    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        String binaryUri = MessageUtil.getFcrepoUri(newExchange.getIn());
        Object body = newExchange.getIn().getBody();
        List<String> incomingList;
        if (body instanceof List) {
            incomingList = (List<String>) body;
        } else if (binaryUri != null) {
            incomingList = Collections.singletonList(binaryUri);
        } else if (body instanceof String) {
            incomingList = singletonList((String) body);
        } else if (body == null) {
            throw new IllegalArgumentException("Message body is null and no fcrepo URI header is present");
        } else {
            log.error("Received unexpected message of type {}, ignoring", body.getClass().getName());
            return oldExchange;
        }

        if (oldExchange == null) {
            List<String> list = new ArrayList<>(incomingList);
            newExchange.getIn().setBody(list);
            return newExchange;
        } else {
            List<String> list = oldExchange.getIn().getBody(List.class);
            list.addAll(incomingList);
            return oldExchange;
        }
    }
}
