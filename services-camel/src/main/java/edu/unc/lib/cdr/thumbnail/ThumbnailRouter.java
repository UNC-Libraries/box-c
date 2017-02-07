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

import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.fcrepo.camel.processor.EventProcessor;

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
	
	@BeanInject(value = "addSmallThumbnailProcessor")
	private AddDerivativeProcessor addSmallThumbnailProcessor;
	
	@BeanInject(value = "addLargeThumbnailProcessor")
	private AddDerivativeProcessor addLargeThumbProcessor;
	
	/**
	 * Configure the thumbnail route workflow.
	 */
	public void configure() throws Exception {
		// See Triplestore Router routes for where this comes from
		from("direct-vm:createThumbnail")
		.log("hooray: ${headers[org.fcrepo.jms.eventType]}")
	//	.log("${headers[org.fcrepo.jms.identifier]}")
	//	.log("${headers[org.fcrepo.jms.resourceType]}")
		.routeId("CdrServiceEnhancements")
		//.process(new EventProcessor())
		.log("Cooc: ${headers[org.fcrepo.jms.resourceType]}")
		.filter(simple("${headers[org.fcrepo.jms.eventType]} contains 'ResourceCreation'"
				+ " && ${headers[org.fcrepo.jms.identifier]} regex '.*original_file'"
				+ " && ${headers[org.fcrepo.jms.resourceType]} contains '" + Binary.getURI() + "'"))
			.to("fcrepo:{{fcrepo.baseUri}}?preferInclude=ServerManaged&accept=text/turtle")
			.process(mdProcessor)
			.log("Mooc: ${headers[MimeType]}")
			.filter(simple("${headers[MimeType]} regex '^application.*?$'"))
			//.filter(simple("${headers[MimeType]} regex '^(image.*?$|application.*?(photoshop|psd)$)'"))
				.multicast()
				.to("direct:small.thumbnail", "direct:large.thumbnail");

		from("direct:small.thumbnail")
		.log(LoggingLevel.INFO, "Creating/Updating Small Thumbnail for ${headers[binaryPath]}")
		.recipientList(simple("exec:/bin/sh?args=/usr/local/bin/convertScaleStage.sh ${headers[BinaryPath]} PNG 64 64 ${properties:services.tempDirectory}${headers[CheckSum]}-small"))
		//.delay(1000)
		.bean(addSmallThumbnailProcessor)
		.end();
		
		from("direct:large.thumbnail")
		.log(LoggingLevel.INFO, "Creating/Updating Large Thumbnail for ${headers[CheckSum]}")
		.recipientList(simple("exec:/bin/sh?args=/usr/local/bin/convertScaleStage.sh ${headers[BinaryPath]} PNG 128 128 ${properties:services.tempDirectory}${headers[CheckSum]}-large"))
		//.delay(1000)
		.bean(addLargeThumbProcessor)
		.end();
	}
}
