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
package edu.unc.lib.dl.services.camel.routing;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.http.HttpStatus;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.slf4j.Logger;

import edu.unc.lib.dl.services.camel.BinaryMetadataProcessor;

/**
 * Meta router which sequences all service routes to run on events.
 *
 * @author bbpennel
 *
 */
public class MetaServicesRouter extends RouteBuilder {
    private static final Logger log = getLogger(MetaServicesRouter.class);

    @BeanInject(value = "binaryMetadataProcessor")
    private BinaryMetadataProcessor mdProcessor;

    @PropertyInject(value = "cdr.enhancement.processingThreads")
    private Integer enhancementThreads;

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
                .redeliveryDelay("{{error.retryDelay}}")
                .maximumRedeliveries("{{error.maxRedeliveries}}")
                .backOffMultiplier("{{error.backOffMultiplier}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("{{fcrepo.stream}}")
            .routeId("CdrMetaServicesRouter")
            .startupOrder(9)
            .filter().method(FedoraIdFilters.class, "allowedForTripleIndex")
            .doTry()
                .to("direct-vm:index.start")
            .endDoTry()
            .doCatch(FcrepoOperationFailedException.class)
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        FcrepoOperationFailedException ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT,
                                FcrepoOperationFailedException.class);
                        if (ex.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                            log.warn("Ignoring exception {} for {}", ex.getStatusText(),
                                    exchange.getIn().getHeader("org.fcrepo.jms.identifier"));
                            exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
                        } else {
                            throw ex;
                        }
                    }
                })
            .end()
            .filter().method(FedoraIdFilters.class, "allowedForLongleaf")
                .wireTap("direct-vm:filter.longleaf")
            .end().end() // ending the filter and the wiretap
            .filter().method(FedoraIdFilters.class, "allowedForEnhancements")
            .wireTap("direct:process.enhancement");

        from("direct:process.enhancement")
            .routeId("ProcessEnhancement")
            .startupOrder(5)
            .delay(simple("{{cdr.enhancement.postIndexingDelay}}"))
            .removeHeaders("CamelHttp*")
            .to("{{cdr.enhancement.stream.camel}}");
    }
}
