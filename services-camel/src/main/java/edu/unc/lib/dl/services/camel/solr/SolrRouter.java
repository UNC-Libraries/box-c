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

package edu.unc.lib.dl.services.camel.solr;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import edu.unc.lib.dl.data.ingest.solr.exception.ObjectTombstonedException;
import edu.unc.lib.dl.fedora.NotFoundException;

/**
 * Router which triggers the full indexing of individual objects to Solr.
 *
 * @author lfarrell
 *
 */
public class SolrRouter extends RouteBuilder {
    @BeanInject(value = "solrIngestProcessor")
    private SolrIngestProcessor solrIngestProcessor;

    @Override
    public void configure() throws Exception {
        from("direct:solrIndexing")
            .routeId("CdrServiceSolr")
            .startupOrder(40)
            .onException(NotFoundException.class)
                .redeliveryDelay("{{cdr.enhancement.solr.error.retryDelay:500}}")
                .maximumRedeliveries("{{cdr.enhancement.solr.error.maxRedeliveries:10}}")
                .backOffMultiplier("{{cdr.enhancement.solr.error.backOffMultiplier:2}}")
                .retryAttemptedLogLevel(LoggingLevel.DEBUG)
            .end()
            .onException(ObjectTombstonedException.class)
                .retriesExhaustedLogLevel(LoggingLevel.DEBUG)
            .end()
            .log(LoggingLevel.DEBUG, "Calling solr indexing route for ${headers[org.fcrepo.jms.identifier]}")
            .bean(solrIngestProcessor);
    }
}
