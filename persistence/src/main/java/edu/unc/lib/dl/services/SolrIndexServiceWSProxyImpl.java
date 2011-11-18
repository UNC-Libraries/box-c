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
package edu.unc.lib.dl.services;

import java.util.Collection;

import javax.xml.transform.Source;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.transport.http.CommonsHttpMessageSender;

import edu.unc.lib.dl.fedora.PID;

public class SolrIndexServiceWSProxyImpl implements SolrIndexService {
	private static final Log log = LogFactory.getLog(SolrIndexServiceWSProxyImpl.class);
	private String password;
	private String url;
	private String username;
	private WebServiceTemplate webServiceTemplate = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.services.SolrIndexService#add(java.util.Collection)
	 */
	public boolean add(Collection<PID> ids) {
		log.debug("Sending PIDs to AddToSearchRequest WS: " + url);
		Document doc = new Document();
		Element root = new Element("AddToSearchRequest", "dls", "http://www.lib.unc.edu/dlservice/schemas");
		doc.setRootElement(root);
		for (PID id : ids) {
			Element pid = new Element("pid", "dls", "http://www.lib.unc.edu/dlservice/schemas");
			pid.setText(id.getPid());
			root.addContent(pid);
		}
		Source src = new org.jdom.transform.JDOMSource(doc);
		JDOMResult responseResult = new JDOMResult();
		boolean foo = this.webServiceTemplate.sendSourceAndReceiveToResult(src, responseResult);
		if (log.isDebugEnabled()) {
			log.debug(new XMLOutputter().outputString(responseResult.getDocument()));
		}
		if (!foo) {
			log.error("There was a problem updating the Solr index. SOAP response follows.");
			log.error(new XMLOutputter().outputString(responseResult.getDocument()));
		}
		return foo;
	}

	public String getPassword() {
		return password;
	}

	public String getUrl() {
		return url;
	}

	public String getUsername() {
		return username;
	}

	public void init() throws Exception {
		SaajSoapMessageFactory msgFactory = new SaajSoapMessageFactory();
		msgFactory.afterPropertiesSet();

		CommonsHttpMessageSender messageSender = new CommonsHttpMessageSender();
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(this.getUsername(), this.getPassword());
		messageSender.setCredentials(creds);
		messageSender.afterPropertiesSet();

		this.webServiceTemplate = new WebServiceTemplate(msgFactory);
		this.webServiceTemplate.setMessageSender(messageSender);
		this.webServiceTemplate.setDefaultUri(this.url);
		this.webServiceTemplate.afterPropertiesSet();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.services.SolrIndexService#remove(java.util.Collection)
	 */
	public boolean remove(Collection<PID> ids) {
		log.debug("Sending PIDs to RemoveFromSearchRequest WS: " + url);
		Document doc = new Document();
		Element root = new Element("RemoveFromSearchRequest", "dls", "http://www.lib.unc.edu/dlservice/schemas");
		doc.setRootElement(root);
		for (PID id : ids) {
			Element pid = new Element("pid", "dls", "http://www.lib.unc.edu/dlservice/schemas");
			pid.setText(id.getPid());
			root.addContent(pid);
		}
		Source src = new org.jdom.transform.JDOMSource(doc);
		JDOMResult responseResult = new JDOMResult();
		boolean foo = this.webServiceTemplate.sendSourceAndReceiveToResult(src, responseResult);
		if (log.isDebugEnabled()) {
			log.debug(new XMLOutputter().outputString(responseResult.getDocument()));
		}
		if (!foo) {
			log.error("There was a problem updating the Solr index. SOAP response follows.");
			log.error(new XMLOutputter().outputString(responseResult.getDocument()));
		}
		return foo;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.services.SolrIndexService#reindexEverything()
	 */
	@Override
	public boolean reindexEverything() {
		log.debug("Reindexing Everything in Solr via WS: " + url);
		Document doc = new Document();
		Element root = new Element("ReindexSearchRequest", "dls", "http://www.lib.unc.edu/dlservice/schemas");
		doc.setRootElement(root);
		root.addContent(new Element("userid", "dls", "http://www.lib.unc.edu/dlservice/schemas").setText("test"));
		Source src = new org.jdom.transform.JDOMSource(doc);
		JDOMResult responseResult = new JDOMResult();
		boolean foo = this.webServiceTemplate.sendSourceAndReceiveToResult(src, responseResult);
		if (log.isDebugEnabled()) {
			log.debug(new XMLOutputter().outputString(responseResult.getDocument()));
		}
		if (!foo) {
			log.error("There was a problem reindexing everything in Solr. SOAP response follows.");
			log.error(new XMLOutputter().outputString(responseResult.getDocument()));
		}
		return foo;
	}
}
