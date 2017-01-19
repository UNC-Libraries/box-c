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
import org.fcrepo.camel.processor.EventProcessor;

import edu.unc.lib.cdr.AddDerivativeProcessor;
import edu.unc.lib.cdr.BinaryMetadataProcessor;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;


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
		from("activemq:topic:fedora")
		.routeId("CdrServiceEnhancements")
		.process(new EventProcessor())
		.filter(simple("${headers[org.fcrepo.jms.eventType]} not contains 'NODE_REMOVED'"
				+ " && ${headers[org.fcrepo.jms.eventType]} contains 'ResourceCreation'"
				+ " && ${headers[org.fcrepo.jms.resourceType]} contains '" + Binary.getURI() + "'"))
			.to("fcrepo:{{fcrepo.baseUri}}?preferInclude=ServerManaged&accept=text/turtle")
			.process(mdProcessor)
			.multicast()
			.to("direct:small.thumbnail", "direct:large.thumbnail");

		from("direct:small.thumbnail")
		.log(LoggingLevel.INFO, "Creating/Updating Small Thumbnail for ${headers[binaryPath]}")
		.recipientList(simple("exec:/bin/sh?args=/usr/local/bin/convertScaleStage.sh ${headers[binaryPath]} PNG 64 64 ${headers[CheckSum]}-small&workingDir=/tmp&outFile=/tmp/${headers[CheckSum]}"))
		.bean(addDerivProcessor);
		
		from("direct:large.thumbnail")
		.log(LoggingLevel.INFO, "Creating/Updating Large Thumbnail for ${headers[CheckSum]}")
		.recipientList(simple("exec:/bin/sh?args=/usr/local/bin/convertScaleStage.sh ${headers[binaryPath]} PNG 128 128 ${headers[CheckSum]}-large&workingDir=/tmp&outFile=/tmp/${headers[CheckSum]}"))
		.bean(addDerivProcessor);
	}
}
