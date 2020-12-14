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

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

import edu.unc.lib.dl.services.camel.images.AddDerivativeProcessor;

/**
 * Routes ingests with full text available through a pipeline to extract
 * the full text and add it as an object in fedora.
 *
 * @author lfarrell
 *
 */
public class FulltextRouter extends RouteBuilder {
    private static final Logger log = getLogger(FulltextRouter.class);

    @BeanInject(value = "fulltextProcessor")
    private FulltextProcessor ftProcessor;

    @BeanInject(value = "addFullTextDerivativeProcessor")
    private AddDerivativeProcessor adProcessor;

    @Override
    public void configure() throws Exception {

        from("direct:process.enhancement.extractFulltext")
            .routeId("CdrServiceFulltextExtraction")
            .startupOrder(31)
            .log(LoggingLevel.DEBUG, log, "Calling text extraction route for ${headers[CamelFcrepoUri]}")
            .filter().method(adProcessor, "needsRun")
            .filter().method(FulltextProcessor.class, "allowedTextType")
            .log(LoggingLevel.INFO, log, "Extracting text from ${headers[CamelFcrepoUri]}"
                    + " of type ${headers[CdrMimeType]}")
            .to("direct:fulltext.extraction");

        from("direct:fulltext.extraction")
            .routeId("ExtractingText")
            .startupOrder(30)
            .log(LoggingLevel.DEBUG, log, "Extracting full text for ${headers[CdrBinaryPath]}")
            .bean(ftProcessor);
    }
}
