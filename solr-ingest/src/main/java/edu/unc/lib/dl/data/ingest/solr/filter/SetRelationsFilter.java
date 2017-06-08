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
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;

/**
 * Populates the relations field with pertinent triples from RELS-Ext that are primarily intended for post retrieval
 * purposes.
 *
 * @author bbpennel
 *
 */
public class SetRelationsFilter implements IndexDocumentFilter{
	private static final Logger log = LoggerFactory.getLogger(SetRelationsFilter.class);
	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		log.debug("Applying setRelationsFilter");

		List<String> relations = new ArrayList<String>();
		Map<String, List<String>> triples = dip.getTriples();

		// Retrieve the default web datastream
		String defaultWebData = dip.getDefaultWebData();
		if (defaultWebData != null)
			relations.add(CDRProperty.defaultWebData.getPredicate() + "|" + new PID(defaultWebData).getPid());

		// Retrieve the default web object, from the cached version if possible.
		DocumentIndexingPackage defaultWebObjectPackage = dip.getDefaultWebObject();
		String defaultWebObject = null;
		if (defaultWebObjectPackage != null) {
			defaultWebObject = defaultWebObjectPackage.getPid().getPid();
		} else {
			List<String> defaultWebObjectTriples = triples.get(CDRProperty.defaultWebObject.toString());
			if (defaultWebObjectTriples != null)
				defaultWebObject = defaultWebObjectTriples.get(0);
		}
		if (defaultWebObject != null)
			relations.add(CDRProperty.defaultWebObject.getPredicate() + "|" + (new PID(defaultWebObject)).getPid());

		// Retrieve original content datastream name for items with a main content payload
		List<String> sourceData = triples.get(CDRProperty.sourceData.toString());
		if (sourceData != null)
			relations.add(CDRProperty.sourceData.getPredicate() + "|" + ((new PID(sourceData.get(0)).getPid())));

		// Retrieve and store label
		List<String> label = triples.get(FedoraProperty.label.toString());
		if (label != null) {
			dip.getDocument().setLabel(label.get(0));
		}

		// Retrieve the default sort order for a container if specified
		List<String> defaultSortOrder = triples.get(CDRProperty.sortOrder.toString());
		if (defaultSortOrder != null) {
			String sortOrder = defaultSortOrder.get(0);
			sortOrder = sortOrder.substring(sortOrder.indexOf('#') + 1);
			relations.add(CDRProperty.sortOrder.getPredicate() + "|" + sortOrder);
		}

		// Retrieve and store embargo
		List<String> embargoUntil = triples.get(CDRProperty.embargoUntil.toString());
		if (embargoUntil != null)
			relations.add(CDRProperty.embargoUntil.getPredicate() + "|" + embargoUntil.get(0));

		// Retrieve and store invalid terms
		List<String> invalidTerms = triples.get(CDRProperty.invalidTerm.toString());
		String invalidTermPred = CDRProperty.invalidTerm.getPredicate();
		if (invalidTerms != null) {
			for (String invalidTermTriple : invalidTerms) {
				relations.add(invalidTermPred + "|" + invalidTermTriple);
			}
		}

		dip.getDocument().setRelations(relations);
	}
}
