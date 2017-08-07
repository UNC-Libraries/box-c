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

package edu.unc.lib.cdr.solr;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import edu.unc.lib.cdr.SolrUpdateProcessor;

/**
 *
 * @author lfarrell
 *
 */
public class SolrUpdateRouter extends RouteBuilder {
    @BeanInject(value = "solrUpdateProcessor")
    private SolrUpdateProcessor solrUpdateProcessor;

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
            .redeliveryDelay("{{error.retryDelay}}")
            .maximumRedeliveries("{{error.maxRedeliveries}}")
            .backOffMultiplier(2)
            .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("{{cdr.stream}}")
            .routeId("CdrServiceSolrUpdate")
            .log(LoggingLevel.DEBUG, "Calling solr index update route for ${headers[org.fcrepo.jms.identifier]}")
            .bean(solrUpdateProcessor);

           /* .choice()
                .when(simple("${headers[edu.unc.lib.cdr.identifier]} contains '" + CDRActions.MOVE + "'"))
                    .recipientList()
                    .header("myHeader")
                .when(simple("${headers[edu.unc.lib.cdr.jms.identifier]} contains '" + CDRActions.REMOVE + "'"))
                    .to("direct:process.enhancements")
                .when(simple("${headers[edu.unc.lib.cdr.jms.identifier]} contains '" + CDRActions.ADD + "'"))
                    .to("direct:process.enhancements")
                .when(simple("${headers[edu.unc.lib.cdr.jms.identifier]} contains '" + CDRActions.REORDER + "'"))
                    .to("direct:process.enhancements")
                .when(simple("${headers[edu.unc.lib.cdr.jms.identifier]} contains '" + CDRActions.PUBLISH + "'"))
                    .to("direct:process.enhancements")
                .when(simple("${headers[edu.unc.lib.cdr.jms.identifier]} contains '" + CDRActions.REINDEX + "'"))
                    .to("direct:process.enhancements")
                .when(simple("${headers[edu.unc.lib.cdr.jms.identifier]} contains '" + CDRActions.INDEX + "'"))
                    .to("direct:process.enhancements")
                .when(simple("${headers[edu.unc.lib.cdr.jms.identifier]} contains '" + CDRActions.EDIT_TYPE + "'"))
                    .to("direct:process.enhancements")
                .otherwise()
                    .log(LoggingLevel.WARN,
                            "Cannot update Solr index for ${headers[org.fcrepo.jms.identifier]}")
            .end(); */
    }
}
