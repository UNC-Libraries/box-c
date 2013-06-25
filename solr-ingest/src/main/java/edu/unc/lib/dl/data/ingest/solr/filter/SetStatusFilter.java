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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DateTimeUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Sets access control and content related status tags
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
		Map<String, List<String>> triples;
		if (dip.getFoxml() == null) {
			triples = tsqs.fetchAllTriples(dip.getPid());
		} else {
			triples = dip.getTriples();
		}
		List<String> status = new ArrayList<String>();
		setAccessStatus(triples, status);
		setPublicationStatus(dip, triples, status);
		List<String> contentStatus = new ArrayList<String>();
		setContentStatus(dip, triples, contentStatus);

		dip.getDocument().setStatus(status);
		dip.getDocument().setContentStatus(contentStatus);
	}

	private void setContentStatus(DocumentIndexingPackage dip, Map<String, List<String>> triples, List<String> status) {
		List<String> datastreams = triples.get(ContentModelHelper.FedoraProperty.disseminates.toString());
		if (datastreams != null) {
			String mdDescriptive = dip.getPid().getURI() + "/" + ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName();
			boolean described = datastreams.contains(mdDescriptive);
			if (described)
				status.add("Described");
			else status.add("Not Described");
		}
		
		// Valid/Not Valid content according to FITS
		
		// If its an aggregate, indicate if it has a default web object
		List<String> contentModels = triples.get(ContentModelHelper.FedoraProperty.hasModel.toString());
		if (contentModels != null && contentModels.contains(ContentModelHelper.Model.AGGREGATE_WORK.toString())) {
			if (triples.containsKey(ContentModelHelper.CDRProperty.defaultWebObject.toString())) {
				status.add("Default Access Object Assigned");
			} else {
				status.add("No Default Access Object");
			}
		}
	}

	private void setAccessStatus(Map<String, List<String>> triples, List<String> status) {
		String inheritPermissions = getFirstValue(triples, ContentModelHelper.CDRProperty.inheritPermissions.toString());
		if ("false".equals(inheritPermissions)) {
			status.add("Not Inheriting Roles");
		}

		String embargo = getFirstValue(triples, ContentModelHelper.CDRProperty.embargoUntil.toString());
		if (embargo != null) {
			try {
				Date embargoDate = DateTimeUtil.parsePartialUTCToDate(embargo);
				Date currentDate = new Date();
				if (currentDate.before(embargoDate))
					status.add("Embargoed");
			} catch (ParseException e) {
				log.warn("Failed to parse embargo date " + embargo, e);
			}
		}

		String discoverable = getFirstValue(triples, ContentModelHelper.CDRProperty.allowIndexing.toString());
		if (!"yes".equals(discoverable)) {
			status.add("Not Discoverable");
		}

		for (Entry<String, List<String>> tripleEntry : triples.entrySet()) {
			int index = tripleEntry.getKey().indexOf('#');
			if (index > 0) {
				String namespace = tripleEntry.getKey().substring(0, index + 1);
				if (JDOMNamespaceUtil.CDR_ROLE_NS.getURI().equals(namespace)) {
					status.add("Roles Assigned");
					break;
				}
			}
		}
	}

	private void setPublicationStatus(DocumentIndexingPackage dip, Map<String, List<String>> triples, List<String> status) {
		// Published by default unless overridden by this item or its parents.
		boolean isPublished = true;
		boolean parentIsPublished = true;

		// Determine the publication status of this item from its nearest cached parent
		if (dip.getParentDocument() != null && dip.getParentDocument().getIsPublished() != null) {
			// No data requests to mulgara or fedora this route.
			Boolean parentIsPublishedObj = dip.getParentDocument().getIsPublished();
			parentIsPublished = (parentIsPublishedObj == null || parentIsPublishedObj);
			String isPublishedString = getFirstValue(triples, ContentModelHelper.CDRProperty.isPublished.toString());
			isPublished = !("no".equals(isPublishedString));
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

			String pidString = dip.getPid().getURI().toString();
			// Scan the results for any nodes that are not published
			for (List<String> row : results) {
				if (pidString.equals(row.get(0))) {
					if ("no".equals(row.get(1))) {
						isPublished = false;
					}
				} else {
					if ("no".equals(row.get(1))) {
						parentIsPublished = false;
					}
				}
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
	}
	
	private String getFirstValue(Map<String, List<String>> triples, String property) { 
		List<String> values = triples.get(property);
		if (values == null || values.size() == 0)
			return null;
		return values.get(0);
	}
}
