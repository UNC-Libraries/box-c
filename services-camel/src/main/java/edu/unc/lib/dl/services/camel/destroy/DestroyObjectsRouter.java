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
package edu.unc.lib.dl.services.camel.destroy;

import static org.apache.camel.LoggingLevel.DEBUG;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

/**
 * Route to execute requests to destroy repository objects
 *
 * @author bbpennel
 *
 */
public class DestroyObjectsRouter extends RouteBuilder {

    @BeanInject(value = "destroyObjectsProcessor")
    private DestroyObjectsProcessor destroyObjectsProcessor;

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
                .redeliveryDelay("{{error.retryDelay}}")
                .maximumRedeliveries("{{error.maxRedeliveries}}")
                .backOffMultiplier("{{error.backOffMultiplier}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("{{cdr.destroy.stream.camel}}")
            .routeId("CdrDestroyObjects")
            .log(DEBUG, "Received destroy objects message")
            .bean(destroyObjectsProcessor);

        from("{{cdr.destroy.cleanup.stream.camel}}")
            .routeId("CdrDestroyObjectsCleanup")
            .log(DEBUG, "Received destroy objects cleanup message")
            .multicast()
            .parallelProcessing()
            .to("activemq://activemq:queue:filter.longleaf.deregister")
            .to("{{cdr.destroy.derivatives.stream.camel}}");
    }
}
