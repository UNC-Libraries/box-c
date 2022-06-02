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

import edu.unc.lib.boxc.indexing.solr.ChildSetRequest;
import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;

/**
 * Updates each object specified in the update request and all of their children.
 *
 * @author bbpennel
 */
public class UpdateTreeSetAction extends UpdateTreeAction {

    private static final Logger log = LoggerFactory.getLogger(UpdateTreeSetAction.class);

    @Override
    public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
        if (!(updateRequest instanceof ChildSetRequest)) {
            throw new IndexingException("ChildSetRequest required to perform TreeSet update, received "
                    + updateRequest.getClass().getName());
        }
        ChildSetRequest childSetRequest = (ChildSetRequest) updateRequest;
        if (childSetRequest.getChildren() == null || childSetRequest.getChildren().size() == 0) {
            throw new IllegalArgumentException("Update request must specify one or more children for indexing");
        }

        // Index the tree for each pid in the set
        for (PID pid : childSetRequest.getChildren()) {
            RepositoryObject obj = repositoryObjectLoader.getRepositoryObject(pid);
            treeIndexer.index(obj, actionType, updateRequest.getUserID());
        }

        if (log.isDebugEnabled()) {
            log.debug("Finished updating tree of {}.  {} objects updated in {} ms.",
                    updateRequest.getPid(), updateRequest.getChildrenPending(),
                    (System.currentTimeMillis() - updateRequest.getTimeStarted()));
        }
    }
}
