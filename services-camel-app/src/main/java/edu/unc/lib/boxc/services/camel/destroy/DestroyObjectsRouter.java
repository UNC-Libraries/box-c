package edu.unc.lib.boxc.services.camel.destroy;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Route to execute requests to destroy repository objects
 *
 * @author bbpennel
 *
 */
public class DestroyObjectsRouter extends RouteBuilder {
    private static final Logger log = getLogger(DestroyObjectsRouter.class);

    @BeanInject(value = "destroyObjectsProcessor")
    private DestroyObjectsProcessor destroyObjectsProcessor;

    private String cdrDestroyStreamCamel;
    private String cdrDestroyPostStreamCamel;
    private String cdrDestroyDerivativesStreamCamel;
    private String longleafDeregisterEndpoint;

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

        from(cdrDestroyStreamCamel)
            .routeId("CdrDestroyObjects")
            .log(DEBUG, log, "Received destroy objects message")
            .bean(destroyObjectsProcessor);

        from(cdrDestroyPostStreamCamel)
            .routeId("CdrDestroyObjectsCleanup")
            .log(DEBUG, log, "Received destroy objects cleanup message")
            .multicast().parallelProcessing()
                .to(longleafDeregisterEndpoint)
                .to(cdrDestroyDerivativesStreamCamel);
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

    public void setDestroyObjectsProcessor(DestroyObjectsProcessor destroyObjectsProcessor) {
        this.destroyObjectsProcessor = destroyObjectsProcessor;
    }

    @PropertyInject("cdr.destroy.stream.camel")
    public void setCdrDestroyStreamCamel(String cdrDestroyStreamCamel) {
        this.cdrDestroyStreamCamel = cdrDestroyStreamCamel;
    }

    @PropertyInject("cdr.destroy.post.stream.camel")
    public void setCdrDestroyPostStreamCamel(String cdrDestroyPostStreamCamel) {
        this.cdrDestroyPostStreamCamel = cdrDestroyPostStreamCamel;
    }

    @PropertyInject("cdr.destroy.derivatives.stream.camel")
    public void setCdrDestroyDerivativesStreamCamel(String cdrDestroyDerivativesStreamCamel) {
        this.cdrDestroyDerivativesStreamCamel = cdrDestroyDerivativesStreamCamel;
    }

    @PropertyInject("longleaf.filter.deregister")
    public void setLongleafDeregisterEndpoint(String longleafDeregisterEndpoint) {
        this.longleafDeregisterEndpoint = longleafDeregisterEndpoint;
    }
}
