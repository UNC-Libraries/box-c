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

import static edu.unc.lib.dl.data.ingest.solr.action.DeleteStaleChildren.STALE_TIMESTAMP;
import static edu.unc.lib.dl.util.IndexingActionType.DELETE_CHILDREN_PRIOR_TO_TIMESTAMP;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.services.IndexingMessageSender;
import edu.unc.lib.dl.util.DateTimeUtil;

/**
 * Performs an update of an object and all of its descendants. After they have all updated, any descendants which were
 * not updated (thereby indicating they have been deleted or removed) will be removed from the index.
 *
 * @author bbpennel
 *
 */
public class IndexTreeInplaceAction extends UpdateTreeAction {
    private static final Logger log = LoggerFactory.getLogger(IndexTreeInplaceAction.class);

    private IndexingMessageSender messageSender;

    @Override
    public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
        log.info("Starting inplace indexing of {}", updateRequest.getPid());

        super.performAction(updateRequest);

        deleteStaleChildren(updateRequest);

        log.debug("Finished queueing inplace indexing of {}.",
                updateRequest.getPid().getRepositoryPath());
    }

    public void deleteStaleChildren(SolrUpdateRequest updateRequest) throws IndexingException {
        Map<String, String> params = new HashMap<>();
        long startTime = updateRequest.getTimeStarted();
        Date startDate = new Date(startTime);
        String dateString = DateTimeUtil.formatDateToUTC(startDate);
        params.put(STALE_TIMESTAMP, dateString);

        // Queue cleanup action for objects in tree that were not updated during this operation
        messageSender.sendIndexingOperation(updateRequest.getUserID(), updateRequest.getPid(),
                DELETE_CHILDREN_PRIOR_TO_TIMESTAMP);
    }

    /**
     * @param messageSender the messageSender to set
     */
    public void setIndexingMessageSender(IndexingMessageSender messageSender) {
        this.messageSender = messageSender;
    }
}
