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
import java.util.List;
import java.util.Map;

import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.xml.NamespaceConstants;

/**
 * Indexing filter which extracts Fedora generated dates about the creation and modification state of the object
 * being indexed.
 * 
 * Sets: dateAdded, dateUpdated
 * @author bbpennel
 *
 */
public class SetRecordDatesFilter extends AbstractIndexDocumentFilter {
	protected static final Logger log = LoggerFactory.getLogger(SetRecordDatesFilter.class);

	private String recordDatesQuery;

	private XPath dateAddedXPath;
	private XPath dateUpdatedXPath;

	public SetRecordDatesFilter() {
		try {
			dateAddedXPath = XPath.newInstance("foxml:property[@NAME='info:fedora/fedora-system:def/model#createdDate']/@VALUE");
			dateAddedXPath.addNamespace(Namespace.getNamespace("foxml", NamespaceConstants.FOXML_URI));
			dateUpdatedXPath = XPath.newInstance("foxml:property[@NAME='info:fedora/fedora-system:def/view#lastModifiedDate']/@VALUE");
			dateUpdatedXPath.addNamespace(Namespace.getNamespace("foxml", NamespaceConstants.FOXML_URI));
		} catch (JDOMException e) {
			log.error("Failed to initialize queries", e);
		}

		try {
			this.recordDatesQuery = this.readFileAsString("getRecordDates.sparql");
		} catch (IOException e) {
			log.error("Unable to find query file", e);
		}
	}
	
	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		if (dip.getFoxml() == null) {
			this.filterFromQuery(dip);
		} else {
			this.filterFromFOXML(dip);
		}
	}
	
	private void filterFromFOXML(DocumentIndexingPackage dip) throws IndexingException {
		Element objectProperties = dip.getObjectProperties();
		try {
			Attribute dateAdded = (Attribute) dateAddedXPath.selectSingleNode(objectProperties);
			Attribute dateUpdated = (Attribute) dateUpdatedXPath.selectSingleNode(objectProperties);
			dip.getDocument().setDateAdded(dateAdded.getValue());
			dip.getDocument().setDateUpdated(dateUpdated.getValue());
		} catch (JDOMException e) {
			throw new IndexingException("Failed to extract record dates from " + dip.getPid().getPid(), e);
		} catch (ParseException e) {
			throw new IndexingException("Failed to parse record dates from " + dip.getPid().getPid(), e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void filterFromQuery(DocumentIndexingPackage dip) throws IndexingException {
		String query = String.format(recordDatesQuery, tsqs.getResourceIndexModelUri(), dip.getPid().getURI());
		
		log.debug("Retrieving modification dates for " + dip.getPid().getPid() + " from triple store.");
		Map results = tsqs.sendSPARQL(query);
		List<Map> bindings = (List<Map>) ((Map) results.get("results")).get("bindings");
		if (bindings.size() == 0) {
			throw new IndexingException("Object " + dip.getPid() + " could not be found");
		} else {
			IndexDocumentBean idb = dip.getDocument();
			
			try {
				idb.setDateUpdated((String)bindings.get(0).get("modifiedDate"));
				idb.setDateAdded((String)bindings.get(0).get("createdDate"));
			} catch (ParseException e) {
				throw new IndexingException("Failed to parse date format from system generated date fields for " + dip.getPid().getPid(), e);
			}
		}
	}
}
