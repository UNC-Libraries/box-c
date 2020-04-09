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
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrEditThumbnail;
import static org.apache.camel.LoggingLevel.DEBUG;
import static org.apache.camel.LoggingLevel.INFO;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;

import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.services.camel.BinaryEnhancementProcessor;
import edu.unc.lib.dl.services.camel.BinaryMetadataProcessor;

/**
 * Router which queues and triggers enhancement services.
 *
 * @author bbpennel
 * @author lfarrell
 *
 */
public class EnhancementRouter extends RouteBuilder {
    @BeanInject(value = "binaryEnhancementProcessor")
    private BinaryEnhancementProcessor enProcessor;

    @BeanInject(value = "binaryMetadataProcessor")
    private BinaryMetadataProcessor mdProcessor;

    @PropertyInject(value = "cdr.enhancement.processingThreads")
    private Integer enhancementThreads;

    @Override
    public void configure() throws Exception {
        from("{{cdr.enhancement.stream.camel}}")
            .routeId("ProcessEnhancementQueue")
            .process(enProcessor)
            .to("fcrepo:{{fcrepo.baseUrl}}?preferInclude=ServerManaged&accept=text/turtle")
            .choice()
                // Process binary enhancement requests
                .when(simple("${headers[org.fcrepo.jms.resourceType]} contains '" + Binary.getURI() + "'"))
                    .log(INFO, "Processing binary ${headers[CamelFcrepoUri]}")
                    .to("direct:process.binary")
                .when(simple("${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.Work.getURI() + "'"
                        + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.FileObject.getURI() + "'"
                        + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.Folder.getURI() + "'"
                        + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.Collection.getURI() + "'"
                        + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.AdminUnit.getURI() + "'"
                        + " || ${headers[org.fcrepo.jms.resourceType]} contains '" + Cdr.ContentRoot.getURI() + "'"
                        ))
                    .log(DEBUG, "Processing enhancements for non-binary ${headers[CamelFcrepoUri]}")
                    .to("direct-vm:solrIndexing")
            .end();

        from("direct:process.binary")
            .routeId("ProcessOriginalBinary")
            .filter(simple("${headers[CamelFcrepoUri]} ends with '/original_file' || " +
                    "${headers[" + CdrEditThumbnail + "]} == 'true'"))
            .log(INFO, "Processing queued enhancements ${headers[CdrEnhancementSet]} for ${headers[CamelFcrepoUri]}")
            .threads(enhancementThreads, enhancementThreads, "CdrEnhancementThread")
            .process(mdProcessor)
            .filter(header(CdrBinaryPath).isNotNull())
                .choice()
                    .when(simple("${headers[" + CdrEditThumbnail + "]} == 'true'"))
                        .to("direct:process.enhancements")
                    .otherwise()
                        .multicast()
                        .to("direct:process.enhancements", "direct-vm:solrIndexing")
                .end();

        from("direct:process.enhancements")
            .routeId("AddBinaryEnhancements")
            .split(simple("${headers[CdrEnhancementSet]}"))
                .log(LoggingLevel.INFO, "Calling enhancement direct-vm:process.enhancement.${body}")
                .toD("direct-vm:process.enhancement.${body}");
    }
}
