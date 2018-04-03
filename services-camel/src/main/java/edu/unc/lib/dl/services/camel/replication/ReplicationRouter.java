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
package edu.unc.lib.dl.services.camel.replication;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

/**
 * Routes binary files for replication to remote storage devices
 *
 * @author lfarrell
 *
 */
public class ReplicationRouter extends RouteBuilder {
    @BeanInject(value = "replicationProcessor")
    private ReplicationProcessor replicationProcessor;

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
            .redeliveryDelay("{{error.retryDelay}}")
            .maximumRedeliveries("{{error.maxRedeliveries}}")
            .backOffMultiplier("{{error.backOffMultiplier}}")
            .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("direct-vm:replication")
            .routeId("CdrReplicationRoute")
            .log(LoggingLevel.INFO, "Calling replication route for ${headers[org.fcrepo.jms.identifier]}")
            .log(LoggingLevel.INFO, "Replication completed for ${headers[org.fcrepo.jms.identifier]}")
            .to("direct:file.replication");

        from("direct:file.replication")
            .bean(replicationProcessor);
    }
}
