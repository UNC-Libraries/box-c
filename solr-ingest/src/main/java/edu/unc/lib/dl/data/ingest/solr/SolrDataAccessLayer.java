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
package edu.unc.lib.dl.data.ingest.solr;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.DirectXmlRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.search.solr.util.SolrSettings;

/**
 * Data access layer for connecting to and interacting with the Solr instance.
 * @author bbpennel
 * $URL: https://vcs.lib.unc.edu/cdr/cdr-master/trunk/solr-ingest/src/main/java/edu/unc/lib/dl/data/ingest/solr/SolrDataAccessLayer.java $
 * $Id: SolrDataAccessLayer.java 2736 2011-08-08 20:04:52Z count0 $
 */
public class SolrDataAccessLayer {
	private static final Logger LOG = LoggerFactory.getLogger(SolrDataAccessLayer.class);
	private SolrServer server;
	private String solrPath;
	private SolrSettings solrSettings;
	
	/**
	 * Initializes the connection to the Solr server.
	 */
	@SuppressWarnings("unused")
	private void initializeSolrServer(){
		server = solrSettings.getSolrServer();
	}
	
	/**
	 * Deletes all items in the index.
	 * @throws Exception
	 */
	public void clearIndex() throws Exception {
		server.deleteByQuery("*:*");
	}
	
	/**
	 * Deletes a single item identified by id
	 * @param id
	 * @throws Exception
	 */
	public void remove(String id) throws Exception {
		server.deleteByQuery("id:" + solrSettings.sanitize(id));
	}
	
	/**
	 * Uploads an XML document to the index for ingest and commits the operation.
	 * @param xml
	 */
	public void updateIndex(String xml) {
		DirectXmlRequest xmlRequest = new DirectXmlRequest( "/update", xml);
		synchronized(server){
			try {
				server.request(xmlRequest);
				server.commit();
			} catch (SolrServerException e) {
				LOG.error("Failed to update index", e);
				LOG.error("Update document contents:\n" + xml);
				try {
					Thread.sleep(120000);
					LOG.info("Retrying document upload");
					server.request(xmlRequest);
					server.commit();
					LOG.info("Document upload second attempt was successful.");
				} catch (Exception e2) {
					LOG.error("Second attempt to upload document failed.", e2);
				}
			} catch (IOException e){
				LOG.error("Failed to update index", e);
			}
		}
	}
	
	/**
	 * Returns the value of a single field from the object identified by pid.
	 * @param pid
	 * @param field
	 * @return The value of the specified field or null if it wasn't found.
	 */
	public Object getField(String pid, String field) throws SolrServerException {
		QueryResponse queryResponse = null;
		SolrQuery solrQuery = new SolrQuery();
		StringBuilder query = new StringBuilder();
		query.append("id:").append(solrSettings.sanitize(pid));
		solrQuery.setQuery(query.toString());
		solrQuery.addField(field);
		
		synchronized(server){
			queryResponse = server.query(solrQuery);
			if (queryResponse.getResults().getNumFound() > 0){
				return queryResponse.getResults().get(0).getFirstValue(field);
			}
		}
		return null;
	}

	public String getSolrPath() {
		return solrPath;
	}

	public void setSolrPath(String solrPath) {
		this.solrPath = solrPath;
	}

	public SolrSettings getSolrSettings() {
		return solrSettings;
	}

	public void setSolrSettings(SolrSettings solrSettings) {
		this.solrSettings = solrSettings;
	}
}
