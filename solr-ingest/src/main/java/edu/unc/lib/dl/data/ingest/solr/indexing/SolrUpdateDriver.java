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
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

public class SolrUpdateDriver {
	private static final Logger log = LoggerFactory.getLogger(SolrUpdateDriver.class);

	private ConcurrentUpdateSolrServer solrServer;
	private SolrSettings solrSettings;

	private int autoPushCount;
	private int updateThreads;

	private static String ID_FIELD = "id";

	public void init() {
		// SSLSocketFactory socketFactory;
		// try {
		// socketFactory = new SSLSocketFactory(SSLContext.getInstance("SSL"),
		// SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		// } catch (NoSuchAlgorithmException e) {
		// log.error("Failed to created httpclient", e);
		// return;
		// }
		// Scheme sch = new Scheme("https", 443, socketFactory);
		// SchemeRegistry schemeRegistry = new SchemeRegistry();
		// schemeRegistry.register(sch);
		//
		// ThreadSafeClientConnManager ccm = new ThreadSafeClientConnManager(schemeRegistry);
		// ccm.setDefaultMaxPerRoute(32);
		// ccm.setMaxTotal(128);
		// DefaultHttpClient httpClient = new DefaultHttpClient(ccm);

		// solrServer = new ConcurrentUpdateSolrServer(solrSettings.getUrl(), httpClient, autoPushCount, updateThreads);
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

	public void updateDocument(String operation, IndexDocumentBean idb) throws IndexingException {
		try {
			SolrInputDocument sid = solrServer.getBinder().toSolrInputDocument(idb);
			for (String fieldName : sid.getFieldNames()) {
				if (!ID_FIELD.equals(fieldName)) {
					SolrInputField inputField = sid.getField(fieldName);
					if (inputField != null && inputField.getValue() != null) {
						Map<String, SolrInputField> partialUpdate = new HashMap<String, SolrInputField>();
						partialUpdate.put(operation, sid.getField(fieldName));
						sid.setField(fieldName, partialUpdate);
					}

				}
			}
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

	public void push() {
		// Queue and empty request so that the concurrent server will push its queue
		// UpdateRequest pushRequest = new UpdateRequest();
		// try {
		// solrServer.request(pushRequest);
		// } catch (SolrServerException e) {
		// throw new IndexingException("Failed to push to solr", e);
		// } catch (IOException e) {
		// throw new IndexingException("Failed to push to solr", e);
		// }
	}

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
