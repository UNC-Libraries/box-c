package edu.unc.lib.boxc.services.camel.viewSettings;

import org.apache.camel.BeanInject;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Router for processing requests to update view setting of UV
 *
 * @author snluong
 */
public class ViewSettingRouter extends RouteBuilder {
    private static final Logger log = getLogger(ViewSettingRouter.class);

    @BeanInject(value = "viewSettingRequestProcessor")
    private ViewSettingRequestProcessor viewSettingRequestProcessor;
    private String viewSettingStreamCamel;

    @Override
    public void configure() throws Exception {
        from(viewSettingStreamCamel)
                .routeId("DcrViewSetting")
                .log(DEBUG, log, "Received view setting request")
                .bean(viewSettingRequestProcessor);
    }

    public void setViewSettingRequestProcessor(ViewSettingRequestProcessor viewSettingRequestProcessor) {
        this.viewSettingRequestProcessor = viewSettingRequestProcessor;
    }

    @PropertyInject("cdr.viewsetting.stream.camel")
    public void setViewSettingStreamCamel(String viewSettingStreamCamel) {
        this.viewSettingStreamCamel = viewSettingStreamCamel;
    }
}
