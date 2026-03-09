package edu.unc.lib.boxc.services.camel.machineGenerated;

import org.apache.camel.BeanInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

public class MachineGenRouter extends RouteBuilder {
    private static final Logger log = getLogger(MachineGenRouter.class);
    @BeanInject("machineGenDescriptionProcessor")
    private MachineGenDescriptionProcessor machineGenDescriptionProcessor;

    @Override
    public void configure() throws Exception {
        from("{{cdr.machine.gen.description.stream.camel}}")
                .routeId("MachineGenDescription")
                .log(DEBUG, log,
                        "Processing machine gen update description request")
//                .multicast()
                // trigger enhancements sequentially followed by indexing
                .bean(machineGenDescriptionProcessor)
                .to("direct:solrIndexing");
    }
}
