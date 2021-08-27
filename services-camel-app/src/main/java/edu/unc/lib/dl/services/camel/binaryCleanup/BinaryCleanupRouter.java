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
package edu.unc.lib.dl.services.camel.binaryCleanup;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Router for tasks to cleanup old binary files
 * @author bbpennel
 */
public class BinaryCleanupRouter extends RouteBuilder {
    private final Logger log = getLogger(BinaryCleanupRouter.class);
    @Autowired
    private BinaryCleanupProcessor binaryCleanupProcessor;

    @Override
    public void configure() throws Exception {
        from("{{cdr.registration.successful.dest}}")
            .routeId("CleanupOldBinaryBatch")
            .log(LoggingLevel.DEBUG, log, "Cleaning up old binaries")
            .startupOrder(119)
            .process(binaryCleanupProcessor);
    }
}
