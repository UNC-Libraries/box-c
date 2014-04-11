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

import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.exception.OrphanedObjectException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * Base class for document indexing filters containing utility methods.
 *
 * @author bbpennel
 * @date Apr 11, 2014
 */
public abstract class AbstractIndexDocumentFilter implements IndexDocumentFilter {
	private static final Logger log = LoggerFactory.getLogger(AbstractIndexDocumentFilter.class);

	protected TripleStoreQueryService tsqs;
	protected DocumentIndexingPackageFactory dipFactory;

	protected String readFileAsString(String filePath) throws java.io.IOException {
		return IOUtils.toString(this.getClass().getResourceAsStream(filePath));
	}

	protected DocumentIndexingPackage retrieveParentDIP(DocumentIndexingPackage dip) throws IndexingException {
		IndexDocumentBean idb = dip.getDocument();
		PID parentPID = null;
		// Try to get the parent pid from the items ancestors if available.
		if (idb.getAncestorPath() != null && idb.getAncestorPath().size() > 0) {
			String ancestor = idb.getAncestorPath().get(idb.getAncestorPath().size() - 1);
			int index = ancestor.indexOf(',');
			ancestor = ancestor.substring(index + 1);
			index = ancestor.indexOf(',');
			ancestor = ancestor.substring(0, index);
			parentPID = new PID(ancestor);
		} else {
			try {
				log.debug("Retrieving parent pid for " + dip.getPid().getPid());
				parentPID = tsqs.fetchByPredicateAndLiteral(ContentModelHelper.Relationship.contains.toString(), dip.getPid()).get(0);
			} catch (IndexOutOfBoundsException e) {
				throw new OrphanedObjectException("Could not retrieve parent pid for " + dip.getPid().getPid());
			}
		}
		log.debug("Retrieving parent DIP " + parentPID.getPid() + " for " + dip.getPid().getPid());
		DocumentIndexingPackage parentDIP = dipFactory.createDocumentIndexingPackage(parentPID);
		dip.setParentDocument(parentDIP);
		return parentDIP;
	}

	/**
	 * Gets a map of triples for the object identified in a DIP, either by pulling it from the indexing package if it is
	 * available or calling back to the triple store.
	 *
	 * @param dip
	 * @return
	 */
	protected Map<String, List<String>> retrieveTriples(DocumentIndexingPackage dip) {
		Map<String, List<String>> triples;
		if (dip.getFoxml() == null) {
			if (dip.getTriples() == null) {
				triples = tsqs.fetchAllTriples(dip.getPid());
				dip.setTriples(triples);
			} else triples = dip.getTriples();
		} else {
			triples = dip.getTriples();
		}
		return triples;
	}

	protected String getFirstTripleValue(Map<String, List<String>> triples, String property) {
		List<String> values = triples.get(property);
		if (values == null || values.size() == 0)
			return null;
		return values.get(0);
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tsqs) {
		this.tsqs = tsqs;
	}

	public void setDocumentIndexingPackageFactory(DocumentIndexingPackageFactory dipFactory) {
		this.dipFactory = dipFactory;
	}
}
