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
package edu.unc.lib.boxc.indexing.solr.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;

/**
 * Updates an object and all of its descendants using the pipeline provided. No
 * cleanup is performed on any of the updated objects.
 *
 * @author bbpennel
 *
 */
public class UpdateTreeAction extends AbstractIndexingAction {
    private static final Logger log = LoggerFactory.getLogger(UpdateTreeAction.class);

    protected RepositoryObjectLoader repositoryObjectLoader;

    protected IndexingActionType actionType;

    protected RecursiveTreeIndexer treeIndexer;

    @Override
    public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
        log.debug("Queuing update tree of {}", updateRequest.getPid());

        // Perform updates
        index(updateRequest);

        log.debug("Finished queuing update of tree for {}.", updateRequest.getPid());
    }

    protected void index(SolrUpdateRequest updateRequest) throws IndexingException {
        PID startingPid = updateRequest.getPid();

        // Start indexing
        RepositoryObject startingObj = repositoryObjectLoader.getRepositoryObject(startingPid);
        treeIndexer.index(startingObj, actionType, updateRequest.getUserID());
    }

    /**
     * @param repositoryObjectLoader the repositoryObjectLoader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    /**
     * @param treeIndexer the treeIndexer to set
     */
    public void setTreeIndexer(RecursiveTreeIndexer treeIndexer) {
        this.treeIndexer = treeIndexer;
    }

    public void setActionType(String actionName) {
        actionType = IndexingActionType.valueOf(actionName);
        Assert.notNull(actionType, "Invalid indexing action type requested: " + actionName);
    }
}