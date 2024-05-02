package edu.unc.lib.boxc.services.camel.triplesReindexing;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.language.xpath.XPathBuilder;
import org.apache.camel.support.builder.Namespaces;
import org.fcrepo.camel.processor.EventProcessor;
import org.fcrepo.camel.processor.SparqlDeleteProcessor;
import org.fcrepo.camel.processor.SparqlUpdateProcessor;
import org.slf4j.Logger;

import java.util.List;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.camel.builder.PredicateBuilder.in;
import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.builder.PredicateBuilder.or;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_NAMED_GRAPH;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.fcrepo.camel.processor.ProcessorUtils.tokenizePropertyPlaceholder;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A content router for handling Fedora events.
 *
 * This is a clone of:
 * https://github.com/fcrepo-exts/fcrepo-camel-toolbox/blob/fcrepo-camel-toolbox-5.0.0/fcrepo-indexing-triplestore/src/main/java/org/fcrepo/camel/indexing/triplestore/TriplestoreRouter.java
 * It needed to be updated for camel 3 since package names changed
 */
public class TriplestoreRouter extends RouteBuilder {
    private static final Logger LOGGER = getLogger(TriplestoreRouter.class);

    private static final String RESOURCE_DELETION = "http://fedora.info/definitions/v4/event#ResourceDeletion";
    private static final String DELETE = "https://www.w3.org/ns/activitystreams#Delete";

    /**
     * Configure the message route workflow.
     */
    @Override
    public void configure() throws Exception {
        // Boxc - Namespace changed package names in camel 3
        final Namespaces ns = new Namespaces("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        ns.add("indexing", "http://fedora.info/definitions/v4/indexing#");

        final XPathBuilder indexable = new XPathBuilder(
                String.format("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='%s']",
                        "http://fedora.info/definitions/v4/indexing#Indexable"));
        indexable.namespaces(ns);

        /**
         * A generic error handler (specific to this RouteBuilder)
         */
        onException(Exception.class)
                .maximumRedeliveries("{{error.maxRedeliveries}}")
                .log("Index Routing Error: ${routeId}");

        /**
         * route a message to the proper queue, based on whether
         * it is a DELETE or UPDATE operation.
         */
        from("{{input.stream}}")
                .routeId("FcrepoTriplestoreRouter")
                .process(new EventProcessor())
                .choice()
                .when(or(header(FCREPO_EVENT_TYPE).contains(RESOURCE_DELETION),
                        header(FCREPO_EVENT_TYPE).contains(DELETE)))
                .to("direct:delete.triplestore")
                .otherwise()
                .to("direct:index.triplestore");

        /**
         * Handle re-index events
         */
        from("{{triplestore.reindex.stream}}")
                .routeId("FcrepoTriplestoreReindex")
                .to("direct:index.triplestore");

        /**
         * Based on an item's metadata, determine if it is indexable.
         */
        from("direct:index.triplestore")
                .routeId("FcrepoTriplestoreIndexer")
                .filter(not(in(tokenizePropertyPlaceholder(getContext(), "{{filter.containers}}", ",").stream()
                        .map(uri -> or(
                                header(FCREPO_URI).startsWith(constant(uri + "/")),
                                header(FCREPO_URI).isEqualTo(constant(uri))))
                        .collect(toList()))))
                .removeHeaders("CamelHttp*")
                .choice()
                .when(simple("{{indexing.predicate}} != 'true'"))
                .to("direct:update.triplestore")
                .otherwise()
                // TODO this won't work with fedora 5
                .to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=PreferMinimalContainer&accept=application/rdf+xml")
                .choice()
                .when(indexable)
                .to("direct:update.triplestore")
                .otherwise()
                .to("direct:delete.triplestore");

        /**
         * Remove an item from the triplestore index.
         */
        from("direct:delete.triplestore")
                .routeId("FcrepoTriplestoreDeleter")
                .process(new SparqlDeleteProcessor())
                .log(LoggingLevel.INFO, LOGGER,
                        "Deleting Triplestore Object ${headers[CamelFcrepoUri]}")
                .to("{{triplestore.baseUrl}}?useSystemProperties=true");

        /**
         * Perform the sparql update.
         */
        from("direct:update.triplestore")
                .routeId("FcrepoTriplestoreUpdater")
                .setHeader(FCREPO_NAMED_GRAPH)
                .simple("{{triplestore.namedGraph}}")
                .to("fcrepo:{{fcrepo.baseUrl}}?accept=application/n-triples" +
                        "&preferOmit={{prefer.omit}}&preferInclude={{prefer.include}}")
                .process(new SparqlUpdateProcessor())
                .log(LoggingLevel.INFO, LOGGER,
                        "Indexing Triplestore Object ${headers[CamelFcrepoUri]}")
                .to("{{triplestore.baseUrl}}?useSystemProperties=true");
    }
//    public static List<String> tokenizePropertyPlaceholder(final CamelContext context, final String property,
//                                                           final String token) {
//        try {
//            return stream(context.resolvePropertyPlaceholders(property).split(token)).map(String::trim)
//                    .filter(val -> !val.isEmpty()).collect(toList());
//        } catch (final Exception ex) {
//            LOGGER.debug("No property value found for {}", property);
//            return emptyList();
//        }
//    }
}
