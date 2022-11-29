package edu.unc.lib.boxc.services.camel.solrUpdate;

import edu.unc.lib.boxc.indexing.solr.exception.RecoverableIndexingException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingPriority;
import edu.unc.lib.boxc.services.camel.util.CacheInvalidatingProcessor;
import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
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

    @Override
    public void configure() throws Exception {
        onException(NotFoundException.class)
            .redeliveryDelay("{{cdr.enhancement.solr.notFound.retryDelay:500}}")
            .maximumRedeliveries("{{cdr.enhancement.solr.notFound.maxRedeliveries:10}}")
            .backOffMultiplier("{{cdr.enhancement.solr.notFound.backOffMultiplier:2}}")
            .retryAttemptedLogLevel(LoggingLevel.DEBUG);
        onException(RecoverableIndexingException.class, FcrepoOperationFailedException.class,
                    ConnectException.class, HttpException.class)
            .redeliveryDelay("{{cdr.enhancement.solr.error.retryDelay:500}}")
            .maximumRedeliveries("{{cdr.enhancement.solr.error.maxRedeliveries:10}}")
            .backOffMultiplier("{{cdr.enhancement.solr.error.backOffMultiplier:2}}")
            .retryAttemptedLogLevel(LoggingLevel.WARN);
        onException(Exception.class).retriesExhaustedLogLevel(LoggingLevel.ERROR);

        from("{{cdr.solrupdate.stream.camel}}")
            .routeId("CdrServiceSolrUpdate")
            .startupOrder(510)
            .bean(solrUpdatePreprocessor)
            // Ensure that data for the object being directly indexed is up to date
            .bean(cacheInvalidatingProcessor)
            .log(LoggingLevel.DEBUG, log, "Received solr update message with action ${header[CdrSolrUpdateAction]}")
            .choice()
                .when(simple("${headers[" + CdrFcrepoHeaders.CdrSolrIndexingPriority
                        + "]} == '" + IndexingPriority.low.name() + "'"))
                    .to("{{cdr.solrupdate.priority.low.camel}}")
                .when().method(SolrUpdatePreprocessor.class, "isLargeAction")
                    .to("{{cdr.solrupdate.large.camel}}")
                .when().method(SolrUpdatePreprocessor.class, "isSmallAction")
                    .log(LoggingLevel.DEBUG, log, "Performing small solr update")
                    .bean(solrSmallUpdateProcessor)
                .otherwise()
                    .bean(solrUpdatePreprocessor, "logUnknownSolrUpdate")
            .endChoice();

        from("{{cdr.solrupdate.large.camel}}")
            .routeId("CdrSolrUpdateLarge")
            .startupOrder(507)
            .log(LoggingLevel.DEBUG, log, "Performing large solr update")
            .bean(solrLargeUpdateProcessor);

        from("{{cdr.solrupdate.priority.low.camel}}")
            .routeId("CdrSolrUpdateLowPriority")
            .startupOrder(508)
            .log(LoggingLevel.DEBUG, log, "Performing low priority solr update")
            .bean(solrSmallUpdateProcessor);

        // Endpoint for receiving individual requests update works when files are updated
        from("activemq://activemq:queue:solr.update.workObject.fileUpdated")
            .routeId("CdrSolrUpdateWorkFileEndpoint")
            .startupOrder(506)
            // Camel does not initialize the sjms endpoint for the batch consumer unless it appears in a route
            .to("{{cdr.solrupdate.workObject.fileUpdated}}");

        // Batch endpoint for updating works when files update, to allow for deduplication of pending requests
        from("{{cdr.solrupdate.workObject.fileUpdated.consumer}}")
            .routeId("CdrSolrUpdateWorkFileUpdated")
            .startupOrder(505)
            .log(LoggingLevel.DEBUG, log, "Processing batch of work updates")
            .bean(aggregateWorkForFileProcessor);
    }
}
