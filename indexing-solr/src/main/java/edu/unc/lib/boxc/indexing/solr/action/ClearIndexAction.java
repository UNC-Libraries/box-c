package edu.unc.lib.boxc.indexing.solr.action;

import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;

/**
 * Deletes all records from the index
 *
 * @author bbpennel
 *
 */
public class ClearIndexAction extends AbstractIndexingAction {

    @Override
    public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
        solrUpdateDriver.deleteByQuery("*:*");
        solrUpdateDriver.commit();
    }
}
