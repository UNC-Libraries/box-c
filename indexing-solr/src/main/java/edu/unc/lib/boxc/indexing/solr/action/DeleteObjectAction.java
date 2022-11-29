package edu.unc.lib.boxc.indexing.solr.action;

import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;

/**
 * 
 * @author bbpennel
 *
 */
public class DeleteObjectAction extends AbstractIndexingAction {

    @Override
    public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
        solrUpdateDriver.delete(updateRequest.getTargetID());
    }

}
