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
package edu.unc.lib.dl.services.camel.fulltext;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

/**
 * Routes ingests with full text available through a pipeline to extract
 * the full text and add it as an object in fedora.
 *
 * @author lfarrell
 *
 */
public class FulltextRouter extends RouteBuilder {

    private static final String MIMETYPE_PATTERN = "^(text/|application/pdf|application/msword"
            + "|application/vnd\\.|application/rtf|application/powerpoint"
            + "|application/postscript).*$";

    @BeanInject(value = "fulltextProcessor")
    private FulltextProcessor ftProcessor;

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
            .redeliveryDelay("{{error.retryDelay}}")
            .maximumRedeliveries("{{error.maxRedeliveries}}")
            .backOffMultiplier("{{error.backOffMultiplier}}")
            .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("direct-vm:process.enhancement.extractFulltext")
            .routeId("CdrServiceFulltextExtraction")
            .log(LoggingLevel.DEBUG, "Calling text extraction route for ${headers[org.fcrepo.jms.identifier]}")
            .filter(simple("${headers[CdrMimeType]} regex '" + MIMETYPE_PATTERN + "'"))
                .log(LoggingLevel.INFO, "Extracting text from ${headers[org.fcrepo.jms.identifier]}"
                        + " of type ${headers[CdrMimeType]}")
                .to("direct:fulltext.extraction");

        from("direct:fulltext.extraction")
            .routeId("ExtractingText")
            .log(LoggingLevel.INFO, "Extracting full text for ${headers[binaryPath]}")
            .bean(ftProcessor);
    }
}
