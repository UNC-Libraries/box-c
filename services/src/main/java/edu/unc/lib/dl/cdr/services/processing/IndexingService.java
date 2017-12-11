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
package edu.unc.lib.dl.cdr.services.processing;

import java.util.Arrays;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.fcrepo4.GlobalPermissionEvaluator;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.util.IndexingActionType;

public class IndexingService {
    private GlobalPermissionEvaluator globalPermissionEvaluator;
    private OperationsMessageSender operationsMessageSender;

    public void reindexObjectAndChildren(AgentPrincipals agent, PID objectPid, Boolean inplace) {
        if (globalPermissionEvaluator.hasGlobalPermission(agent.getGroups(), Permission.reindex)) {
            if (inplace == null || inplace) {
                operationsMessageSender.sendIndexingOperation(GroupsThreadStore.getUsername(), Arrays.asList(objectPid),
                        IndexingActionType.RECURSIVE_REINDEX);
            } else {
                operationsMessageSender.sendIndexingOperation(GroupsThreadStore.getUsername(), Arrays.asList(objectPid),
                        IndexingActionType.CLEAN_REINDEX);
            }
        } else {
            throw new AccessRestrictionException("User does not have permission to reindex");
        }
    }

    public void reindexObject(AgentPrincipals agent, PID objectPid) {
        if (globalPermissionEvaluator.hasGlobalPermission(agent.getGroups(), Permission.reindex)) {
            operationsMessageSender.sendIndexingOperation(GroupsThreadStore.getUsername(),
                    Arrays.asList(objectPid), IndexingActionType.ADD);
        } else {
            throw new AccessRestrictionException("User does not have permission to reindex");
        }

    }

    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }

    public void setGlobalPermissionEvaluator(GlobalPermissionEvaluator globalPermissionEvaluator) {
        this.globalPermissionEvaluator = globalPermissionEvaluator;
    }

}
