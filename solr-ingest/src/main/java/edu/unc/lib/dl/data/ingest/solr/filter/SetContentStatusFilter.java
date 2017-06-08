/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.util.FacetConstants;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Model;

/**
 * Sets content related status tags
 *
 * @author bbpennel
 *
 */
public class SetContentStatusFilter implements IndexDocumentFilter{
	private static final Logger log = LoggerFactory.getLogger(SetContentStatusFilter.class);
	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		Map<String, List<String>> triples = dip.getTriples();

		List<String> contentStatus = new ArrayList<String>();
		setContentStatus(dip, triples, contentStatus);

		dip.getDocument().setContentStatus(contentStatus);

		log.debug("Content status for {} set to {}", dip.getPid().getPid(), contentStatus);
	}

	private void setContentStatus(DocumentIndexingPackage dip, Map<String, List<String>> triples, List<String> status)
			throws IndexingException {
		List<String> datastreams = triples.get(FedoraProperty.disseminates.toString());
		if (datastreams != null) {
			String mdDescriptive = dip.getPid().getURI() + "/" + Datastream.MD_DESCRIPTIVE.getName();
			boolean described = datastreams.contains(mdDescriptive);
			if (described)
				status.add(FacetConstants.CONTENT_DESCRIBED);
			else
				status.add(FacetConstants.CONTENT_NOT_DESCRIBED);
		}

		// Valid/Not Valid content according to FITS

		// Vocabulary validation
		if (triples.containsKey(CDRProperty.invalidTerm.toString())) {
			status.add(FacetConstants.INVALID_VOCAB_TERM);
		}

		// If its an aggregate, indicate if it has a default web object
		List<String> contentModels = triples.get(FedoraProperty.hasModel.toString());
		if (contentModels != null && contentModels.contains(Model.AGGREGATE_WORK.toString())) {
			if (triples.containsKey(CDRProperty.defaultWebObject.toString())) {
				status.add(FacetConstants.CONTENT_DEFAULT_OBJECT);
			} else {
				status.add(FacetConstants.CONTENT_NO_DEFAULT_OBJECT);
			}
		} else {
			// Check to see if this object is the DefaultWebObject for its parent
			DocumentIndexingPackage parentDip = dip.getParentDocument();
			String parentsDWO = parentDip.getFirstTriple(CDRProperty.defaultWebObject.toString());
			if (parentsDWO != null) {
				if (dip.getPid().equals(new PID(parentsDWO))) {
					status.add(FacetConstants.CONTENT_IS_DEFAULT_OBJECT);
				}
			}
		}
	}


}
