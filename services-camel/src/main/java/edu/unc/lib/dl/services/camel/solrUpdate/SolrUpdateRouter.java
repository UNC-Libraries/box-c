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
package edu.unc.lib.dl.services.camel.solrUpdate;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import edu.unc.lib.dl.fedora.NotFoundException;

/**
 * Route for performing solr updates for update requests.
 *
 * @author lfarrell
 *
 */
public class SolrUpdateRouter extends RouteBuilder {
    private final static Logger log = getLogger(SolrUpdateRouter.class);

    @BeanInject(value = "solrLargeUpdateProcessor")
    private SolrUpdateProcessor solrLargeUpdateProcessor;

    @BeanInject(value = "solrSmallUpdateProcessor")
    private SolrUpdateProcessor solrSmallUpdateProcessor;

    @BeanInject(value = "solrUpdatePreprocessor")
    private SolrUpdatePreprocessor solrUpdatePreprocessor;

    @Override
    public void configure() throws Exception {
        onException(NotFoundException.class)
            .redeliveryDelay("{{cdr.enhancement.solr.error.retryDelay:500}}")
            .maximumRedeliveries("{{cdr.enhancement.solr.error.maxRedeliveries:10}}")
            .backOffMultiplier("{{cdr.enhancement.solr.error.backOffMultiplier:2}}")
            .retryAttemptedLogLevel(LoggingLevel.DEBUG);

        onException(Exception.class)
            .redeliveryDelay("{{cdr.enhancement.solr.error.retryDelay:500}}")
            .maximumRedeliveries("{{cdr.enhancement.solr.error.maxRedeliveries:10}}")
            .backOffMultiplier("{{cdr.enhancement.solr.error.backOffMultiplier:2}}")
            .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("{{cdr.solrupdate.stream.camel}}")
            .routeId("CdrServiceSolrUpdate")
            .startupOrder(510)
            .bean(solrUpdatePreprocessor)
            .log(LoggingLevel.DEBUG, "Received solr update message with action ${header[CdrSolrUpdateAction]}")
            .choice()
                .when().method(SolrUpdatePreprocessor.class, "isLargeAction")
                    .to("{{cdr.solrupdate.large.consumer}}")
                .when().method(SolrUpdatePreprocessor.class, "isSmallAction")
                    .to("{{cdr.solrupdate.small.dest}}")
                .otherwise()
                    .bean(solrUpdatePreprocessor, "logUnknownSolrUpdate")
            .endChoice();

        from("{{cdr.solrupdate.small.consumer}}")
            .routeId("CdrSolrUpdateSmall")
            .startupOrder(508)
            .log(LoggingLevel.DEBUG, log, "Performing batch of small solr updates")
            .split(body())
            .bean(solrSmallUpdateProcessor);

        from("{{cdr.solrupdate.large.consumer}}")
            .routeId("CdrSolrUpdateLarge")
            .startupOrder(507)
            .log(LoggingLevel.DEBUG, log, "Performing large solr update")
            .bean(solrLargeUpdateProcessor);
    }
}
