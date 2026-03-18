package edu.unc.lib.boxc.services.camel.machineGenerated;

import edu.unc.lib.boxc.services.camel.images.ImageDerivativeProcessor;
import org.apache.camel.BeanInject;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Router for processing machine gen description updates via boxctron
 * @author snluong
 */
public class MachineGenRouter extends RouteBuilder {
    private static final Logger log = getLogger(MachineGenRouter.class);
    @BeanInject("machineGenDescriptionProcessor")
    private MachineGenDescriptionProcessor machineGenDescriptionProcessor;

    private String machineGenDescriptionStreamCamel;

    @Override
    public void configure() throws Exception {
        from(machineGenDescriptionStreamCamel)
                .routeId("MachineGenDescription")
                .log(DEBUG, log, "Processing machine gen update description request")
                .filter().method(machineGenDescriptionProcessor, "needsRun")
                .filter().method(ImageDerivativeProcessor.class, "allowedImageType")
                .bean(machineGenDescriptionProcessor);
    }

    public void setMachineGenDescriptionProcessor(MachineGenDescriptionProcessor machineGenDescriptionProcessor) {
        this.machineGenDescriptionProcessor = machineGenDescriptionProcessor;
    }

    @PropertyInject("cdr.machine.gen.description.stream.camel")
    public void setMachineGenDescriptionStreamCamel(String machineGenDescriptionStreamCamel) {
        this.machineGenDescriptionStreamCamel = machineGenDescriptionStreamCamel;
    }
}
