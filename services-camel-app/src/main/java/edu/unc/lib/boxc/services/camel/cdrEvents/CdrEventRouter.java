package edu.unc.lib.boxc.services.camel.cdrEvents;

import static edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions.ADD;
import static edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions.EDIT_ACCESS_CONTROL;
import static edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions.EDIT_TYPE;
import static edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions.MARK_FOR_DELETION;
import static edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions.MOVE;
import static edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions.REMOVE;
import static edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions.RESTORE_FROM_DELETION;
import static edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions.SET_AS_PRIMARY_OBJECT;
import static edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions.UPDATE_DESCRIPTION;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrUpdateAction;
import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import edu.unc.lib.boxc.services.camel.solr.CdrEventToSolrUpdateProcessor;

/**
 * Router which listens to and processes CDR Event messages
 *
 * @author lfarrell
 *
 */
public class CdrEventRouter extends RouteBuilder {
    private static final Logger log = getLogger(CdrEventRouter.class);

    @BeanInject(value = "cdrEventProcessor")
    private CdrEventProcessor cdrEventProcessor;

    @BeanInject(value = "cdrEventToSolrUpdateProcessor")
    private CdrEventToSolrUpdateProcessor cdrEventToSolrUpdateProcessor;
    private long errorRetryDelay;
    private int errorMaxRedeliveries;
    private int errorBackOffMultiplier;
    private String cdrStreamCamel;

    private static final List<String> solrAllowedActions = asList(ADD.toString(), EDIT_ACCESS_CONTROL.toString(),
            EDIT_TYPE.toString(), MARK_FOR_DELETION.toString(), MOVE.toString(), REMOVE.toString(),
            RESTORE_FROM_DELETION.toString(), SET_AS_PRIMARY_OBJECT.toString(), UPDATE_DESCRIPTION.toString());

    private static final String solrAllowed = String.join(",", solrAllowedActions);

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
                .redeliveryDelay(errorRetryDelay)
                .maximumRedeliveries(errorMaxRedeliveries)
                .backOffMultiplier(errorBackOffMultiplier)
                .retryAttemptedLogLevel(LoggingLevel.WARN);

        from(cdrStreamCamel)
            .routeId("CdrServiceCdrEvents")
            .startupOrder(2)
            .log(LoggingLevel.DEBUG, log, "CDR Event Message received ${headers[" + CdrUpdateAction + "]}")
            .bean(cdrEventProcessor)
            .filter(simple("${headers[" + CdrUpdateAction + "]} in '" + solrAllowed + "'"))
            .to("direct:solr-update");

        from("direct:solr-update")
            .routeId("CdrServiceCdrEventToSolrUpdateProcessor")
            .startupOrder(1)
            .log(LoggingLevel.DEBUG, log, "Updating solr index for ${headers[org.fcrepo.jms.identifier]}")
            .bean(cdrEventToSolrUpdateProcessor);
    }

    @PropertyInject("error.retryDelay:10000")
    public void setErrorRetryDelay(long errorRetryDelay) {
        this.errorRetryDelay = errorRetryDelay;
    }

    @PropertyInject("error.maxRedeliveries:3")
    public void setErrorMaxRedeliveries(int errorMaxRedeliveries) {
        this.errorMaxRedeliveries = errorMaxRedeliveries;
    }

    @PropertyInject("error.backOffMultiplier:2")
    public void setErrorBackOffMultiplier(int errorBackOffMultiplier) {
        this.errorBackOffMultiplier = errorBackOffMultiplier;
    }

    public void setCdrEventProcessor(CdrEventProcessor cdrEventProcessor) {
        this.cdrEventProcessor = cdrEventProcessor;
    }

    public void setCdrEventToSolrUpdateProcessor(CdrEventToSolrUpdateProcessor cdrEventToSolrUpdateProcessor) {
        this.cdrEventToSolrUpdateProcessor = cdrEventToSolrUpdateProcessor;
    }

    @PropertyInject("cdr.stream.camel")
    public void setCdrStreamCamel(String cdrStreamCamel) {
        this.cdrStreamCamel = cdrStreamCamel;
    }
}
