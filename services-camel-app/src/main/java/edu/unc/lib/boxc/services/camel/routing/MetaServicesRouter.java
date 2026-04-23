package edu.unc.lib.boxc.services.camel.routing;

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.slf4j.LoggerFactory.getLogger;

import edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.services.camel.util.MessageUtil;
import org.apache.camel.BeanInject;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.http.HttpStatus;
import org.apache.jena.sparql.function.library.leviathan.log;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.slf4j.Logger;

import edu.unc.lib.boxc.services.camel.BinaryMetadataProcessor;
import edu.unc.lib.boxc.services.camel.util.CacheInvalidatingProcessor;

/**
 * Meta router which sequences all service routes to run on events.
 *
 * @author bbpennel
 *
 */
public class MetaServicesRouter extends RouteBuilder {
    private static final Logger log = getLogger(MetaServicesRouter.class);

    @BeanInject("binaryMetadataProcessor")
    private BinaryMetadataProcessor mdProcessor;

    @BeanInject("cacheInvalidatingProcessor")
    private CacheInvalidatingProcessor cacheInvalidatingProcessor;

    @PropertyInject("cdr.enhancement.processingThreads")
    private Integer enhancementThreads;

    private FedoraHeadersProcessor fedoraHeadersProcessor = new FedoraHeadersProcessor();

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
            .process(fedoraHeadersProcessor)
            .filter().method(FedoraIdFilters.class, "allowedForTripleIndex")
            .doTry()
                .wireTap("direct:index.start")
            .endDoTry()
            .doCatch(FcrepoOperationFailedException.class)
                .process(exchange -> {
                    FcrepoOperationFailedException ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT,
                            FcrepoOperationFailedException.class);
                    if (ex.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                        log.warn("Ignoring exception {} for {}", ex.getStatusText(),
                                exchange.getIn().getHeader("org.fcrepo.jms.identifier"));
                        exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
                    } else {
                        throw ex;
                    }
                })
            .end()
            .bean(cacheInvalidatingProcessor)
            .filter().method(FedoraIdFilters.class, "allowedForLongleaf")
                .wireTap("direct:filter.longleaf")
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
