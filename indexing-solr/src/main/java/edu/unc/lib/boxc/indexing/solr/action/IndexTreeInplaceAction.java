package edu.unc.lib.boxc.indexing.solr.action;

import static edu.unc.lib.boxc.indexing.solr.action.DeleteStaleChildren.STALE_TIMESTAMP;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.DELETE_CHILDREN_PRIOR_TO_TIMESTAMP;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.common.util.DateTimeUtil;
import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;

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
