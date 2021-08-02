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
package edu.unc.lib.boxc.operations.jms.indexing;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.model.api.ids.PID;
import io.dropwizard.metrics5.Timer;

/**
 * Service that manages reindexing of the repository
 *
 * @author harring
 *
 */
public class IndexingService {
    private AccessControlService aclService;
    private IndexingMessageSender indexingMessageSender;

    private static final Timer timer = TimerFactory.createTimerForClass(IndexingService.class);

    /**
     * Performs an in-place recursive reindexing of an object and its descendants
     * or a clean reindexing, as indicated by the request
     *
     * @param agent security principals of the agent making request
     * @param objPid the PID where reindexing should begin in the tree
     * @param inplace whether in-place reindexing has been requested
     */
    public void reindexObjectAndChildren(AgentPrincipals agent, PID objectPid, Boolean inplace) {
        try (Timer.Context context = timer.time()) {
            aclService.assertHasAccess("User does not have permission to reindex", objectPid, agent.getPrincipals(),
                    Permission.reindex);
            if (inplace == null || inplace) {
                // Add message to indexing queue
                indexingMessageSender.sendIndexingOperation(agent.getUsername(), objectPid,
                        IndexingActionType.RECURSIVE_REINDEX);
            } else {
                // Add message to indexing queue
                indexingMessageSender.sendIndexingOperation(agent.getUsername(), objectPid,
                        IndexingActionType.CLEAN_REINDEX);
            }
        }
    }

    /**
     * Performs reindexing of a single object
     *
     * @param agent security principals of the agent making request
     * @param objectPid the PID of the object to be reindexed
     */
    public void reindexObject(AgentPrincipals agent, PID objectPid) {
        try (Timer.Context context = timer.time()) {
            aclService.assertHasAccess("User does not have permission to reindex", objectPid, agent.getPrincipals(),
                    Permission.reindex);
            // Add message to indexing queue
            indexingMessageSender.sendIndexingOperation(agent.getUsername(), objectPid,
                    IndexingActionType.ADD);
        }
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setIndexingMessageSender(IndexingMessageSender indexingMessageSender) {
        this.indexingMessageSender = indexingMessageSender;
    }
}
