package edu.unc.lib.boxc.services.camel.longleaf;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import edu.unc.lib.boxc.services.camel.AddFailedRouteProcessor;
import org.springframework.beans.factory.annotation.Value;

/**
 * Router for longleaf operations
 *
 * @author bbpennel
 */
public class LongleafRouter extends RouteBuilder {
    private static final Logger log = getLogger(LongleafRouter.class);

    @BeanInject(value = "getUrisProcessor")
    private GetUrisProcessor getUrisProcessor;

    @BeanInject(value = "registerLongleafProcessor")
    private RegisterToLongleafProcessor registerProcessor;

    @BeanInject(value = "deregisterLongleafProcessor")
    private DeregisterLongleafProcessor deregisterProcessor;

    @BeanInject(value = "longleafAggregationStrategy")
    private LongleafAggregationStrategy longleafAggregationStrategy;
    @Value("${longleaf.maxRedelivieries:3}")
    private int longleafMaxRedelivieries;

    @Value("${longleaf.redeliveryDelay:10000}")
    private long longleafRedeliveryDelay;
    private int batchSize;
    private long batchTimeout;

    private String longleafDeadLetterQueueDestination;
    private String longleafRegisterConsumer;
    private String longleafRegisterBatchConsumer;
    private String longleafRegisterDestination;
    private String longleafDeregisterConsumer;
    private String longleafDeregisterDestination;
    private String longleafDeregisterBatchDestination;
    private String longleafFilterDeregister;

    @Override
    public void configure() throws Exception {
        AddFailedRouteProcessor failedRouteProcessor = new AddFailedRouteProcessor();

        errorHandler(deadLetterChannel("{{longleaf.dlq.dest}}")
                .maximumRedeliveries(longleafMaxRedelivieries)
                .redeliveryDelay(longleafRedeliveryDelay)
                .onPrepareFailure(failedRouteProcessor));

        from("direct-vm:filter.longleaf")
            .routeId("RegisterLongleafQueuing")
            .startupOrder(8)
            .filter().method(RegisterToLongleafProcessor.class, "registerableBinary")
            .log(LoggingLevel.DEBUG, log, "Queuing ${headers[CamelFcrepoUri]} for registration to longleaf")
            .to(longleafRegisterDestination);

        // Directing multiple input streams to the registration stream, since camel 3 disallows multiple addresses in "from"
        from(longleafRegisterConsumer)
            .to(longleafRegisterDestination);
        from(longleafRegisterBatchConsumer)
            .to(longleafRegisterDestination);
        from(longleafRegisterDestination)
            .routeId("RegisterLongleafProcessing")
            .startupOrder(5)
            .log(LoggingLevel.INFO, log, "Processing batch of longleaf registrations")
                .aggregate(longleafAggregationStrategy).constant(true)
                .completionSize(batchSize)
                .completionTimeout(batchTimeout)
            .bean(registerProcessor);

        from(longleafFilterDeregister)
            .routeId("DeregisterLongleafQueuing")
            .startupOrder(4)
            .log(LoggingLevel.DEBUG, log, "Queuing ${body} for deregistration in longleaf")
            .process(getUrisProcessor)
            .to(longleafDeregisterDestination);

        // Directing multiple input streams to the deregistration stream
        from(longleafDeregisterConsumer)
            .to(longleafDeregisterDestination);
        from(longleafDeregisterBatchDestination)
            .to(longleafDeregisterDestination);
        from(longleafDeregisterDestination)
            .routeId("DeregisterLongleafProcessing")
            .startupOrder(1)
            .log(LoggingLevel.DEBUG, log, "Processing batch of longleaf deregistrations")
                .aggregate(longleafAggregationStrategy).constant(true)
                .completionSize(batchSize)
                .completionTimeout(batchTimeout)
            .bean(deregisterProcessor);
    }

    public void setGetUrisProcessor(GetUrisProcessor getUrisProcessor) {
        this.getUrisProcessor = getUrisProcessor;
    }

    public void setRegisterProcessor(RegisterToLongleafProcessor registerProcessor) {
        this.registerProcessor = registerProcessor;
    }

    public void setDeregisterProcessor(DeregisterLongleafProcessor deregisterProcessor) {
        this.deregisterProcessor = deregisterProcessor;
    }

    public void setLongleafAggregationStrategy(LongleafAggregationStrategy longleafAggregationStrategy) {
        this.longleafAggregationStrategy = longleafAggregationStrategy;
    }

    public void setLongleafMaxRedelivieries(int longleafMaxRedelivieries) {
        this.longleafMaxRedelivieries = longleafMaxRedelivieries;
    }

    public void setLongleafRedeliveryDelay(long longleafRedeliveryDelay) {
        this.longleafRedeliveryDelay = longleafRedeliveryDelay;
    }

    public void setLongleafDeadLetterQueueDestination(String longleafDeadLetterQueueDestination) {
        this.longleafDeadLetterQueueDestination = longleafDeadLetterQueueDestination;
    }

    @PropertyInject("longleaf.register.consumer")
    public void setLongleafRegisterConsumer(String longleafRegisterConsumer) {
        this.longleafRegisterConsumer = longleafRegisterConsumer;
    }

    @PropertyInject("longleaf.deregister.consumer")
    public void setLongleafDeregisterConsumer(String longleafDeregisterConsumer) {
        this.longleafDeregisterConsumer = longleafDeregisterConsumer;
    }

    @PropertyInject("longleaf.register.dest")
    public void setLongleafRegisterDestination(String longleafRegisterDestination) {
        this.longleafRegisterDestination = longleafRegisterDestination;
    }

    @PropertyInject("longleaf.deregister.dest")
    public void setLongleafDeregisterDestination(String longleafDeregisterDestination) {
        this.longleafDeregisterDestination = longleafDeregisterDestination;
    }

    @PropertyInject("longleaf.filter.deregister")
    public void setLongleafFilterDeregister(String longleafFilterDeregister) {
        this.longleafFilterDeregister = longleafFilterDeregister;
    }

    @PropertyInject("longleaf.register.batch.consumer")
    public void setLongleafRegisterBatchConsumer(String longleafRegisterBatchConsumer) {
        this.longleafRegisterBatchConsumer = longleafRegisterBatchConsumer;
    }

    @PropertyInject("longleaf.deregister.batch.consumer")
    public void setLongleafDeregisterBatchDestination(String longleafDeregisterBatchDestination) {
        this.longleafDeregisterBatchDestination = longleafDeregisterBatchDestination;
    }

    @PropertyInject("longleaf.batchSize")
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @PropertyInject("longleaf.batchTimeout")
    public void setBatchTimeout(long batchTimeout) {
        this.batchTimeout = batchTimeout;
    }
}
