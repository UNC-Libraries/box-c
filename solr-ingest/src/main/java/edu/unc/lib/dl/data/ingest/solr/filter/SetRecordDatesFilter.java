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

import java.text.ParseException;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;

/**
 * Indexing filter which extracts Fedora generated dates about the creation and modification state of the object
 * being indexed.
 * 
 * Sets: dateAdded, dateUpdated
 * @author bbpennel
 *
 */
public class SetRecordDatesFilter {
	
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		try {
			dip.getDocument().setDateAdded(dip.getFirstTriple(Fcrepo4Repository.created.toString()));
			dip.getDocument().setDateUpdated(dip.getFirstTriple(Fcrepo4Repository.lastModified.toString()));
		} catch (ParseException e) {
			throw new IndexingException("Failed to parse record dates from " + dip.getPid(), e);
		}
	}
}
