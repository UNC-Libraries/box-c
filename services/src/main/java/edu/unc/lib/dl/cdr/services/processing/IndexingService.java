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
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.action.IndexTreeCleanAction;
import edu.unc.lib.dl.data.ingest.solr.action.IndexTreeInplaceAction;
import edu.unc.lib.dl.data.ingest.solr.action.UpdateObjectAction;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 * Service that manages reindexing of the repository
 *
 * @author harring
 *
 */
public class IndexingService {
    private GlobalPermissionEvaluator globalPermissionEvaluator;
    private OperationsMessageSender operationsMessageSender;
    private UpdateObjectAction updateObjectAction;
    private IndexTreeInplaceAction indexInplaceAction;
    private IndexTreeCleanAction indexCleanAction;

    /**
     * Performs an in-place recursive reindexing of an object and its descendants
     * or a clean reindexing, as indicated by the request
     *
     * @param agent security principals of the agent making request
     * @param objPid the PID where reindexing should begin in the tree
     * @param inplace whether in-place reindexing has been requested
     * @throws IndexingException
     */
    public void reindexObjectAndChildren(AgentPrincipals agent, PID objPid, Boolean inplace)
            throws IndexingException {
        if (globalPermissionEvaluator.hasGlobalPermission(agent.getGroups(), Permission.reindex)) {
            if (inplace == null || inplace) {
                SolrUpdateRequest updateRequest = new SolrUpdateRequest(objPid.getRepositoryPath(),
                        IndexingActionType.RECURSIVE_REINDEX);
                indexInplaceAction.performAction(updateRequest);
                // Send message that the action completed
                operationsMessageSender.sendIndexingOperation(GroupsThreadStore.getUsername(), Arrays.asList(objPid),
                        IndexingActionType.RECURSIVE_REINDEX);
            } else {
                SolrUpdateRequest updateRequest = new SolrUpdateRequest(objPid.getRepositoryPath(),
                        IndexingActionType.CLEAN_REINDEX);
                indexCleanAction.performAction(updateRequest);
                // Send message that the action completed
                operationsMessageSender.sendIndexingOperation(GroupsThreadStore.getUsername(), Arrays.asList(objPid),
                        IndexingActionType.CLEAN_REINDEX);
            }
        } else {
            throw new AccessRestrictionException("User does not have permission to reindex");
        }
    }

    /**
     * Performs reindexing of a single object
     *
     * @param agent security principals of the agent making request
     * @param objectPid the PID of the object to be reindexed
     * @throws IndexingException
     */
    public void reindexObject(AgentPrincipals agent, PID objectPid) throws IndexingException {
        if (globalPermissionEvaluator.hasGlobalPermission(agent.getGroups(), Permission.reindex)) {
            SolrUpdateRequest updateRequest = new SolrUpdateRequest(objectPid.getRepositoryPath(), IndexingActionType.ADD);
            updateObjectAction.performAction(updateRequest);
            // Send message that the action completed
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

    public void setUpdateObjectAction(UpdateObjectAction updateObjectAction) {
        this.updateObjectAction = updateObjectAction;
    }

    public void setIndexInplaceAction(IndexTreeInplaceAction indexInplaceAction) {
        this.indexInplaceAction = indexInplaceAction;
    }

    public void setIndexCleanAction(IndexTreeCleanAction indexCleanAction) {
        this.indexCleanAction = indexCleanAction;
    }

}
