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

package edu.unc.lib.cdr.fulltext;

import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import edu.unc.lib.cdr.BinaryMetadataProcessor;
import edu.unc.lib.cdr.FulltextProcessor; 

/**
 * Routes ingests with full text availablethrough a pipeline to extract the full text and add it as an object in fedora.
 * 
 * @author lfarrell
 *
 */
public class FulltextRouter extends RouteBuilder {
	@BeanInject(value = "binaryMetadataProcessor")
	private BinaryMetadataProcessor mdProcessor;
	
	@BeanInject(value = "fulltextProcessor")
	private FulltextProcessor ftProcessor;
	
	
	public void configure() throws Exception {
		String mimetypePattern = "^(text/|application/pdf|application/msword|application/vnd\\.|application/rtf|application/powerpoint|application/postscript).*$";
		
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
		.filter(simple("${headers[MimeType]} regex '" + mimetypePattern + "'"))
			.to("direct:fulltext.extraction");
		
		from("direct:fulltext.extraction")
		.routeId("ExtractingText")
		.log(LoggingLevel.INFO, "Extracting full text for ${headers[binaryPath]}")
		.bean(ftProcessor);
	} 
}

