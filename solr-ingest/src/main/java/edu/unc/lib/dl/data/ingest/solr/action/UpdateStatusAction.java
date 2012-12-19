package edu.unc.lib.dl.data.ingest.solr.action;

import java.util.List;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateAction;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;

public class UpdateStatusAction extends AbstractIndexingAction {
	@Override
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
		DocumentIndexingPackage dip = dipFactory.createDocumentIndexingPackageWithRelsExt(
				updateRequest.getPid());
		updateRequest.setDocumentIndexingPackage(dip);
		// This needs to be the publication status pipeline
		pipeline.process(dip);
		solrUpdateDriver.updateDocument("set", dip.getDocument());
		// If this is the first item in this indexing chain, determine if publication status is blocked by its parent
		if (!(updateRequest.getParent() == null && dip.getDocument().getStatus().contains("Parent Unpublished"))) {
			// Continue updating all the children since their inherited status has changed
			List<PID> children = dip.getChildren();
			if (children != null) {
				for (PID child : children) {
					SolrUpdateRequest childRequest = new SolrUpdateRequest(child, SolrUpdateAction.UPDATE_STATUS,
							solrUpdateService.nextMessageID(), updateRequest);
					solrUpdateService.offer(childRequest);
				}
			}
		}
	}
}