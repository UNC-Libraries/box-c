package solrUpdateJob;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import edu.unc.lib.cdr.SolrUpdateJobProcessor;

public class SolrUpdateJobRouter extends RouteBuilder {
    @BeanInject(value = "solrUpdateProcessor")
    private SolrUpdateJobProcessor solrUpdateJobProcessor;

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
        .redeliveryDelay("{{error.retryDelay}}")
        .maximumRedeliveries("{{error.maxRedeliveries}}")
        .backOffMultiplier(2)
        .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("{{cdr.stream}}")
            .routeId("CdrServiceSolrUpdateJob")
            .bean(solrUpdateJobProcessor);

    }

}
