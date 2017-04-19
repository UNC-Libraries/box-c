package edu.unc.lib.cdr.replication;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import edu.unc.lib.cdr.ReplicationProcessor;

public class ReplicationRouter extends RouteBuilder {
	@BeanInject(value = "replicationProcessor")
	private ReplicationProcessor replicationProcessor;
	
	public void configure() throws Exception {
		onException(Exception.class)
		.redeliveryDelay("{{error.retryDelay}}")
		.maximumRedeliveries("{{error.maxRedeliveries}}")
		.backOffMultiplier(2)
		.retryAttemptedLogLevel(LoggingLevel.WARN);
		
		from("direct-vm:replication")
		.routeId("CdrReplicationRoute")
		.log(LoggingLevel.INFO, "Calling replication route for ${headers[org.fcrepo.jms.identifier]}")
		.to("direct:file.replication");
		
		from("direct:file.replication")
		.bean(replicationProcessor);
	}
}
