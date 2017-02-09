package edu.unc.lib.cdr.routing;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.language.SimpleExpression;

public class MetaServicesRouter extends RouteBuilder {
	
	public void configure() throws Exception {
		System.out.println("STARTING UP THE ROUTER");
		from("{{input.stream}}")
		.routeId("MetaServicesRouter")
		.log("Routing that router")
		.routingSlip(new SimpleExpression("direct-vm:index.start,direct-vm:createThumbnail"));
	}

}
