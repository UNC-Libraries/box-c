package edu.unc.lib.cdr.thumbnail;

import java.io.InputStream;
import java.util.StringJoiner;

import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;

//import static org.apache.http.entity.ContentType.parse;
import com.hp.hpl.jena.rdf.model.ModelFactory;
//import static com.hp.hpl.jena.riot.RDFLanguages.contentTypeToLang;

import com.hp.hpl.jena.rdf.model.Model;
//import org.fcrepo.camel.processor.EventProcessor;

import com.google.common.base.Splitter;


public class ThumbnailRouter extends RouteBuilder {
	private static final String EVENT_TYPE = "http://fedora.info/definitions/v4/event";
	
	/**
	 * Configure the thumbnail route workflow.
	 */
	public void configure() throws Exception {
		final String baseBinaryPath = "/var/lib/tomcat7/fcrepo4-data/fcrepo.binary.directory/";
		Predicate isCreated = header("org.fcrepo.jms.eventType").isEqualTo(EVENT_TYPE + "#ResourceCreation");
		Predicate isBinary = xpath("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/repository#Binary']");
		
		from("activemq:topic:fedora")
		.routeId("CdrServiceEnhancements")
		.log(LoggingLevel.INFO, "Thumbnail Creation")
//		.process(new EventProcessor())
		.filter(simple("${headers[org.fcrepo.jms.eventType]} not contains 'NODE_REMOVED'"))
		.to("fcrepo:localhost:8080/fcrepo/rest")
		.log(LoggingLevel.INFO, "INFO-HEADERS: ${headers}")
		.process(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				final Message in = exchange.getIn();
				final Model model = ModelFactory.createDefaultModel();
				
			//	Model values = model.read(in.getBody(InputStream.class),
			//			contentTypeToLang(parse(in.getHeader(Exchange.CONTENT_TYPE, String.class)).getMimeType()).toString());
				
				Model values = model.read(in.getBody(InputStream.class),  null, "TURTLE");
				
				String fcrepoMimeType = values.getProperty("hasMimeType").toString();
				String fcrepoChecksum = values.getProperty("hasMessageDigest").toString();
				String fcrepoChecksumStart = fcrepoChecksum.substring(0, 7);
				
				String binaryPath = "";
				for (String substring : Splitter.fixedLength(2).split(fcrepoChecksumStart)) {
					binaryPath += substring + "/";
				}
				
				String fullPath = new StringJoiner("")
					.add(baseBinaryPath)
					.add(binaryPath)
					.add(fcrepoChecksum)
					.toString();
				
				in.setHeader("MimeType", fcrepoMimeType);
				in.setHeader("CheckSum", fcrepoChecksum);
				in.setHeader("BinaryPath", fullPath);
			}
		})
		
		.choice()
			.when(PredicateBuilder.and(isCreated, isBinary, header("mimeType").startsWith("image")))
				.to("fcrepo:localhost:8080/fcrepo/rest?metadata=false")
				.setHeader("CamelFileName", simple("${headers[org.fcrepo.jms.identifier]}"))
				.multicast()
				.to("direct:small.thumbnail", "direct:large.thumbnail");

		from("direct:small.thumbnail")
		.log(LoggingLevel.INFO, "Creating/Updating Small Thumbnail")
		.recipientList(simple("exec:/bin/sh?args=/usr/local/bin/convertScaleStage.sh ${headers[binaryPath]} PNG 64 64&amp;workingDir=/tmp&amp;outFile=/tmp/${headers[CheckSum]}"));
		
		from("direct:large.thumbnail")
		.log(LoggingLevel.INFO, "Creating/Updating Large Thumbnail")
		.recipientList(simple("exec:/bin/sh?args=/usr/local/bin/convertScaleStage.sh ${headers[binaryPath]} PNG 128 128&amp;workingDir=/tmp&amp;outFile=/tmp/${headers[CheckSum]}"));
	}
}
