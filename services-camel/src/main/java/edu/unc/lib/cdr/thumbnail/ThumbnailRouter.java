/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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
		from("direct-vm:createThumbnail")
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
		//.filter(simple("${headers[MimeType]} regex '^application.*?$'"))
			.filter(simple("${headers[MimeType]} regex '^(image.*$|application.*?(photoshop|psd)$)'"))
			.multicast()
			.to("direct:small.thumbnail", "direct:large.thumbnail");

		from("direct:small.thumbnail")
		.routeId("SmallThumbnail")
		.log(LoggingLevel.INFO, "Creating/Updating Small Thumbnail for ${headers[binaryPath]}")
		.recipientList(simple("exec:/bin/sh?args=/usr/local/bin/convertScaleStage.sh ${headers[BinaryPath]} PNG 64 64 ${properties:services.tempDirectory}${headers[CheckSum]}-small"))
		.bean(addSmallThumbnailProcessor);
		
		from("direct:large.thumbnail")
		.routeId("LargeThumbnail")
		.log(LoggingLevel.INFO, "Creating/Updating Large Thumbnail for ${headers[CheckSum]}")
		.recipientList(simple("exec:/bin/sh?args=/usr/local/bin/convertScaleStage.sh ${headers[BinaryPath]} PNG 128 128 ${properties:services.tempDirectory}${headers[CheckSum]}-large"))
		.bean(addLargeThumbProcessor);
	}
}
