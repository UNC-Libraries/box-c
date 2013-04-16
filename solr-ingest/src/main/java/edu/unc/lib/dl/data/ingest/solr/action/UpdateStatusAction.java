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

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;

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
					SolrUpdateRequest childRequest = new SolrUpdateRequest(child, IndexingActionType.UPDATE_STATUS,
							solrUpdateService.nextMessageID(), updateRequest);
					solrUpdateService.offer(childRequest);
				}
			}
		}
	}
}