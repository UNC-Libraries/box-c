package edu.unc.lib.dl.data.ingest.solr.action;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.CountDownUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.DeleteChildrenPriorToTimestampRequest;
import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateAction;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.UpdateNodeRequest;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;

public class RecursiveReindexAction extends AbstractIndexingAction {
	private static final Logger LOG = LoggerFactory.getLogger(RecursiveReindexAction.class);
	
	@Override
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
		this.recursiveReindex(updateRequest, true);
		
	}
	
	/**
	 * Reindexes the pid of request. If it was a container, than a recursive add is issued for all of its children,
	 * followed by a commit and then a request to delete all children of pid which were not updated by the recursive add,
	 * indicating that they are no longer linked to pid.
	 * 
	 * @param updateRequest
	 */
	protected void recursiveReindex(SolrUpdateRequest updateRequest, boolean cleanupOutdated) {
		try {
			boolean targetAll = false;
			if (TARGET_ALL.equals(updateRequest.getTargetID())) {
				updateRequest.setPid(collectionsPid.getPid());
				targetAll = true;
			} else if (collectionsPid.getPid().equals(updateRequest.getTargetID())) {
				targetAll = true;
			}

			long startTime = System.currentTimeMillis();
			DocumentIndexingPackage dip = dipFactory.createDocumentIndexingPackage(
					updateRequest.getPid());

			if (dip != null) {
				// Store the DIP for this document in the request.
				updateRequest.setDocumentIndexingPackage(dip);
				// Retrieve the parent requests document indexing package and store it as the current dip's parent.
				UpdateNodeRequest parentRequest = updateRequest.getParent();
				if (parentRequest != null) {
					dip.setParentDocument(parentRequest.getDocumentIndexingPackage());
				}

				// Skip indexing this document if it was a reindex all flag
				//if (!targetAll) {
					// Perform the indexing pipeline
					pipeline.process(dip);
					solrUpdateDriver.addDocument(dip.getDocument());
				/*} else {
					dip.getDocument().setAncestorNames("/Collections");
					dip.getDocument().setAncestorPath(new ArrayList<String>());
				}*/

				List<PID> children = dip.getChildren();
				if (children != null) {
					CountDownUpdateRequest cleanupRequest = null;
					CountDownUpdateRequest commitRequest = null;

					if (cleanupOutdated) {
						// Generate cleanup request before offering children to be
						// processed. Set start time to minus one so that the search is less than (instead of <=)
						cleanupRequest = new DeleteChildrenPriorToTimestampRequest(updateRequest.getTargetID(),
								SolrUpdateAction.DELETE_CHILDREN_PRIOR_TO_TIMESTAMP, solrUpdateService.nextMessageID(),
								updateRequest, startTime - 1);

						commitRequest = new CountDownUpdateRequest(updateRequest.getTargetID(), SolrUpdateAction.COMMIT,
								cleanupRequest, solrUpdateService.nextMessageID(), updateRequest);

						LOG.debug("CleanupRequest: " + cleanupRequest.toString());
					}

					// Get all children, set to block the cleanup request
					for (PID child : children) {
						SolrUpdateRequest childRequest = new SolrUpdateRequest(child, SolrUpdateAction.RECURSIVE_ADD,
								commitRequest, solrUpdateService.nextMessageID(), updateRequest);
						LOG.debug("Queueing for recursive reindex: " + child.getPid() + "|" + childRequest.getTargetID());
						solrUpdateService.offer(childRequest);
					}

					if (cleanupOutdated) {
						solrUpdateService.offer(commitRequest);
						// Add the cleanup request
						solrUpdateService.offer(cleanupRequest);
					}
				}
			}
		} catch (Exception e) {
			throw new IndexingException("Error while performing a recursive add on " + updateRequest.getTargetID(), e);
		}
	}
}
