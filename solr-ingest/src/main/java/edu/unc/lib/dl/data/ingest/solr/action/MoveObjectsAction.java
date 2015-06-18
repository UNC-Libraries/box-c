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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.ChildSetRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;

/**
 * Updates the path and inherited properties of one or more objects sharing the same parent container
 *
 * @author bbpennel
 *
 */
public class MoveObjectsAction extends UpdateChildSetAction {
	private static final Logger log = LoggerFactory.getLogger(MoveObjectsAction.class);

	public MoveObjectsAction() {
		this.addDocumentMode = false;
	}

	@Override
	protected DocumentIndexingPackage getParentDIP(ChildSetRequest childSetRequest) throws IndexingException {
		// Store the MD_CONTENTS of the parents so new children can be correctly located
		DocumentIndexingPackage dip = dipFactory.createDocumentIndexingPackage(childSetRequest.getPid());
		// Process the parent to get its inheritable properties
		this.pipeline.process(dip);
		return dip;
	}

	@Override
	public DocumentIndexingPackage getDocumentIndexingPackage(PID pid, DocumentIndexingPackage parent)
			throws IndexingException {
		DocumentIndexingPackage dip = new DocumentIndexingPackage(pid);
		dip.setParentDocument(parent);
		// Get all triples in order to retrieve children
		dip.setTriples(tsqs.fetchAllTriples(dip.getPid()));

		// For the top level children that were just moved we need to check for display order
		if (parent.getMdContents() != null) {
			log.debug("Updating display order for top level moved object {}", pid.getPid());
			dip.getDocument().setDisplayOrder(parent.getDisplayOrder(pid.getPid()));
		}
		return dip;
	}
}
