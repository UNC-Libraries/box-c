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
package edu.unc.lib.dl.search.solr.util;

import java.util.HashMap;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class which stores Solr index addressing and instantiation settings from a properties file.
 * @author bbpennel
 * $Id: SolrSettings.java 2799 2011-08-30 18:51:44Z bbpennel $
 * $URL: https://vcs.lib.unc.edu/cdr/cdr-master/trunk/solr-search/src/main/java/edu/unc/lib/dl/search/solr/util/SolrSettings.java $
 */
public class SolrSettings extends AbstractSettings  {
	private final Logger LOG = LoggerFactory.getLogger(SolrSettings.class);
	private String path;
	private String url;
	private String core;
	private int socketTimeout;
	private int connectionTimeout;
	private int defaultMaxConnectionsPerHost;
	private int maxConnections;
	private int maxRetries;
	private HashMap<String,String> fieldNames;
	
	public SolrSettings(){
		fieldNames = new HashMap<String,String>();
	}
	
	/**
	 * Initialize SolrSettings attributes from a properties input object 
	 * @param properties solr settings properties object.
	 */
	public void setProperties(Properties properties){
		LOG.debug("Setting properties.");
		this.setPath(properties.getProperty("solr.path", ""));
		this.setCore(properties.getProperty("solr.core", ""));
		this.setSocketTimeout(Integer.parseInt(properties.getProperty("solr.socketTimeout", "1000")));
		this.setConnectionTimeout(Integer.parseInt(properties.getProperty("solr.connectionTimeout", "100")));
		this.setDefaultMaxConnectionsPerHost(Integer.parseInt(properties.getProperty("solr.defaultMaxConnectionsPerHost", "100")));
		this.setMaxConnections(Integer.parseInt(properties.getProperty("solr.maxConnections", "100")));
		this.setMaxRetries(Integer.parseInt(properties.getProperty("solr.maxRetries", "1")));
		
		//Store the URL to the Solr index for non-embedded connections.  Add the core if specified.
		if (this.path != null){
			this.url = this.path;
			if (this.core != null && !this.core.equals("")){
				if (this.url.lastIndexOf("/") != this.url.length()-1)
					this.url += "/";
				this.url += this.core;
			}
		}
		
		populateMapFromProperty("solr.field.", fieldNames, properties);
		
		LOG.debug(this.toStringStatic());
	}


	/**
	 * Retrieve a SolrServer object according to the configuration specified in settings.
	 */
	public SolrServer getSolrServer(){
		SolrServer server = null;
		try {
			LOG.debug("Establishing Solr server:" + getUrl());
			server = new CommonsHttpSolrServer(getUrl());
			((CommonsHttpSolrServer)server).setSoTimeout(getSocketTimeout());  // socket read timeout
			((CommonsHttpSolrServer)server).setConnectionTimeout(getConnectionTimeout());
			((CommonsHttpSolrServer)server).setDefaultMaxConnectionsPerHost(getDefaultMaxConnectionsPerHost());
			((CommonsHttpSolrServer)server).setMaxTotalConnections(getMaxConnections());
			((CommonsHttpSolrServer)server).setMaxRetries(maxRetries);
		} catch (Exception e) {
			LOG.error("Error initializing Solr Server instance", e);
		}
		return server;
	}
	
	public String sanitize(String value){
		if (value == null)
			return value;
		return value.replaceAll("([\\+\\-!\\(\\)\\{\\}\\[\\]\\^\"~\\*\\?:\\\\])", "\\\\$1")
				.replaceAll("(AND|OR|NOT)", "'$1'");
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	

	public String getCore() {
		return core;
	}

	public void setCore(String core) {
		this.core = core;
	}


	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public int getDefaultMaxConnectionsPerHost() {
		return defaultMaxConnectionsPerHost;
	}

	public void setDefaultMaxConnectionsPerHost(int defaultMaxConnectionsPerHost) {
		this.defaultMaxConnectionsPerHost = defaultMaxConnectionsPerHost;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public String toStringStatic(){
		String output = " path: " + path;
		output += "\n url: " + url;
		output += "\n core: " + core;
		output += "\n socketTimeout: " + socketTimeout;
		output += "\n connectionTimeout: " + connectionTimeout;
		output += "\n defaultMaxConnectionsPerHost: " + defaultMaxConnectionsPerHost;
		output += "\n maxConnections: " + maxConnections;
		output += "\n fieldNames: " + fieldNames;
		return output;
	}

	public String getFieldKey(String name){
		return getKey(fieldNames, name);
	}
	
	public String getFieldName(String key){
		return fieldNames.get(key);
	}
	
	public HashMap<String, String> getFieldNamesInverted() {
		return getInvertedHashMap(fieldNames);
	}
	
	public HashMap<String, String> getFieldNames() {
		return fieldNames;
	}

	public void setFieldNames(HashMap<String, String> fieldNames) {
		this.fieldNames = fieldNames;
	}
	
	public int getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}
}
