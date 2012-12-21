package edu.unc.lib.dl.data.ingest.solr.action;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;

public class RecursiveUpdateAction extends RecursiveReindexAction {
	@Override
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
		this.recursiveReindex(updateRequest, false);
	}
}