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

import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;

/**
 * Indexing filter which extracts Fedora generated dates about the creation and modification state of the object
 * being indexed.
 * 
 * Sets: dateAdded, dateUpdated
 * @author bbpennel
 *
 */
public class SetRecordDatesFilter extends AbstractIndexDocumentFilter {
	private static final Logger log = LoggerFactory.getLogger(SetRecordDatesFilter.class);
	
	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		String dateAdded = dip.getFirstTriple(FedoraProperty.createdDate.toString());
		String dateUpdated = dip.getFirstTriple(FedoraProperty.lastModifiedDate.toString());
		
		if (dateAdded == null || dateUpdated == null) {
			// Force load of FOXML in the few cases where triple store has failed to index required date fields
			log.warn("Failed to load date field for {}, falling back to FOXML as data source", dip.getPid());
			dip.setTriples(null);
			dip.getFoxml();
			dateAdded = dip.getFirstTriple(FedoraProperty.createdDate.toString());
			dateUpdated = dip.getFirstTriple(FedoraProperty.lastModifiedDate.toString());
		}
		
		try {
			dip.getDocument().setDateAdded(dateAdded);
		} catch (ParseException e) {
			throw new IndexingException("Failed to parse record dates from " + dip.getPid().getPid(), e);
		}
		
		try {
			dip.getDocument().setDateUpdated(dateUpdated);
		} catch (ParseException e) {
			throw new IndexingException("Failed to parse record dates from " + dip.getPid().getPid(), e);
		}
	}
}
