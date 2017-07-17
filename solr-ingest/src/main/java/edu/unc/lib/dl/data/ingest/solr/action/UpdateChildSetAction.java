/**
 * Copyright © 2008 The University of North Carolina at Chapel Hill (cdr@unc.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.data.ingest.solr.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.ChildSetRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;

/**
 * Performs indexing of a specific set of children sharing a single parent. Does not index the parent itself
 *
 * @author bbpennel
 *
 */
public class UpdateChildSetAction extends UpdateTreeAction {
    private static final Logger log = LoggerFactory.getLogger(UpdateChildSetAction.class);

    public UpdateChildSetAction() {
        this.addDocumentMode = false;
    }

    @Override
    public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
        if (!(updateRequest instanceof ChildSetRequest)) {
            throw new IndexingException("ChildSetRequest required to perform move objects update, received "
                    + updateRequest.getClass().getName());
        }
        ChildSetRequest childSetRequest = (ChildSetRequest) updateRequest;

        log.debug("Starting update tree of {}", updateRequest.getPid().getPid());

        // Generate the DIP for the new destination, but don't index it
        DocumentIndexingPackage dip = getParentDIP(childSetRequest);

        // Calculate total number of objects to be indexed
        int indexTargetTotal = 0;
        for (PID child : childSetRequest.getChildren()) {
            indexTargetTotal += this.countDescendants(child) + 1;
        }
        updateRequest.setChildrenPending(indexTargetTotal);

        // Index all the newly moved children
        RecursiveTreeIndexer treeIndexer = new RecursiveTreeIndexer(updateRequest, this, this.addDocumentMode);
        treeIndexer.indexChildren(dip, childSetRequest.getChildren());

        if (log.isDebugEnabled()) {
            log.debug("Finished updating tree of {}.  {} objects updated in {} ms.", new Object[] {
                    updateRequest.getPid().getPid(), updateRequest.getChildrenPending(),
                    (System.currentTimeMillis() - updateRequest.getTimeStarted()) });
        }
    }

    protected DocumentIndexingPackage getParentDIP(ChildSetRequest childSetRequest) throws IndexingException {
        return factory.createDip(childSetRequest.getPid());
    }
}
