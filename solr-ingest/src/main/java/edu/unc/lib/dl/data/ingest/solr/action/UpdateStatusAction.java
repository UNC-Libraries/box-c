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

import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class UpdateStatusAction extends AbstractIndexingAction {
	private static final Logger log = LoggerFactory.getLogger(UpdateStatusAction.class);
	@Autowired
	private TripleStoreQueryService tripleStoreQueryService;
	
	@SuppressWarnings("unchecked")
	@Override
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
		boolean isFirstInChain = updateRequest.getParent().getDocumentIndexingPackage() == null;
		DocumentIndexingPackage dip;
		if (isFirstInChain) {
			dip = dipFactory.createDocumentIndexingPackageWithRelsExt(
					updateRequest.getPid());
			dip.setParentDocument(updateRequest.getParent().getDocumentIndexingPackage());
		} else {
			dip = new DocumentIndexingPackage();
			dip.getDocument().setId(updateRequest.getPid().getPid());
			dip.setPid(updateRequest.getPid());
			dip.setChildren(tripleStoreQueryService.fetchChildren(updateRequest.getPid()));
		}
		try {
			Object statusObject = this.solrSearchService.getField(updateRequest.getPid().getPid(), SearchFieldKeys.STATUS.name());
			if (statusObject != null) {
				List<String> statusList = (List<String>) statusObject;
				// Clear out previous publication state
				clearExistingPublicationStatus(statusList);
				dip.getDocument().setStatus(statusList);
			}
		} catch (SolrServerException e) {
			throw new IndexingException("Failed to retrieve existing status field for " + updateRequest.getPid(), e);
		}
		updateRequest.setDocumentIndexingPackage(dip);
		if (updateRequest.getParent() != null)
			dip.setParentDocument(updateRequest.getParent().getDocumentIndexingPackage());
		// This needs to be the publication status pipeline
		pipeline.process(dip);
		solrUpdateDriver.updateDocument("set", dip.getDocument());
		
		// If this is the first item in this indexing chain, determine if publication status is blocked by its parent
		if (!isFirstInChain || (isFirstInChain && !dip.getDocument().getStatus().contains("Parent Unpublished"))) {
			List<PID> children = dip.getChildren();
			if (children != null) {
				log.debug("Queueing up " + children.size() + " children for reindexing");
				for (PID child : children) {
					SolrUpdateRequest childRequest = new SolrUpdateRequest(child, IndexingActionType.UPDATE_STATUS,
							solrUpdateService.nextMessageID(), updateRequest);
					solrUpdateService.offer(childRequest);
				}
			}
		}
		
		
//		DocumentIndexingPackage dip = dipFactory.createDocumentIndexingPackageWithRelsExt(
//				updateRequest.getPid());
//		if (updateRequest.getParent() != null)
//			dip.setParentDocument(updateRequest.getParent().getDocumentIndexingPackage());
//		updateRequest.setDocumentIndexingPackage(dip);
//		log.debug("Has rels-ext: " + dip.getRelsExt());
//		// This needs to be the publication status pipeline
//		pipeline.process(dip);
//		solrUpdateDriver.updateDocument("set", dip.getDocument());
//		if (log.isDebugEnabled())
//			log.debug("Has parent " + (dip.getParentDocument() != null) + ", parent is unpublished " + (dip.getDocument().getStatus().contains("Parent Unpublished")));
//		// If this is the first item in this indexing chain, determine if publication status is blocked by its parent
//		if (dip.getParentDocument() != null || (dip.getParentDocument() == null && !dip.getDocument().getStatus().contains("Parent Unpublished"))) {
//			// Continue updating all the children since their inherited status has changed
//			List<PID> children = dip.getChildren();
//			if (children != null) {
//				log.debug("Queueing up " + children.size() + " children for reindexing");
//				for (PID child : children) {
//					SolrUpdateRequest childRequest = new SolrUpdateRequest(child, IndexingActionType.UPDATE_STATUS,
//							solrUpdateService.nextMessageID(), updateRequest);
//					solrUpdateService.offer(childRequest);
//				}
//			}
//		}
	}
	
	private void clearExistingPublicationStatus(List<String> statusList) {
		int index = statusList.indexOf("Published");
		if (index != -1)
			statusList.remove(index);
		else {
			index = statusList.indexOf("Parent Unpublished");
			if (index != -1)
				statusList.remove(index);
			index = statusList.indexOf("Unpublished");
			if (index != -1)
				statusList.remove(index);
		}
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}