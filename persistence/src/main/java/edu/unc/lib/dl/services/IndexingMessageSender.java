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
package edu.unc.lib.dl.services;

import static edu.unc.lib.dl.util.IndexingMessageHelper.makeIndexingOperationBody;

import java.util.Collection;

import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 * Constructs and sends JMS messages describing CDR operations related to reindexing
 *
 * @author harring
 *
 */
public class IndexingMessageSender extends AbstractMessageSender {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingMessageSender.class);

    /**
     * Adds message to JMS queue for object(s) to be reindexed.
     *
     * @param userid id of user who triggered the operation
     * @param targetPid PID of object to be indexed
     * @param actionType type of indexing action to perform
     */
    public void sendIndexingOperation(String userid, PID targetPid, IndexingActionType actionType) {
        sendIndexingOperation(userid, targetPid, null, actionType);
    }

    /**
     * Adds message to JMS queue for object(s) to be reindexed.
     *
     * @param userid id of user who triggered the operation
     * @param targetPid PID of object to be indexed
     * @param children pids of other objects to be indexed
     * @param actionType type of indexing action to perform
     */
    public void sendIndexingOperation(String userid, PID targetPid, Collection<PID> children,
            IndexingActionType actionType) {
        Document msg = makeIndexingOperationBody(userid, targetPid, children, actionType);

        LOG.debug("sending solr update message for {} of type {}", targetPid, actionType.toString());
        sendMessage(msg);
        LOG.debug("sent indexing operation JMS message using JMS template: {}", this.getJmsTemplate());
    }
}
