package edu.unc.lib.boxc.services.camel.triplesReindexing;

import static org.apache.camel.LoggingLevel.INFO;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

/**
 * Route for processing reindexing requests.
 *
 * @author bbpennel
 *
 */
public class TriplesReindexingRouter extends RouteBuilder {
    private static final Logger log = getLogger(TriplesReindexingRouter.class);

    private static final String LDP_CONTAINS = "<http://www.w3.org/ns/ldp#contains>";

    @BeanInject(value = "indexingMessageProcessor")
    private IndexingMessageProcessor indexingMessageProcessor;

    private String fcrepoBaseUrl;
    private String reindexingStream;
    private String triplestoreReindexStream;
    private String triplesUpdateStreamCamel;
    private long errorRetryDelay;
    private int errorMaxRedeliveries;
    private int errorBackOffMultiplier;

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
            .redeliveryDelay(errorRetryDelay)
            .maximumRedeliveries(errorMaxRedeliveries)
            .backOffMultiplier(errorBackOffMultiplier)
            .retryAttemptedLogLevel(LoggingLevel.WARN);

        // Route for receiving reindexing request messages
        from(triplesUpdateStreamCamel)
            .routeId("TripleIndexingRoute")
            .startupOrder(2)
            .bean(indexingMessageProcessor)
            .log(INFO, log, "Received triple reindexing update message: ${headers[CamelFcrepoUri]}")
            .inOnly(reindexingStream + "?disableTimeToLive=true");

        // Route which recursively steps through fedora objects and submits them for indexing
        from(reindexingStream + "?asyncConsumer=true")
            .routeId("FcrepoReindexingTraverse")
            .startupOrder(1)
            .log(INFO, log, "Reindexing ${headers[CamelFcrepoUri]}")
            .inOnly(triplestoreReindexStream)
            .to("fcrepo:" + fcrepoBaseUrl + "?preferInclude=PreferContainment" +
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
            .inOnly(reindexingStream + "?disableTimeToLive=true");
    }

    @PropertyInject("error.retryDelay:500")
    public void setErrorRetryDelay(long errorRetryDelay) {
        this.errorRetryDelay = errorRetryDelay;
    }

    @PropertyInject("error.maxRedeliveries:10")
    public void setErrorMaxRedeliveries(int errorMaxRedeliveries) {
        this.errorMaxRedeliveries = errorMaxRedeliveries;
    }

    @PropertyInject("error.backOffMultiplier:2")
    public void setErrorBackOffMultiplier(int errorBackOffMultiplier) {
        this.errorBackOffMultiplier = errorBackOffMultiplier;
    }

    public void setIndexingMessageProcessor(IndexingMessageProcessor indexingMessageProcessor) {
        this.indexingMessageProcessor = indexingMessageProcessor;
    }

    @PropertyInject("fcrepo.baseUrl")
    public void setFcrepoBaseUrl(String fcrepoBaseUrl) {
        this.fcrepoBaseUrl = fcrepoBaseUrl;
    }

    @PropertyInject("reindexing.stream")
    public void setReindexingStream(String reindexingStream) {
        this.reindexingStream = reindexingStream;
    }

    @PropertyInject("triplestore.reindex.stream")
    public void setTriplestoreReindexStream(String triplestoreReindexStream) {
        this.triplestoreReindexStream = triplestoreReindexStream;
    }

    @PropertyInject("cdr.triplesupdate.stream.camel")
    public void setTriplesUpdateStreamCamel(String triplesUpdateStreamCamel) {
        this.triplesUpdateStreamCamel = triplesUpdateStreamCamel;
    }
}
