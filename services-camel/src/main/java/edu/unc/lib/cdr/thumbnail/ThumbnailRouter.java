package edu.unc.lib.cdr.thumbnail;

import java.io.InputStream;
import java.util.StringJoiner;

import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.fcrepo.camel.RdfNamespaces;

import static com.hp.hpl.jena.rdf.model.ModelFactory.createDefaultModel;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.google.common.base.Splitter;

import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import static edu.unc.lib.dl.rdf.Ebucore.hasMimeType;
import static edu.unc.lib.dl.rdf.Premis.hasMessageDigest;


public class ThumbnailRouter extends RouteBuilder {
	/**
	 * Configure the thumbnail route workflow.
	 */
	public void configure() throws Exception {
		final String baseBinaryPath = "/var/lib/tomcat7/fcrepo4-data/fcrepo.binary.directory/";
		final Namespaces ns = new Namespaces("rdf", RdfNamespaces.RDF);
		
		from("activemq:topic:fedora")
		.routeId("CdrServiceEnhancements")
		.filter(simple("${headers[org.fcrepo.jms.eventType]} not contains 'NODE_REMOVED' && ${headers[org.fcrepo.jms.eventType]} contains 'ResourceCreation'"))
			.to("fcrepo:localhost:8080/fcrepo/rest?preferInclude=ServerManged&accept=application/rdf+xml")
			.filter()
			.xpath("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/repository#Binary']", ns)
				.to("fcrepo:localhost:8080/fcrepo/rest?preferInclude=ServerManged&accept=text/turtle")
				.process(new Processor() {
					@Override
					public void process(final Exchange exchange) throws Exception {
						final Message in = exchange.getIn();
						final Model model = createDefaultModel();
	
						Model values = model.read(in.getBody(InputStream.class),  null, "Turtle");
						ResIterator resources = values.listResourcesWithProperty(RDF.type, Fcrepo4Repository.Binary);
						
						if (resources.hasNext()) {
							Resource resource = resources.next();
							String mimeType = resource.getProperty(hasMimeType).getObject().toString();
							String fcrepoChecksum = resource.getProperty(hasMessageDigest).getObject().toString();
							String[] fcrepoChecksumSplit = fcrepoChecksum.split(":");
							String fcrepoChecksumStart = fcrepoChecksumSplit[2].substring(0, 6);
								
							String binaryPath = "";
							for (String substring : Splitter.fixedLength(2).split(fcrepoChecksumStart)) {
								binaryPath += substring + "/";
							}
								
							String fullPath = new StringJoiner("")
								.add(baseBinaryPath)
								.add(binaryPath)
								.add(fcrepoChecksumSplit[2])
								.toString();
								
							in.setHeader("Checksum", fcrepoChecksumSplit[2]);
							in.setHeader("MimeType", mimeType);
							in.setHeader("BinaryPath", fullPath);
						}
					}
				})
				.multicast()
				.to("direct:small.thumbnail", "direct:large.thumbnail");

		from("direct:small.thumbnail")
		.log(LoggingLevel.INFO, "Creating/Updating Small Thumbnail")
		.recipientList(simple("exec:/bin/sh?args=/usr/local/bin/convertScaleStage.sh ${headers[binaryPath]} PNG 64 64 ${headers[CheckSum]}-small&workingDir=/tmp&outFile=/tmp/${headers[CheckSum]}"));
		
		from("direct:large.thumbnail")
		.log(LoggingLevel.INFO, "Creating/Updating Large Thumbnail")
		.recipientList(simple("exec:/bin/sh?args=/usr/local/bin/convertScaleStage.sh ${headers[binaryPath]} PNG 128 128 ${headers[CheckSum]}-large&workingDir=/tmp&outFile=/tmp/${headers[CheckSum]}"));
	}
}
