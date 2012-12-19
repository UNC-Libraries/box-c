package edu.unc.lib.dl.data.ingest.solr.action;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;

public interface IndexingAction {
	/**
	 * Performs an indexing action based on the update request received
	 * 
	 * @param updateRequest
	 * @throws IndexingException
	 */
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException;
}
