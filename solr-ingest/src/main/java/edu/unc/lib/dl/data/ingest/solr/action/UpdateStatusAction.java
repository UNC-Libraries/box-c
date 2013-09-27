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
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class UpdateStatusAction extends AbstractIndexingAction {
	private static final Logger log = LoggerFactory.getLogger(UpdateStatusAction.class);
	@Autowired
	private TripleStoreQueryService tripleStoreQueryService;

	@Override
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
		log.debug("UpdateStatusAction for " + updateRequest.getPid());
		boolean isFirstInChain = updateRequest.getParent().getDocumentIndexingPackage() == null;
		
		DocumentIndexingPackage dip = new DocumentIndexingPackage();
		dip.getDocument().setId(updateRequest.getPid().getPid());
		dip.setPid(updateRequest.getPid());
		dip.setTriples(tripleStoreQueryService.fetchAllTriples(updateRequest.getPid()));

		updateRequest.setDocumentIndexingPackage(dip);
		if (updateRequest.getParent() != null)
			dip.setParentDocument(updateRequest.getParent().getDocumentIndexingPackage());
		// This needs to be the publication status pipeline
		pipeline.process(dip);
		solrUpdateDriver.updateDocument("set", dip.getDocument());

		// Reindex children
		if (!updateRequest.getUpdateAction().equals(IndexingActionType.UPDATE_STATUS) ||
		// For status updates, no need to reindex children if the parent was already not published
				!isFirstInChain || (isFirstInChain && !dip.getDocument().getStatus().contains("Parent Unpublished"))) {
			List<PID> children = dip.getChildren();
			if (children != null) {
				log.debug("Queueing up " + children.size() + " children for reindexing");
				for (PID child : children) {
					SolrUpdateRequest childRequest = new SolrUpdateRequest(child, updateRequest.getUpdateAction(),
							solrUpdateService.nextMessageID(), updateRequest);
					solrUpdateService.offer(childRequest);
				}
			}
		}
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}