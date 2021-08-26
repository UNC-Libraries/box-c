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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
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
