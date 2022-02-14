/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.boxc.services.camel.util;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Message aggregation strategy which puts the bodies of messages into an ordered set.
 * So contents will be in insertion order, and deduplicated.
 *
 * @author bbpennel
 */
public class OrderedSetAggregationStrategy implements AggregationStrategy {
    private final static Logger log = getLogger(OrderedSetAggregationStrategy.class);

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) {
            log.debug("Starting new ordered set batch");
            var orderedSet = new LinkedHashSet<>();
            orderedSet.add(newExchange.getIn().getBody());
            newExchange.getIn().setBody(orderedSet);
            return newExchange;
        } else {
            var orderedSet = oldExchange.getIn().getBody(Set.class);
            orderedSet.add(newExchange.getIn().getBody());
            log.debug("Added to batch, now contains {}", orderedSet.size());
            return oldExchange;
        }
    }
}
