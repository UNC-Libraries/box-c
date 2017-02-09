package edu.unc.lib.cdr.routing;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.language.SimpleExpression;

public class MetaServicesRouter extends RouteBuilder {
	
	public void configure() throws Exception {
		from("{{fcrepo.stream}}")
		.routeId("MetaServicesRouter")
		.routingSlip(new SimpleExpression("direct-vm:index.start,direct-vm:createThumbnail"));
	}

}
