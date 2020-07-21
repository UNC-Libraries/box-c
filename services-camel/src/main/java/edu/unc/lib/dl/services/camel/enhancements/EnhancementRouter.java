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

import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrEnhancementSet;
import static org.apache.camel.LoggingLevel.DEBUG;
import static org.apache.camel.LoggingLevel.INFO;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.services.camel.BinaryEnhancementProcessor;
import edu.unc.lib.dl.services.camel.BinaryMetadataProcessor;
import edu.unc.lib.dl.services.camel.NonBinaryEnhancementProcessor;

/**
 * Router which queues and triggers enhancement services.
 *
 * @author bbpennel
 * @author lfarrell
 *
 */
public class EnhancementRouter extends RouteBuilder {
    private static final Logger log = getLogger(EnhancementRouter.class);

    @BeanInject(value = "binaryEnhancementProcessor")
    private BinaryEnhancementProcessor enProcessor;

    @BeanInject(value = "binaryMetadataProcessor")
    private BinaryMetadataProcessor mdProcessor;

    @BeanInject(value = "nonBinaryEnhancementProcessor")
    private NonBinaryEnhancementProcessor nbProcessor;

    @PropertyInject(value = "cdr.enhancement.processingThreads")
    private Integer enhancementThreads;

    private static final String DEFAULT_ENHANCEMENTS = "thumbnails,imageAccessCopy,extractFulltext";
    private static final String THUMBNAIL_ENHANCEMENTS = "thumbnails";
    @Override
    public void configure() throws Exception {

        // Queue which interprets fedora messages into enhancement requests
        from("{{cdr.enhancement.stream.camel}}")
            .routeId("ProcessEnhancementQueue")
            .startupOrder(110)
            .process(enProcessor)
            .to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=ServerManaged&accept=text/turtle")
            .choice()
                .when(simple("${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.Tombstone.getURI() + "'"))
                    .log(DEBUG, log, "Ignoring tombstone object for enhancements ${headers[CamelFcrepoUri]}")
                // Process binary enhancement requests
                .when(simple("${headers[org.fcrepo.jms.resourceType]} contains '" + Binary.getURI() + "'"))
                    .log(INFO, log, "Processing binary ${headers[CamelFcrepoUri]}")
                    .to("direct:process.binary")
                .when(simple("${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.Work.getURI() + "'"
                        + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.FileObject.getURI() + "'"
                        + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.Folder.getURI() + "'"
                        + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.Collection.getURI() + "'"
                        + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.AdminUnit.getURI() + "'"
                        + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.ContentRoot.getURI() + "'"
                        ))
                    .log(DEBUG, log, "Processing enhancements for non-binary ${headers[CamelFcrepoUri]}")
                    .process(nbProcessor)
                    .setHeader(CdrEnhancementSet, constant(THUMBNAIL_ENHANCEMENTS))
                    .to("{{cdr.enhancement.perform.camel}}")
            .end();

        // Route which processes fedora binary objects
        from("direct:process.binary")
            .routeId("ProcessBinary")
            .startupOrder(109)
            .multicast()
            .to("direct-vm:filter.longleaf", "direct:process.original");

        // Route to perform enhancements IF a binary is an original file
        from("direct:process.original")
            .routeId("ProcessOriginalBinary")
            .startupOrder(108)
            .filter(simple("${headers[CamelFcrepoUri]} ends with '/original_file'"))
                .setHeader(CdrEnhancementSet, constant(DEFAULT_ENHANCEMENTS))
                .process(mdProcessor)
                .filter(header(CdrBinaryPath).isNotNull())
                    .to("{{cdr.enhancement.perform.camel}}");

        // Queue for executing enhancnement operations
        from("{{cdr.enhancement.perform.camel}}")
            .routeId("PerformEnhancementsQueue")
            .startupOrder(107)
            .choice()
            .when(simple("${headers[" + CdrBinaryPath + "]} == null"))
                .log(INFO, log, "Indexing queued resource without binary path ${headers[CamelFcrepoUri]}")
                .to("direct:solrIndexing")
            .otherwise()
                .log(INFO, log, "Processing queued enhancements ${headers[CdrEnhancementSet]}" +
                    "for ${headers[CamelFcrepoUri]}")
                .multicast()
                // trigger enhancements sequentially followed by indexing
                .to("direct:process.enhancements", "direct:solrIndexing")
            .end();

        // Expands enhancement requests into individual services
        from("direct:process.enhancements")
            .routeId("AddBinaryEnhancements")
            .startupOrder(106)
            .doTry()
                .split(simple("${headers[CdrEnhancementSet]}"))
                    .shareUnitOfWork()
                    .log(LoggingLevel.INFO, log, "Calling enhancement direct:process.enhancement.${body}")
                    .toD("direct:process.enhancement.${body}")
                .end()
            .endDoTry()
            .doCatch(IllegalStateException.class)
                .log(LoggingLevel.WARN, log, "Shutdown interrupted processing of ${headers[CdrBinaryPath]}, requeuing")
                .setHeader("AMQ_SCHEDULED_DELAY", constant("10000"))
                .inOnly("{{cdr.enhancement.perform.camel}}");

    }
}
