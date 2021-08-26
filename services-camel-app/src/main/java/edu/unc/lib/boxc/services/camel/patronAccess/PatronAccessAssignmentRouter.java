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
package edu.unc.lib.boxc.services.camel.patronAccess;

import static org.apache.camel.LoggingLevel.DEBUG;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.camel.BeanInject;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;

/**
 * Route which executes requests to update patron access control
 *
 * @author bbpennel
 */
public class PatronAccessAssignmentRouter extends RouteBuilder {
    private static final Logger log = getLogger(PatronAccessAssignmentRouter.class);

    @BeanInject(value = "patronAccessAssignmentProcessor")
    private PatronAccessAssignmentProcessor patronAccessAssignmentProcessor;

    @Override
    public void configure() throws Exception {
        from("{{cdr.patron.access.assignment.stream.camel}}")
            .routeId("CdrUpdatePatronAccess")
            .log(DEBUG, log, "Received patron access assignment message")
            .bean(patronAccessAssignmentProcessor);
    }
}
