/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.data.ingest.solr.action;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.BlockUntilTargetCompleteRequest;
import edu.unc.lib.dl.data.ingest.solr.DeleteChildrenPriorToTimestampRequest;
import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.UpdateNodeRequest;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;

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
			if (TARGET_ALL.equals(updateRequest.getTargetID())) {
				updateRequest.setPid(collectionsPid.getPid());
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

				// Reindex the target
				pipeline.process(dip);
				solrUpdateDriver.addDocument(dip.getDocument());

				List<PID> children = dip.getChildren();
				if (children != null) {
					
					BlockUntilTargetCompleteRequest cleanupRequest = null;
					BlockUntilTargetCompleteRequest commitRequest = null;

					if (cleanupOutdated) {
						commitRequest = new BlockUntilTargetCompleteRequest(updateRequest.getTargetID(), IndexingActionType.COMMIT,
								solrUpdateService.nextMessageID(), null, updateRequest);
						
						// Generate cleanup request before offering children to be
						// processed. Set start time to minus one so that the search is less than (instead of <=)
						cleanupRequest = new DeleteChildrenPriorToTimestampRequest(updateRequest.getTargetID(),
								IndexingActionType.DELETE_CHILDREN_PRIOR_TO_TIMESTAMP, solrUpdateService.nextMessageID(),
								commitRequest, updateRequest, startTime - 1);

						LOG.debug("CleanupRequest: " + cleanupRequest.toString());
					}

					// Get all children, set to block the cleanup request
					for (PID child : children) {
						SolrUpdateRequest childRequest = new SolrUpdateRequest(child, IndexingActionType.RECURSIVE_ADD,
								solrUpdateService.nextMessageID(), updateRequest);
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
