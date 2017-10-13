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
package edu.unc.lib.dl.data.ingest.solr.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 * Action which clears index records and then regenerates them for the specified
 * object and all of its children.
 *
 * @author bbpennel
 *
 */
public class IndexTreeCleanAction extends UpdateTreeAction {
    private static final Logger log = LoggerFactory.getLogger(IndexTreeCleanAction.class);

    private DeleteSolrTreeAction deleteAction;

    public IndexTreeCleanAction() {
        // Clean index doesn't make sense with update mode
        addDocumentMode = true;
    }

    @Override
    public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
        log.debug("Starting clean indexing of {}", updateRequest.getPid());

        SolrUpdateRequest deleteRequest = new SolrUpdateRequest(updateRequest.getPid().getRepositoryPath(),
                IndexingActionType.DELETE_SOLR_TREE);
        deleteAction.performAction(deleteRequest);

        // Force commit to ensure delete finishes before we start repopulating
        solrUpdateDriver.commit();

        // Perform normal recursive update
        super.performAction(updateRequest);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Finished clean indexing of {}.  {} objects updated in {}ms",
                    updateRequest.getPid().getRepositoryPath(), updateRequest.getChildrenPending(),
                    System.currentTimeMillis() - updateRequest.getTimeStarted()));
        }
    }

    public void setDeleteAction(DeleteSolrTreeAction deleteAction) {
        this.deleteAction = deleteAction;
    }

    @Override
    public void setAddDocumentMode(boolean addDocumentMode) {
        // Do nothing, clean index should always be in add mode
    }
}
