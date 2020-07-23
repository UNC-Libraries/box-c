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

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

/**
 * Router for longleaf operations
 *
 * @author bbpennel
 */
public class LongleafRouter extends RouteBuilder {

    @BeanInject(value = "registerLongleafProcessor")
    private RegisterToLongleafProcessor registerProcessor;

    @BeanInject(value = "deregisterLongleafProcessor")
    private DeregisterLongleafProcessor deregisterProcessor;

    @Override
    public void configure() throws Exception {
        from("direct-vm:filter.longleaf")
            .routeId("RegisterLongleafQueuing")
            .startupOrder(4)
            .filter().method(RegisterToLongleafProcessor.class, "registerableBinary")
            .log(LoggingLevel.DEBUG, "Queuing ${headers[CamelFcrepoUri]} for registration to longleaf")
            .to("sjms:register.longleaf?transacted=true");

        from("sjms-batch:register.longleaf?completionTimeout={{longleaf.register.completionTimeout}}"
                + "&completionSize={{longleaf.register.completionSize}}"
                + "&consumerCount={{longleaf.register.consumers}}"
                + "&aggregationStrategy=#longleafAggregationStrategy"
                + "&connectionFactory=jmsFactory")
            .routeId("RegisterLongleafProcessing")
            .startupOrder(3)
            .log(LoggingLevel.DEBUG, "Processing batch of longleaf registrations")
            .bean(registerProcessor);

        from("activemq://activemq:queue:filter.longleaf.deregister")
            .routeId("DeregisterLongleafQueuing")
            .startupOrder(2)
            .log(LoggingLevel.DEBUG, "Queuing ${body} for deregistration in longleaf")
            .to("sjms:deregister.longleaf?transacted=true");

        from("sjms-batch:deregister.longleaf?completionTimeout={{longleaf.register.completionTimeout}}"
                + "&completionSize={{longleaf.register.completionSize}}"
                + "&consumerCount={{longleaf.register.consumers}}"
                + "&aggregationStrategy=#longleafAggregationStrategy"
                + "&connectionFactory=jmsFactory")
            .routeId("DeregisterLongleafProcessing")
            .startupOrder(1)
            .log(LoggingLevel.DEBUG, "Processing batch of longleaf deregistrations")
            .bean(deregisterProcessor);
    }

}
