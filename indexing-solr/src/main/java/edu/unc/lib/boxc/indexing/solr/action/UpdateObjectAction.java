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
package edu.unc.lib.boxc.indexing.solr.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;

/**
 * Updates or adds the metadata for a single object
 *
 * @author bbpennel
 *
 */
public class UpdateObjectAction extends AbstractIndexingAction {
    final Logger log = LoggerFactory.getLogger(UpdateObjectAction.class);

    @Override
    public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
        log.debug("Indexing object {}", updateRequest.getPid());
        // Retrieve object metadata from Fedora and add to update document list
        DocumentIndexingPackage dip = updateRequest.getDocumentIndexingPackage();
        if (dip == null) {
            dip = factory.createDip(updateRequest.getPid());
            updateRequest.setDocumentIndexingPackage(dip);
        }

        pipeline.process(dip);
        if (this.addDocumentMode) {
            solrUpdateDriver.addDocument(dip.getDocument());
        } else {
            solrUpdateDriver.updateDocument(dip.getDocument());
        }

    }
}
