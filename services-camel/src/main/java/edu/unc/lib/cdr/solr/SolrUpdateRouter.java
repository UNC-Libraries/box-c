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

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrSolrUpdateAction;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

import edu.unc.lib.cdr.CdrEventProcessor;
import edu.unc.lib.cdr.SolrUpdateProcessor;
import edu.unc.lib.dl.util.JMSMessageUtil.CDRActions;

/**
 *
 * @author lfarrell
 *
 */
public class SolrUpdateRouter extends RouteBuilder {
    @BeanInject(value = "cdrEventProcessor")
    private CdrEventProcessor cdrEventProcessor;

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
            .bean(cdrEventProcessor)
            .filter(simple("${headers[" + CdrSolrUpdateAction + "} contains '" + CDRActions.MOVE + "'"
                    + " || ${headers[" + CdrSolrUpdateAction + "]} contains '" + CDRActions.REMOVE + "'"
                    + " || ${headers[" + CdrSolrUpdateAction + "]} contains '" + CDRActions.ADD + "'"
                    + " || ${headers[" + CdrSolrUpdateAction + "]} contains '" + CDRActions.REORDER + "'"
                    + " || ${headers[" + CdrSolrUpdateAction + "]} contains '" + CDRActions.PUBLISH + "'"
                    + " || ${headers[" + CdrSolrUpdateAction + "]} contains '" + CDRActions.REINDEX + "'"
                    + " || ${headers[" + CdrSolrUpdateAction + "]} contains '" + CDRActions.INDEX + "'"
                    + " || ${headers[" + CdrSolrUpdateAction + "]} contains '" + CDRActions.EDIT_TYPE + "'"))
            .to("direct:solr-update");

        from("direct:solr-update")
            .routeId("CdrServiceSolrUpdateProcess")
            .log(LoggingLevel.DEBUG, "Updating solr index for ${headers[org.fcrepo.jms.identifier]}")
            .bean(solrUpdateProcessor);
    }
}
