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
            .to("direct:process.enhancement");

        from("direct:process.enhancement")
            .routeId("ProcessEnhancement")
            .filter(simple("${headers[org.fcrepo.jms.eventType]} contains 'ResourceCreation'"))
                // Trigger binary processing after an asynchronously
                .threads(enhancementThreads, enhancementThreads, "CdrEnhancementThread")
                .delay(simple("{{cdr.enhancement.postIndexingDelay}}"))
                .multicast()
                .to("direct:process.binary.original", "direct:process.solr");

        from("direct:process.binary.original")
            .routeId("ProcessOriginalBinary")
            .log(LoggingLevel.DEBUG, "Processing binary metadata for ${headers[org.fcrepo.jms.identifier]}")
            .filter(simple("${headers[org.fcrepo.jms.identifier]} regex '.*original_file'"
                    + " && ${headers[org.fcrepo.jms.resourceType]} contains '" + Binary.getURI() + "'"))
            .removeHeaders("CamelHttp*")
            .to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=ServerManaged&accept=text/turtle")
            .process(mdProcessor)
            .multicast()
                .to("direct-vm:imageEnhancements","direct-vm:extractFulltext");

        from("direct:process.solr")
            .routeId("IngestSolrIndexing")
            .log("Dean: ${headers}")
            .log(LoggingLevel.DEBUG, "Ingest solr indexing for ${headers[org.fcrepo.jms.identifier]}")
            .filter(simple("${headers[org.fcrepo.jms.resourceType]} not contains '" + Binary.getURI() + "'"))
            .removeHeaders("CamelHttp*")
            .to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=ServerManaged&accept=text/turtle")
            .to("direct-vm:solrIndexing");
    }

}
