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
package edu.unc.lib.boxc.services.camel.destroy;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

/**
 * Route to execute requests to destroy repository objects
 *
 * @author bbpennel
 *
 */
public class DestroyObjectsRouter extends RouteBuilder {
    private static final Logger log = getLogger(DestroyObjectsRouter.class);

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
            .log(DEBUG, log, "Received destroy objects message")
            .bean(destroyObjectsProcessor);

        from("{{cdr.destroy.post.stream.camel}}")
            .routeId("CdrDestroyObjectsCleanup")
            .log(DEBUG, log, "Received destroy objects cleanup message")
            .multicast()
            .parallelProcessing()
            .to("activemq://activemq:queue:filter.longleaf.deregister",
                    "{{cdr.destroy.derivatives.stream.camel}}");
    }
}
