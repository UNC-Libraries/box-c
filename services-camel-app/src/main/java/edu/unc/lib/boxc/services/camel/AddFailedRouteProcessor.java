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
