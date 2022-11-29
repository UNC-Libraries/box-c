package edu.unc.lib.boxc.indexing.solr.action;

import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;

/**
 * 
 * @author bbpennel
 *
 */
public interface IndexingAction {
    /**
     * Performs an indexing action based on the update request received
     *
     * @param updateRequest
     * @throws IndexingException
     */
    public void performAction(SolrUpdateRequest updateRequest) throws IndexingException;
}
