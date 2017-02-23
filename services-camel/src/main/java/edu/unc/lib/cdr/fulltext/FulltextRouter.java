package edu.unc.lib.cdr.fulltext;

import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import edu.unc.lib.cdr.BinaryMetadataProcessor;
import edu.unc.lib.cdr.FulltextProcessor; 

public class FulltextRouter extends RouteBuilder {
	@BeanInject(value = "binaryMetadataProcessor")
	private BinaryMetadataProcessor mdProcessor;
	
	@BeanInject(value = "fulltextProcessor")
	private FulltextProcessor ftProcessor;
	
	
	public void configure() throws Exception {
		//String mimetypePattern = "^(text/|application/pdf|application/msword|application/vnd\\.|application/rtf|application/powerpoint|application/postscript).*)";
		String mimetypePattern = "^application.*?$";
		
		from("direct-vm:extractFulltext")
		.routeId("CdrServiceFulltextExtraction")
		.filter(simple("${headers[org.fcrepo.jms.eventType]} not contains 'NODE_REMOVED'"
				+ " && ${headers[org.fcrepo.jms.identifier]} regex '.*original_file'"
				+ " && ${headers[org.fcrepo.jms.eventType]} contains 'ResourceCreation'"
				+ " && ${headers[org.fcrepo.jms.resourceType]} contains '" + Binary.getURI() + "'"))
			.removeHeaders("CamelHttp*")
			.to("fcrepo:{{fcrepo.baseUri}}?preferInclude=ServerManaged&accept=text/turtle")
			.process(mdProcessor)
			.to("direct:fulltext.filter");
		
		from("direct:fulltext.filter")
		.routeId("HasFulltext")
		.filter(simple("${headers[MimeType]} regex '^application.*?$'"))
			.to("direct:fulltext.extraction");
		
		from("direct:fulltext.extraction")
		.routeId("ExtractingText")
		.log(LoggingLevel.INFO, "Extracting full text for ${headers[binaryPath]}")
		.bean(ftProcessor);
	} 
}

