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
package edu.unc.lib.boxc.services.camel.solrUpdate;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Sends individual indexing operations from aggregated collections of object identifiers
 *
 * @author bbpennel
 */
public class AggregateUpdateProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(AggregateUpdateProcessor.class);
    private IndexingMessageSender messageSender;
    private IndexingActionType actionType;
    private boolean forceCommit;

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        var idCollection = in.getBody(Collection.class);
        if (idCollection.isEmpty()) {
            return;
        }
        if (forceCommit) {
            // Force commit of any pending solr updates before sending indexing operations
            messageSender.sendIndexingOperation(null, RepositoryPaths.getRootPid(), IndexingActionType.COMMIT);
        }
        for (Object idObj : idCollection) {
            PID pid = PIDs.get(idObj.toString());
            messageSender.sendIndexingOperation(null, pid, actionType);
        }
    }

    public void setIndexingMessageSender(IndexingMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    public void setActionType(IndexingActionType actionType) {
        this.actionType = actionType;
    }

    public void setForceCommit(boolean forceCommit) {
        this.forceCommit = forceCommit;
    }
}
