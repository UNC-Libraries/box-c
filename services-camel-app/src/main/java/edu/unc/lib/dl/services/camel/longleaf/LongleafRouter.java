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

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import edu.unc.lib.dl.services.camel.AddFailedRouteProcessor;

/**
 * Router for longleaf operations
 *
 * @author bbpennel
 */
public class LongleafRouter extends RouteBuilder {
    private static final Logger log = getLogger(LongleafRouter.class);

    @BeanInject(value = "getUrisProcessor")
    private GetUrisProcessor getUrisProcessor;

    @BeanInject(value = "registerLongleafProcessor")
    private RegisterToLongleafProcessor registerProcessor;

    @BeanInject(value = "deregisterLongleafProcessor")
    private DeregisterLongleafProcessor deregisterProcessor;

    @Value("${longleaf.maxRedelivieries:3}")
    private int longleafMaxRedelivieries;

    @Value("${longleaf.redeliveryDelay:10000}")
    private long longleafRedeliveryDelay;

    @Override
    public void configure() throws Exception {
        AddFailedRouteProcessor failedRouteProcessor = new AddFailedRouteProcessor();

        errorHandler(deadLetterChannel("{{longleaf.dlq.dest}}")
                .maximumRedeliveries(longleafMaxRedelivieries)
                .redeliveryDelay(longleafRedeliveryDelay)
                .onPrepareFailure(failedRouteProcessor));

        from("direct-vm:filter.longleaf")
            .routeId("RegisterLongleafQueuing")
            .startupOrder(4)
            .filter().method(RegisterToLongleafProcessor.class, "registerableBinary")
            .log(LoggingLevel.DEBUG, log, "Queuing ${headers[CamelFcrepoUri]} for registration to longleaf")
            .to("sjms:register.longleaf?transacted=true");

        from("{{longleaf.register.consumer}}")
            .routeId("RegisterLongleafProcessing")
            .startupOrder(3)
            .log(LoggingLevel.DEBUG, log, "Processing batch of longleaf registrations")
            .bean(registerProcessor);

        from("activemq://activemq:queue:filter.longleaf.deregister")
            .routeId("DeregisterLongleafQueuing")
            .startupOrder(2)
            .log(LoggingLevel.DEBUG, log, "Queuing ${body} for deregistration in longleaf")
            .process(getUrisProcessor)
            .to("sjms:deregister.longleaf?transacted=true");

        from("{{longleaf.deregister.consumer}}")
            .routeId("DeregisterLongleafProcessing")
            .startupOrder(1)
            .log(LoggingLevel.DEBUG, log, "Processing batch of longleaf deregistrations")
            .bean(deregisterProcessor);
    }
}
