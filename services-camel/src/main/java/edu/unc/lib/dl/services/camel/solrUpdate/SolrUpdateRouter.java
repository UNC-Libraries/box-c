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

import static org.apache.camel.LoggingLevel.DEBUG;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

/**
 * Route for performing solr updates for update requests.
 *
 * @author lfarrell
 *
 */
public class SolrUpdateRouter extends RouteBuilder {
    @BeanInject(value = "solrUpdateProcessor")
    private SolrUpdateProcessor solrUpdateProcessor;

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
        .redeliveryDelay("{{error.retryDelay}}")
        .maximumRedeliveries("{{error.maxRedeliveries}}")
        .backOffMultiplier(2)
        .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("{{cdr.solrupdate.stream.camel}}")
            .routeId("CdrServiceSolrUpdate")
            .log(DEBUG, "Received solr update message")
            .bean(solrUpdateProcessor);

    }

}
