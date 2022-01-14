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

package edu.unc.lib.boxc.services.camel.solr;

import edu.unc.lib.boxc.indexing.solr.exception.RecoverableIndexingException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.http.HttpException;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.slf4j.Logger;

import java.net.ConnectException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Router which triggers the full indexing of individual objects to Solr.
 *
 * @author lfarrell
 *
 */
public class SolrRouter extends RouteBuilder {
    private static final Logger log = getLogger(SolrRouter.class);

    @BeanInject(value = "solrIngestProcessor")
    private SolrIngestProcessor solrIngestProcessor;

    @Override
    public void configure() throws Exception {
        from("direct:solrIndexing")
            .routeId("CdrServiceSolr")
            .startupOrder(40)
            .onException(NotFoundException.class)
                .redeliveryDelay("{{cdr.enhancement.solr.notFound.retryDelay:500}}")
                .maximumRedeliveries("{{cdr.enhancement.solr.notFound.maxRedeliveries:10}}")
                .backOffMultiplier("{{cdr.enhancement.solr.notFound.backOffMultiplier:2}}")
                .retryAttemptedLogLevel(LoggingLevel.DEBUG)
            .end()
            .onException(RecoverableIndexingException.class, FcrepoOperationFailedException.class,
                    ConnectException.class, HttpException.class)
                .redeliveryDelay("{{cdr.enhancement.solr.error.retryDelay:500}}")
                .maximumRedeliveries("{{cdr.enhancement.solr.error.maxRedeliveries:10}}")
                .backOffMultiplier("{{cdr.enhancement.solr.error.backOffMultiplier:2}}")
                .retryAttemptedLogLevel(LoggingLevel.WARN)
            .end()
            .onException(Exception.class)
                .retriesExhaustedLogLevel(LoggingLevel.ERROR)
            .end()
            .log(LoggingLevel.DEBUG, log, "Calling solr indexing route for ${headers[org.fcrepo.jms.identifier]}")
            .bean(solrIngestProcessor);
    }
}
