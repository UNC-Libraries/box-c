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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.util.ContentModelHelper;

/**
 * Sets content related status tags
 * 
 * @author bbpennel
 * 
 */
public class SetContentStatusFilter extends AbstractIndexDocumentFilter {
	private static final Logger log = LoggerFactory.getLogger(SetContentStatusFilter.class);

	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		Map<String, List<String>> triples = retrieveTriples(dip);

		List<String> contentStatus = new ArrayList<String>();
		setContentStatus(dip, triples, contentStatus);

		dip.getDocument().setContentStatus(contentStatus);
		
		if (log.isDebugEnabled())
			log.debug("Content status for {} set to {}", dip.getPid().getPid(), contentStatus);
	}

	private void setContentStatus(DocumentIndexingPackage dip, Map<String, List<String>> triples, List<String> status) {
		List<String> datastreams = triples.get(ContentModelHelper.FedoraProperty.disseminates.toString());
		if (datastreams != null) {
			String mdDescriptive = dip.getPid().getURI() + "/" + ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName();
			boolean described = datastreams.contains(mdDescriptive);
			if (described)
				status.add("Described");
			else
				status.add("Not Described");
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


}
