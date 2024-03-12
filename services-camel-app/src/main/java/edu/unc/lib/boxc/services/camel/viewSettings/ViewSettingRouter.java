package edu.unc.lib.boxc.services.camel.viewSettings;

import org.apache.camel.BeanInject;
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

    @Override
    public void configure() throws Exception {
        from("{{cdr.viewsetting.stream.camel}}")
                .routeId("DcrViewSetting")
                .log(DEBUG, log, "Received view setting request")
                .bean(viewSettingRequestProcessor);
    }
}
