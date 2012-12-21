package edu.unc.lib.dl.data.ingest.solr.action;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;

public class DeleteObjectAction extends AbstractIndexingAction {

	@Override
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
		solrUpdateDriver.delete(updateRequest.getTargetID());
	}

}
