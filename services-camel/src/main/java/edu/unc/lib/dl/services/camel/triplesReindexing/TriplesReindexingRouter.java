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
package edu.unc.lib.dl.services.camel.triplesReindexing;

import static org.apache.camel.LoggingLevel.INFO;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

/**
 * Route for processing reindexing requests.
 *
 * @author bbpennel
 *
 */
public class TriplesReindexingRouter extends RouteBuilder {

    private static final String LDP_CONTAINS = "<http://www.w3.org/ns/ldp#contains>";

    @BeanInject(value = "indexingMessageProcessor")
    private IndexingMessageProcessor indexingMessageProcessor;

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
            .redeliveryDelay("{{error.retryDelay}}")
            .maximumRedeliveries("{{error.maxRedeliveries}}")
            .backOffMultiplier("{{error.backOffMultiplier}}")
            .retryAttemptedLogLevel(LoggingLevel.WARN);

        // Route for receiving reindexing request messages
        from("{{cdr.triplesupdate.stream.camel}}")
            .routeId("TripleIndexingRoute")
            .bean(indexingMessageProcessor)
            .log(INFO, "Received triple reindexing update message: ${headers[CamelFcrepoUri]}")
            .inOnly("{{reindexing.stream}}?disableTimeToLive=true");

        // Route which recursively steps through fedora objects and submits them for indexing
        from("{{reindexing.stream}}?asyncConsumer=true")
            .routeId("FcrepoReindexingTraverse")
            .log(INFO, "Reindexing ${headers[CamelFcrepoUri]}")
            .inOnly("{{triplestore.reindex.stream}}")
            .to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=PreferContainment" +
                    "&preferOmit=ServerManaged&accept=application/n-triples")
            // split the n-triples stream on line breaks so that each triple is split into a separate message
            .split(body().tokenize("\\n")).streaming()
                .removeHeader(FCREPO_URI)
                .removeHeader("JMSCorrelationID")
                .process(exchange -> {
                    // This is a simple n-triples parser, spliting nodes on whitespace according to
                    // https://www.w3.org/TR/n-triples/#n-triples-grammar
                    // If the body is not null and the predicate is ldp:contains and the object is a URI,
                    // then set the CamelFcrepoUri header (if that header is not set, the processing stops
                    // at the filter() line below.
                    final String body = exchange.getIn().getBody(String.class);
                    if (body != null) {
                        final String parts[] = body.split("\\s+");
                        if (parts.length > 2 && parts[1].equals(LDP_CONTAINS) && parts[2].startsWith("<")) {
                            exchange.getIn().setHeader(FCREPO_URI, parts[2].substring(1, parts[2].length() - 1));
                        }
                        exchange.getIn().setBody(null);
                    }
                })
            .filter(header(FCREPO_URI).isNotNull())
            .inOnly("{{reindexing.stream}}?disableTimeToLive=true");
    }
}
