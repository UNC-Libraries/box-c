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
package edu.unc.lib.dl.data.ingest.solr.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Sets the publication status, taking into account the publication status of its parents Published, Unpublished,
 * Parent Unpublished
 * 
 * @author bbpennel
 * 
 */
public class SetStatusFilter extends AbstractIndexDocumentFilter {
	private static final Logger log = LoggerFactory.getLogger(SetStatusFilter.class);
	
	private String isPublishedQuery;

	public SetStatusFilter() {
		try {
			this.isPublishedQuery = this.readFileAsString("isPublished.itql");
		} catch (IOException e) {
			log.error("Unable to find query file", e);
		}
	}
	
	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		Element relsExt = dip.getRelsExt();
		
		List<String> status = new ArrayList<String>();
		
		// Published by default unless overridden by this item or its parents.
		boolean isPublished = true;
		boolean parentIsPublished = true;
		// Determine the publication status of this item from its nearest cached parent and its own RELS-EXT, if both are available
		if (relsExt != null && dip.getParentDocument() != null && dip.getParentDocument().getIsPublished() != null) {
			// No data requests to mulgara or fedora this route.
			Boolean parentIsPublishedObj = dip.getParentDocument().getIsPublished();
			//isPublished = (parentIsPublished == null || !parentIsPublished);
			parentIsPublished = (parentIsPublishedObj == null || parentIsPublishedObj);
			// Check to see if the item itself is published or not.
			String thisIsPublished = relsExt.getChildText(ContentModelHelper.CDRProperty.isPublished.name(), JDOMNamespaceUtil.CDR_NS);
			isPublished = !("no".equals(thisIsPublished));
		} else {
			log.debug("Retrieving publication status for " + dip.getPid().getPid() + " from triple store.");
			// Triple store route, query for the publication status of this item and all its parents
			String query = String.format(isPublishedQuery, tsqs.getResourceIndexModelUri(), dip.getPid().getURI());
			List<List<String>> results = tsqs.queryResourceIndex(query);
			// Abandon ship if we couldn't get a path for this object.
			if (results.size() == 0) {
				throw new IndexingException("Object " + dip.getPid() + " could not be found");
			}
			
			if (log.isDebugEnabled()) {
				log.debug("Publication query results: " + results);
			}
			
			boolean selfOnly = true;
			String pidString = dip.getPid().getURI().toString();
			// Scan the results for any nodes that are not published
			for (List<String> row : results) {
				if (pidString.equals(row.get(0))) {
					if ("no".equals(row.get(1))) {
						isPublished = false;
					}
				} else {
					selfOnly = false;
					if ("no".equals(row.get(1))) {
						parentIsPublished = false;
					}
				}
			}
			if (selfOnly) {
				throw new IndexingException("Object " + dip.getPid().getPid() + " is orphaned.");
			}
		}
		
		// Set the publication status based on this items status and that of its parents.
		if (parentIsPublished) {
			// If the parent is publish, publication status is completely up to the item being processed.
			if (isPublished) {
				status.add("Published");
			} else {
				status.add("Unpublished");
			}
		} else {
			// If the parent is unpublished, then this item is unpublished. 
			status.add("Parent Unpublished");
			// Store that this item specifically is unpublished if it is explicitly unpublished
			if (!isPublished) {
				status.add("Unpublished");
			}
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Parent is published: " + parentIsPublished);
			log.debug("Item is published: " + isPublished);
			log.debug("Final Status: " + status);
		}
		
		
		dip.setIsPublished(parentIsPublished && isPublished);
		dip.getDocument().setStatus(status);
	}
}
