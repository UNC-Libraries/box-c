package edu.unc.lib.boxc.indexing.solr.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;

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
}
