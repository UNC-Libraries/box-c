package edu.unc.lib.cdr.jp2;

import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import edu.unc.lib.cdr.AddDerivativeProcessor;
import edu.unc.lib.cdr.BinaryMetadataProcessor;

public class AccessImageRouter extends RouteBuilder {
	@BeanInject(value = "binaryMetadataProcessor")
	private BinaryMetadataProcessor mdProcessor;
	
	@BeanInject(value = "addAccessCopyProcessor")
	private AddDerivativeProcessor addAccessCopyProcessor;
	
	public void configure() throws Exception {
		from("direct-vm:createJpeg2000")
		.routeId("CdrServiceEnhancements")
		.filter(simple("${headers[org.fcrepo.jms.eventType]} contains 'ResourceCreation'"
				+ " && ${headers[org.fcrepo.jms.identifier]} regex '.*original_file'"
				+ " && ${headers[org.fcrepo.jms.resourceType]} contains '" + Binary.getURI() + "'"))
			.removeHeaders("CamelHttp*")
			.to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=ServerManaged&accept=text/turtle")
			.process(mdProcessor)
			.to("direct:images");
			
		from("direct:images")
		.routeId("IsImage")
			.filter(simple("${headers[MimeType]} regex '^application.*?$'"))
			//.filter(simple("${headers[MimeType]} regex '${properties:cdr.enhancement.imageRegex})'"))
			.to("direct:accessImage");
		
		from("direct:accessImage")
		.routeId("AccessCopy")
		.log(LoggingLevel.INFO, "Creating/Updating JP2 access copy for ${headers[CheckSum]}")
		.recipientList(simple("exec:/bin/sh?args=/usr/local/bin/convertScaleStage.sh ${headers[BinaryPath]} PNG ${properties:services.tempDirectory}${headers[CheckSum]}-access"))
		.bean(addAccessCopyProcessor);
	}
}
