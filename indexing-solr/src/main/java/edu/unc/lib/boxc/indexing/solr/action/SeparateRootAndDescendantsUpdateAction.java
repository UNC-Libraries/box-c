package edu.unc.lib.boxc.indexing.solr.action;

import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action which performs indexing of a tree of objects, where the root of the tree and its children
 * are indexed with separate indexing actions.
 *
 * @author bbpennel
 */
public class SeparateRootAndDescendantsUpdateAction extends AbstractIndexingAction {
    private static final Logger log = LoggerFactory.getLogger(SeparateRootAndDescendantsUpdateAction.class);
    private IndexingActionType rootActionType;
    private IndexingActionType descendantsActionType;
    private RecursiveTreeIndexer treeIndexer;
    private IndexingMessageSender messageSender;

    @Override
    public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
        log.debug("Queuing update of root {}", updateRequest.getPid());
        indexRoot(updateRequest);
        log.debug("Queuing update of descendants of {}", updateRequest.getPid());
        indexDescendants(updateRequest);
        log.debug("Finished queuing updates for root and descendants for {}.", updateRequest.getPid());
    }

    private void indexRoot(SolrUpdateRequest updateRequest) throws IndexingException {
        messageSender.sendIndexingOperation(updateRequest.getUserID(), updateRequest.getPid(), rootActionType);
    }

    private void indexDescendants(SolrUpdateRequest updateRequest) throws IndexingException {
        treeIndexer.indexChildren(updateRequest.getPid(), descendantsActionType, updateRequest.getUserID());
    }

    /**
     * @param treeIndexer the treeIndexer to set
     */
    public void setTreeIndexer(RecursiveTreeIndexer treeIndexer) {
        this.treeIndexer = treeIndexer;
    }

    public void setMessageSender(IndexingMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    public void setRootActionType(IndexingActionType rootActionType) {
        this.rootActionType = rootActionType;
    }

    public void setDescendantsActionType(IndexingActionType descendantsActionType) {
        this.descendantsActionType = descendantsActionType;
    }
}
