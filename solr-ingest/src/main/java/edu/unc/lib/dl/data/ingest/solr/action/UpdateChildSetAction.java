package edu.unc.lib.dl.data.ingest.solr.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.ChildSetRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;

/**
 * Performs indexing of a specific set of children sharing a single parent. Does not index the parent itself
 * 
 * @author bbpennel
 * 
 */
public class UpdateChildSetAction extends UpdateTreeAction {
	private static final Logger log = LoggerFactory.getLogger(UpdateChildSetAction.class);
	
	public UpdateChildSetAction() {
		this.addDocumentMode = true;
	}

	@Override
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
		if (!(updateRequest instanceof ChildSetRequest))
			throw new IndexingException("ChildSetRequest required to perform move objects update, received "
					+ updateRequest.getClass().getName());
		ChildSetRequest childSetRequest = (ChildSetRequest) updateRequest;

		log.debug("Starting update tree of {}", updateRequest.getPid().getPid());

		// Generate the DIP for the new destination, but don't index it
		DocumentIndexingPackage dip = getParentDIP(childSetRequest);

		// Calculate total number of objects to be indexed
		int indexTargetTotal = 0;
		for (PID child : childSetRequest.getChildren()) {
			indexTargetTotal += this.countDescendants(child) + 1;
		}
		updateRequest.setChildrenPending(indexTargetTotal);

		// Index all the newly moved children
		RecursiveTreeIndexer treeIndexer = new RecursiveTreeIndexer(updateRequest, this, false);
		treeIndexer.indexChildren(dip, childSetRequest.getChildren());

		if (log.isDebugEnabled())
			log.debug("Finished updating tree of " + updateRequest.getPid().getPid() + ".  "
					+ updateRequest.getChildrenPending() + " objects updated in "
					+ (System.currentTimeMillis() - updateRequest.getTimeStarted()) + " ms");
	}

	protected DocumentIndexingPackage getParentDIP(ChildSetRequest childSetRequest) {
		return dipFactory.createDocumentIndexingPackage(childSetRequest.getPid());
	}

	public DocumentIndexingPackage getDocumentIndexingPackage(PID pid, DocumentIndexingPackage parent) {
		return dipFactory.createDocumentIndexingPackage(pid);
	}
}
