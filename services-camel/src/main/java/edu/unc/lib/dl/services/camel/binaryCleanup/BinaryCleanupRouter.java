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

import static edu.unc.lib.dl.fcrepo4.FcrepoJmsConstants.RESOURCE_TYPE;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;

import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Router for tasks to cleanup old binary files
 * @author bbpennel
 */
public class BinaryCleanupRouter extends RouteBuilder {
    @PropertyInject(value = "cdr.binaryCleanup.delay")
    private Long cleanup_delay;

    @Autowired
    private BinaryCleanupProcessor binaryCleanupProcessor;

    @Override
    public void configure() throws Exception {
        // Queue which interprets fedora messages into enhancement requests
        from("{{cdr.binaryCleanup.camel}}")
            .routeId("CleanupOldBinaryCopies")
            .startupOrder(120)
            .filter(simple("${headers[" + RESOURCE_TYPE + "]} contains '" + Binary.getURI() + "'"))
            .delay(cleanup_delay)
            .process(binaryCleanupProcessor);
    }
}
