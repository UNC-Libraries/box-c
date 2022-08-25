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
package edu.unc.lib.boxc.services.camel.order;

import edu.unc.lib.boxc.services.camel.exportxml.ExportXMLProcessor;
import edu.unc.lib.boxc.services.camel.exportxml.ExportXMLRouter;
import org.apache.camel.BeanInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Router for processing requests to order members of containers
 *
 * @author bbpennel
 */
public class OrderMembersRouter extends RouteBuilder {
    private static final Logger log = getLogger(OrderMembersRouter.class);

    @BeanInject(value = "orderRequestProcessor")
    private OrderRequestProcessor orderRequestProcessor;

    @Override
    public void configure() throws Exception {
        from("{{cdr.ordermembers.stream.camel}}")
                .routeId("DcrOrderMembers")
                .log(DEBUG, log, "Received order members request")
                .bean(orderRequestProcessor);
    }
}
