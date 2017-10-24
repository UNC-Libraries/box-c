/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.services.camel.routing;

import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;

import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.services.camel.BinaryMetadataProcessor;
import edu.unc.lib.dl.services.camel.CleanupBinaryProcessor;
import edu.unc.lib.dl.services.camel.GetBinaryProcessor;

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

    @BeanInject(value = "getBinaryProcessor")
    private GetBinaryProcessor getBinaryProcessor;

    @BeanInject(value = "cleanupBinaryProcessor")
    private CleanupBinaryProcessor cleanupBinaryProcessor;

    @Override
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
                .removeHeaders("CamelHttp*")
                .to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=ServerManaged&accept=text/turtle")
                .multicast()
                .to("direct:process.binary", "direct:process.solr");

        from("direct:process.binary")
            .routeId("ProcessOriginalBinary")
            .filter(simple("${headers[org.fcrepo.jms.identifier]} regex '.*(original_file|techmd_fits)'"
                    + " && ${headers[org.fcrepo.jms.resourceType]} contains '" + Binary.getURI() + "'"))
                .process(mdProcessor)
                .process(getBinaryProcessor)
                .choice()
                    .when(simple("${headers[org.fcrepo.jms.identifier]} regex '.*original_file'"))
                        .to("direct-vm:replication")
                        .to("direct:process.enhancements")
                    .when(simple("${headers[org.fcrepo.jms.identifier]} regex '.*techmd_fits'"))
                        .to("direct-vm:replication")
                    .otherwise()
                        .log(LoggingLevel.WARN,
                                "Cannot process binary metadata for ${headers[org.fcrepo.jms.identifier]}")
                .end()
                .process(cleanupBinaryProcessor);

        from("direct:process.enhancements")
            .routeId("AddBinaryEnhancements")
            .multicast()
            .to("direct-vm:imageEnhancements","direct-vm:extractFulltext");

        from("direct:process.solr")
            .routeId("IngestSolrIndexing")
            .filter(simple("${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.Work.getURI() + "'"
                    + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.FileObject.getURI() + "'"
                    + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.Folder.getURI() + "'"
                    + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.Collection.getURI() + "'"
                    + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.AdminUnit.getURI() + "'"
                    ))
            // Filter out descriptive md separately because camel simple filters don't support basic () operators
            .filter(simple("${headers[org.fcrepo.jms.resourceType]} not contains '"
                    + Cdr.DescriptiveMetadata.getURI() + "'"))
                .log(LoggingLevel.INFO, "Ingest solr indexing for ${headers[org.fcrepo.jms.identifier]}")
                .to("direct-vm:solrIndexing");
    }
}
