package edu.unc.lib.boxc.services.camel.solrUpdate;

import edu.unc.lib.boxc.indexing.solr.exception.RecoverableIndexingException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingPriority;
import edu.unc.lib.boxc.services.camel.util.CacheInvalidatingProcessor;
import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import edu.unc.lib.boxc.services.camel.util.OrderedSetAggregationStrategy;
import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.http.HttpException;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.slf4j.Logger;

import java.net.ConnectException;

import static org.slf4j.LoggerFactory.getLogger;

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

    @BeanInject(value = "cacheInvalidatingProcessor")
    private CacheInvalidatingProcessor cacheInvalidatingProcessor;

    @BeanInject
    private AggregateUpdateProcessor aggregateWorkForFileProcessor;
    @BeanInject
    private OrderedSetAggregationStrategy orderedSetAggregationStrategy;

    private long errorRetryDelay;
    private int errorMaxRedeliveries;
    private int errorBackOffMultiplier;
    private String solrUpdateStreamCamel;
    private String solrUpdateLargeCamel;
    private String solrUpdatePriorityLowCamel;
    private String solrUpdateWorkObjectFileUpdatedEndpoint;
    private String solrUpdateWorkObjectFileUpdatedDestination;
    private String solrUpdateWorkObjectFileUpdatedConsumer;

    @Override
    public void configure() throws Exception {
        onException(NotFoundException.class)
            .redeliveryDelay(errorRetryDelay)
            .maximumRedeliveries(errorMaxRedeliveries)
            .backOffMultiplier(errorBackOffMultiplier)
            .retryAttemptedLogLevel(LoggingLevel.DEBUG);
        onException(RecoverableIndexingException.class, FcrepoOperationFailedException.class,
                    ConnectException.class, HttpException.class)
            .redeliveryDelay(errorRetryDelay)
            .maximumRedeliveries(errorMaxRedeliveries)
            .backOffMultiplier(errorBackOffMultiplier)
            .retryAttemptedLogLevel(LoggingLevel.WARN);
        onException(Exception.class).retriesExhaustedLogLevel(LoggingLevel.ERROR);

        from(solrUpdateStreamCamel)
            .routeId("CdrServiceSolrUpdate")
            .startupOrder(510)
            .bean(solrUpdatePreprocessor)
            // Ensure that data for the object being directly indexed is up to date
            .bean(cacheInvalidatingProcessor)
            .log(LoggingLevel.DEBUG, log, "Received solr update message with action ${header[CdrSolrUpdateAction]}")
            .choice()
                .when(simple("${headers[" + CdrFcrepoHeaders.CdrSolrIndexingPriority
                        + "]} == '" + IndexingPriority.low.name() + "'"))
                    .to(solrUpdatePriorityLowCamel)
                .when().method(SolrUpdatePreprocessor.class, "isLargeAction")
                    .to(solrUpdateLargeCamel)
                .when().method(SolrUpdatePreprocessor.class, "isSmallAction")
                    .log(LoggingLevel.DEBUG, log, "Performing small solr update")
                    .bean(solrSmallUpdateProcessor)
                .otherwise()
                    .bean(solrUpdatePreprocessor, "logUnknownSolrUpdate")
            .endChoice();

        from(solrUpdateLargeCamel)
            .routeId("CdrSolrUpdateLarge")
            .startupOrder(507)
            .log(LoggingLevel.DEBUG, log, "Performing large solr update")
            .bean(solrLargeUpdateProcessor);

        from(solrUpdatePriorityLowCamel)
            .routeId("CdrSolrUpdateLowPriority")
            .startupOrder(508)
            .log(LoggingLevel.DEBUG, log, "Performing low priority solr update")
            .bean(solrSmallUpdateProcessor);

        // Endpoint for receiving individual requests update works when files are updated
        from(solrUpdateWorkObjectFileUpdatedEndpoint)
            .routeId("CdrSolrUpdateWorkFileEndpoint")
            .startupOrder(506)
            // Camel does not initialize the sjms endpoint for the batch consumer unless it appears in a route
            .to(solrUpdateWorkObjectFileUpdatedDestination);

        // Batch endpoint for updating works when files update, to allow for deduplication of pending requests
        from(solrUpdateWorkObjectFileUpdatedConsumer)
            .routeId("CdrSolrUpdateWorkFileUpdated")
            .startupOrder(505)
            .log(LoggingLevel.DEBUG, log, "Processing batch of work updates")
                .aggregate(orderedSetAggregationStrategy).constant(true)
                .completionSize(100)
                .completionTimeout(1000)
            .bean(aggregateWorkForFileProcessor);
    }

    @PropertyInject("cdr.enhancement.solr.notFound.retryDelay:500")
    public void setErrorRetryDelay(long errorRetryDelay) {
        this.errorRetryDelay = errorRetryDelay;
    }

    @PropertyInject("cdr.enhancement.solr.notFound.maxRedeliveries:10")
    public void setErrorMaxRedeliveries(int errorMaxRedeliveries) {
        this.errorMaxRedeliveries = errorMaxRedeliveries;
    }

    @PropertyInject("cdr.enhancement.solr.notFound.backOffMultiplier:2")
    public void setErrorBackOffMultiplier(int errorBackOffMultiplier) {
        this.errorBackOffMultiplier = errorBackOffMultiplier;
    }

    public void setSolrLargeUpdateProcessor(SolrUpdateProcessor solrLargeUpdateProcessor) {
        this.solrLargeUpdateProcessor = solrLargeUpdateProcessor;
    }

    public void setSolrSmallUpdateProcessor(SolrUpdateProcessor solrSmallUpdateProcessor) {
        this.solrSmallUpdateProcessor = solrSmallUpdateProcessor;
    }

    public void setSolrUpdatePreprocessor(SolrUpdatePreprocessor solrUpdatePreprocessor) {
        this.solrUpdatePreprocessor = solrUpdatePreprocessor;
    }

    public void setCacheInvalidatingProcessor(CacheInvalidatingProcessor cacheInvalidatingProcessor) {
        this.cacheInvalidatingProcessor = cacheInvalidatingProcessor;
    }

    public void setAggregateWorkForFileProcessor(AggregateUpdateProcessor aggregateWorkForFileProcessor) {
        this.aggregateWorkForFileProcessor = aggregateWorkForFileProcessor;
    }

    @PropertyInject("cdr.solrupdate.stream.camel")
    public void setSolrUpdateStreamCamel(String solrUpdateStreamCamel) {
        this.solrUpdateStreamCamel = solrUpdateStreamCamel;
    }

    @PropertyInject("cdr.solrupdate.large.camel")
    public void setSolrUpdateLargeCamel(String solrUpdateLargeCamel) {
        this.solrUpdateLargeCamel = solrUpdateLargeCamel;
    }

    @PropertyInject("cdr.solrupdate.priority.low.camel")
    public void setSolrUpdatePriorityLowCamel(String solrUpdatePriorityLowCamel) {
        this.solrUpdatePriorityLowCamel = solrUpdatePriorityLowCamel;
    }

    @PropertyInject("cdr.solrupdate.workObject.fileUpdated.endpoint")
    public void setSolrUpdateWorkObjectFileUpdatedEndpoint(String solrUpdateWorkObjectFileUpdatedEndpoint) {
        this.solrUpdateWorkObjectFileUpdatedEndpoint = solrUpdateWorkObjectFileUpdatedEndpoint;
    }

    @PropertyInject("cdr.solrupdate.workObject.fileUpdated.consumer")
    public void setSolrUpdateWorkObjectFileUpdatedConsumer(String solrUpdateWorkObjectFileUpdatedConsumer) {
        this.solrUpdateWorkObjectFileUpdatedConsumer = solrUpdateWorkObjectFileUpdatedConsumer;
    }

    @PropertyInject("cdr.solrupdate.workObject.fileUpdated.destination")
    public void setSolrUpdateWorkObjectFileUpdatedDestination(String solrUpdateWorkObjectFileUpdatedDestination) {
        this.solrUpdateWorkObjectFileUpdatedDestination = solrUpdateWorkObjectFileUpdatedDestination;
    }
}
