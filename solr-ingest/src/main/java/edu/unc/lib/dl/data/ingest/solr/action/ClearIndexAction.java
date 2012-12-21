package edu.unc.lib.dl.data.ingest.solr.action;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateAction;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;

public class ClearIndexAction extends AbstractIndexingAction {

	@Override
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
		solrUpdateDriver.deleteByQuery("*:*");
		solrUpdateService.offer(new SolrUpdateRequest(updateRequest.getTargetID(), SolrUpdateAction.COMMIT,
				solrUpdateService.nextMessageID(), updateRequest));
	}

}
