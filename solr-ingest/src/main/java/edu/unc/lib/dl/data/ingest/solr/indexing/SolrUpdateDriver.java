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
package edu.unc.lib.dl.data.ingest.solr.indexing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

/**
 * Performs batch add/delete/update operations to a Solr index.
 * 
 * @author bbpennel
 * 
 */
public class SolrUpdateDriver {
	private static final Logger log = LoggerFactory.getLogger(SolrUpdateDriver.class);

	private ConcurrentUpdateSolrServer solrServer;
	private SolrSettings solrSettings;

	private int autoPushCount;
	private int updateThreads;

	private static String ID_FIELD = "id";

	private static String UPDATE_TIMESTAMP = "timestamp";

	public void init() {
		log.debug("Instantiating concurrent udpate solr server for " + solrSettings.getUrl());
		solrServer = new ConcurrentUpdateSolrServer(solrSettings.getUrl(), autoPushCount, updateThreads);
	}

	public void addDocument(IndexDocumentBean idb) throws IndexingException {
		try {
			solrServer.addBean(idb);
		} catch (IOException e) {
			throw new IndexingException("Failed to add document to solr", e);
		} catch (SolrServerException e) {
			throw new IndexingException("Failed to add document to solr", e);
		}
	}

	/**
	 * Perform a partial document update from a IndexDocumentBean. Null fields are considered to be unspecified and will
	 * not be changed, except for the update timestamp field which is always set.
	 * 
	 * @param operation
	 * @param idb
	 * @throws IndexingException
	 */
	public void updateDocument(String operation, IndexDocumentBean idb) throws IndexingException {
		try {
			SolrInputDocument sid = solrServer.getBinder().toSolrInputDocument(idb);
			for (String fieldName : sid.getFieldNames()) {
				if (!ID_FIELD.equals(fieldName)) {
					SolrInputField inputField = sid.getField(fieldName);
					// Adding in each non-null field value, except the timestamp field which gets cleared if not specified so
					// that it always gets updated as part of a partial update
					// TODO enable timestamp updating when fix for SOLR-4133 is released, which enables setting null fields
					if (inputField != null && (inputField.getValue() != null || UPDATE_TIMESTAMP.equals(fieldName))) {
						Map<String, Object> partialUpdate = new HashMap<String, Object>();
						partialUpdate.put(operation, inputField.getValue());
						sid.setField(fieldName, partialUpdate);
					}
				}
			}
			log.debug("Performing partial update:\n" + ClientUtils.toXML(sid));
			solrServer.add(sid);
		} catch (IOException e) {
			throw new IndexingException("Failed to add document to solr", e);
		} catch (SolrServerException e) {
			throw new IndexingException("Failed to add document to solr", e);
		}
	}

	public void delete(PID pid) {
		this.delete(pid.getPid());
	}

	public void delete(String pid) {
		try {
			solrServer.deleteById(pid);
		} catch (IOException e) {
			throw new IndexingException("Failed to delete document from solr", e);
		} catch (SolrServerException e) {
			throw new IndexingException("Failed to delete document from solr", e);
		}
	}

	public void deleteByQuery(String query) {
		try {
			solrServer.deleteByQuery(query);
		} catch (IOException e) {
			throw new IndexingException("Failed to add document batch to solr", e);
		} catch (SolrServerException e) {
			throw new IndexingException("Failed to add document batch to solr", e);
		}
	}

	/**
	 * Force a commit of the currently staged updates.
	 */
	public void commit() {
		try {
			solrServer.commit();
		} catch (SolrServerException e) {
			throw new IndexingException("Failed to commit changes to solr", e);
		} catch (IOException e) {
			throw new IndexingException("Failed to commit changes to solr", e);
		}
	}

	public int getAutoPushCount() {
		return autoPushCount;
	}

	public void setAutoPushCount(int autoPushCount) {
		this.autoPushCount = autoPushCount;
	}

	public int getUpdateThreads() {
		return updateThreads;
	}

	public void setUpdateThreads(int updateThreads) {
		this.updateThreads = updateThreads;
	}

	public void setSolrServer(ConcurrentUpdateSolrServer solrServer) {
		this.solrServer = solrServer;
	}

	public void setSolrSettings(SolrSettings solrSettings) {
		this.solrSettings = solrSettings;
	}

}
