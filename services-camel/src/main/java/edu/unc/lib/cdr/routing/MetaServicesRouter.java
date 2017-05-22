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
package edu.unc.lib.cdr.routing;

import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;

import edu.unc.lib.cdr.BinaryMetadataProcessor;

/**
 * Meta router which sequences all service routes to run on events.
 * 
 * @author bbpennel
 *
 */
public class MetaServicesRouter extends RouteBuilder {
	@BeanInject(value = "binaryMetadataProcessor")
	private BinaryMetadataProcessor mdProcessor;

	@PropertyInject(value = "cdr.enhancement.processingThreads")
	private Integer enhancementThreads;

	public void configure() throws Exception {
		from("{{fcrepo.stream}}")
			.routeId("CdrMetaServicesRouter")
			.to("direct-vm:index.start")
			.to("direct:process.ingest");

		from("direct:process.ingest")
			.routeId("ProcessIngest")
			.filter(simple("${headers[org.fcrepo.jms.eventType]} contains 'ResourceCreation'"
					+ " && ${headers[org.fcrepo.jms.identifier]} regex '.*(original_file|techmd_fits)'"
					+ " && ${headers[org.fcrepo.jms.resourceType]} contains '" + Binary.getURI() + "'"))
					
				// Trigger binary processing after an asynchronously
				.threads(enhancementThreads, enhancementThreads, "CdrEnhancementThread")
				.delay(simple("{{cdr.enhancement.postIndexingDelay}}"))
				.to("direct:process.creation");

		from("direct:process.creation")
			.routeId("ProcessCreationEvent")
			.log(LoggingLevel.DEBUG, "Processing binary metadata for ${headers[org.fcrepo.jms.identifier]}")
			.removeHeaders("CamelHttp*")
			.to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=ServerManaged&accept=text/turtle")
			.process(mdProcessor)
			.choice()
				.when(simple("${headers[org.fcrepo.jms.identifier]} regex '.*original_file'"))
					.to("direct-vm:replication")
					.log(LoggingLevel.INFO, "Replication completed for ${headers[org.fcrepo.jms.identifier]}")
					.to("direct:process.enhancements")
				.when(simple("${headers[org.fcrepo.jms.identifier]} regex '.*techmd_fits'"))
					.to("direct-vm:replication")
					.log(LoggingLevel.INFO, "Replication completed for ${headers[org.fcrepo.jms.identifier]}")
				.otherwise()
					.log(LoggingLevel.WARN, "Unable to process binary metadata for ${headers[org.fcrepo.jms.identifier]}")
			.end();
		
		from("direct:process.enhancements")
			.routeId("ProcessEnhancements")
			.multicast()
				.to("direct-vm:imageEnhancements","direct-vm:extractFulltext");
	}

}
