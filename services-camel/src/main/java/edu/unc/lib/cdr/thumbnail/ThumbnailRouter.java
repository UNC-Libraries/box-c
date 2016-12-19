/**
 * Copyright 2016 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.cdr.thumbnail;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.fcrepo.camel.RdfNamespaces;

import edu.unc.lib.cdr.AddDerivativeProcessor;
import edu.unc.lib.cdr.BinaryMetadataProcessor;


/**
 * Router which triggers the creation of thumbnails when applicable binaries
 * are written.
 *
 */
public class ThumbnailRouter extends RouteBuilder {
	@BeanInject(value = "binaryMetadataProcessor")
	private BinaryMetadataProcessor mdProcessor;
	
	@BeanInject(value = "addDerivativeProcessor")
	private AddDerivativeProcessor addDerivProcessor;
	
	/**
	 * Configure the thumbnail route workflow.
	 */
	public void configure() throws Exception {
		final Namespaces ns = new Namespaces("rdf", RdfNamespaces.RDF);
		
		from("activemq:topic:fedora")
		.routeId("CdrServiceEnhancements")
		.log("Dean: ${headers}")
		.filter(simple("${headers[org.fcrepo.jms.eventType]} not contains 'NODE_REMOVED' && ${headers[org.fcrepo.jms.eventType]} contains 'ResourceCreation'"))
			.to("fcrepo:{{fcrepo.baseUri}}?preferInclude=ServerManged&accept=application/rdf+xml")
			.filter()
			.xpath("/rdf:RDF/rdf:Description/rdf:type[@rdf:resource='http://fedora.info/definitions/v4/repository#Binary']", ns)
				.to("fcrepo:{{fcrepo.baseUri}}?preferInclude=ServerManged&accept=text/turtle")
				.process(mdProcessor).id("thumbBinaryMetadataProcessor")
				.multicast()
				.to("direct:small.thumbnail", "direct:large.thumbnail");

		from("direct:small.thumbnail")
		.log(LoggingLevel.INFO, "Grand Creating/Updating Small Thumbnail ${headers[BaseURL]}")
		.to("exec:/bin/sh?args=/usr/local/bin/convertScaleStage.sh ${headers[binaryPath]} PNG 64 64 ${headers[CheckSum]}-small&workingDir=/tmp&outFile=/tmp/${headers[CheckSum]}")
		.bean(addDerivProcessor);
	//	.bean(FileObject.class, new PID("${headers.org.fcrepo.jms.baseURL}/${headers.org.fcrepo.jms.identifier}"), *, *);
		
		from("direct:large.thumbnail")
		.log(LoggingLevel.INFO, "Creating/Updating Large Thumbnail")
		.to("exec:/bin/sh?args=/usr/local/bin/convertScaleStage.sh ${headers[binaryPath]} PNG 128 128 ${headers[CheckSum]}-large&workingDir=/tmp&outFile=/tmp/${headers[CheckSum]}")
		.bean(addDerivProcessor);
	}
}
