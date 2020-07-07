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
package edu.unc.lib.dl.services.camel.longleaf;

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
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
        String binaryUri = (String) newExchange.getIn().getHeader(FCREPO_URI);
        if (binaryUri == null) {
            Object body = newExchange.getIn().getBody();
            if (body instanceof String) {
                binaryUri = (String) body;
            } else {
                log.error("Received unexpected message of type {}, ignoring", body.getClass().getName());
                return oldExchange;
            }
        }

        if (oldExchange == null) {
            List<String> list = new ArrayList<>();
            list.add(binaryUri);
            newExchange.getIn().setBody(list);
            return newExchange;
        } else {
            List<String> list = oldExchange.getIn().getBody(List.class);
            list.add(binaryUri);
            return oldExchange;
        }
    }
}
