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
package edu.unc.lib.dl.services.camel.enhancements;

import static edu.unc.lib.dl.rdf.Cdr.AdminUnit;
import static edu.unc.lib.dl.rdf.Cdr.Collection;
import static edu.unc.lib.dl.rdf.Cdr.DescriptiveMetadata;
import static edu.unc.lib.dl.rdf.Cdr.FileObject;
import static edu.unc.lib.dl.rdf.Cdr.Folder;
import static edu.unc.lib.dl.rdf.Cdr.Work;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrEnhancementSet;
import static org.apache.camel.LoggingLevel.INFO;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;

import edu.unc.lib.dl.services.camel.BinaryMetadataProcessor;
import edu.unc.lib.dl.services.camel.CleanupBinaryProcessor;
import edu.unc.lib.dl.services.camel.GetBinaryProcessor;

/**
 * Router which queues and triggers enhancement services.
 *
 * @author bbpennel
 *
 */
public class EnhancementRouter extends RouteBuilder {

    private static final String DEFAULT_ENHANCEMENTS = "thumbnails,imageAccessCopy,extractFulltext";

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
        from("direct-vm:enhancements.fedora")
            .routeId("QueueEnhancementsFromFedora")
            .log(INFO, "Queuing enhancements from Fedora message for ${headers[CamelFcrepoUri]}")
            .setHeader(CdrEnhancementSet, constant(DEFAULT_ENHANCEMENTS))
            .to("{{cdr.enhancement.stream.camel}}");

        from("{{cdr.enhancement.stream.camel}}")
            .routeId("ProcessEnhancementQueue")
            .log(INFO, "Processing queued enhancements ${headers[CdrEnhancementSet]} for ${headers[CamelFcrepoUri]}")
            .to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=ServerManaged&accept=text/turtle")
            .multicast()
            .to("direct:process.binary", "direct:process.solr");

        from("direct:process.binary")
            .routeId("ProcessOriginalBinary")
            .filter(simple("${headers[CamelFcrepoUri]} contains '/original_file'"
                    + " && ${headers[org.fcrepo.jms.resourceType]} contains '" + Binary.getURI() + "'"))
            .threads(enhancementThreads, enhancementThreads, "CdrEnhancementThread")
            .process(mdProcessor)
            .process(getBinaryProcessor)
            .to("direct:process.enhancements")
            .process(cleanupBinaryProcessor);

        from("direct:process.enhancements")
            .routeId("AddBinaryEnhancements")
            .split(simple("${headers[CdrEnhancementSet]}"))
                .log(LoggingLevel.INFO, "Calling enhancement direct-vm:process.enhancement.${body}")
                .toD("direct-vm:process.enhancement.${body}");

        from("direct:process.solr")
            .routeId("IngestSolrIndexing")
            .log(LoggingLevel.INFO, "Requesting solr indexing of ${headers[CamelFcrepoUri]}"
                    + " with types ${headers[org.fcrepo.jms.resourceType]}")
            .filter(simple("${headers[org.fcrepo.jms.resourceType]} contains '" + Work.getURI() + "'"
                    + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + FileObject.getURI() + "'"
                    + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + Folder.getURI() + "'"
                    + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + Collection.getURI() + "'"
                    + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + AdminUnit.getURI() + "'"
                    ))
            // Filter out descriptive md separately because camel simple filters don't support basic () operators
            .filter(simple("${headers[org.fcrepo.jms.resourceType]} not contains '"
                    + DescriptiveMetadata.getURI() + "'"))
                .log(LoggingLevel.INFO, "Ingest solr indexing for ${headers[CamelFcrepoUri]}")
                .to("direct-vm:solrIndexing");
    }
}
