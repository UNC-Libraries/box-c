package edu.unc.lib.dl.data.ingest.solr.action;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;

public class UpdateObjectAction extends AbstractIndexingAction {

	@Override
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
		// Retrieve object metadata from Fedora and add to update document list
		DocumentIndexingPackage dip = dipFactory.createDocumentIndexingPackage(
				updateRequest.getPid());
		if (dip != null) {
			updateRequest.setDocumentIndexingPackage(dip);
			pipeline.process(dip);
			solrUpdateDriver.addDocument(dip.getDocument());
		}
	}

}
