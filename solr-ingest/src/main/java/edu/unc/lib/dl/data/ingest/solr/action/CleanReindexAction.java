package edu.unc.lib.dl.data.ingest.solr.action;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateAction;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;

public class CleanReindexAction extends AbstractIndexingAction {

	@Override
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
		SolrUpdateRequest deleteTreeRequest = new SolrUpdateRequest(updateRequest.getTargetID(),
				SolrUpdateAction.DELETE_SOLR_TREE, solrUpdateService.nextMessageID(), updateRequest);
		SolrUpdateRequest commitRequest = new SolrUpdateRequest(updateRequest.getTargetID(), SolrUpdateAction.COMMIT,
				solrUpdateService.nextMessageID(), updateRequest);
		SolrUpdateRequest recursiveAddRequest = new SolrUpdateRequest(updateRequest.getTargetID(),
				SolrUpdateAction.RECURSIVE_ADD, solrUpdateService.nextMessageID(), updateRequest);
		solrUpdateService.offer(deleteTreeRequest);
		solrUpdateService.offer(commitRequest);
		solrUpdateService.offer(recursiveAddRequest);
	}

}
