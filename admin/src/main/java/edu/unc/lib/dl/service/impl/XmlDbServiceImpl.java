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
/**
 * 
 */
package edu.unc.lib.dl.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.springframework.ws.soap.SoapFaultException;

import edu.unc.lib.dl.schema.DataResponse;
import edu.unc.lib.dl.service.DataService;
import edu.unc.lib.dl.service.IdService;
import edu.unc.lib.dl.service.ItemInfoService;
import edu.unc.lib.dl.service.XmlDbService;
import edu.unc.lib.dl.util.UtilityMethods;
import edu.unc.lib.dl.ws.XmlDbErrorResultException;

/**
 * @author
 * 
 */
public class XmlDbServiceImpl implements XmlDbService {
	private final Logger logger = Logger.getLogger(getClass());

	private String xmlDbCollectionUrl;
	private String xmlDbBaseUrl;
	private String xmlDbUser;
	private String xmlDbPass;
	private String baseIrUrl;

	private DataService dataService;
	private IdService idService;
	private ItemInfoService itemInfoService;
	private UtilityMethods utilityMethods = new UtilityMethods();

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.service.XmlDbService#getFromXmlDb(java.lang.String)
	 */
	public String getFromXmlDb(String query) throws SoapFaultException {
		String response = null;

		StringBuffer url = new StringBuffer();
		url.append(this.xmlDbBaseUrl);

		try {
			HttpClientParams clientparams = new HttpClientParams();
			clientparams.setContentCharset("UTF-8");
			HttpClient client = new HttpClient();
			client.setParams(clientparams);

			UsernamePasswordCredentials cred = new UsernamePasswordCredentials(
					this.xmlDbUser, this.xmlDbPass);
			client.getState().setCredentials(new AuthScope(null, 443), cred);
			client.getState().setCredentials(new AuthScope(null, 80), cred);

			if (logger.isDebugEnabled()) {
				logger.debug(url.toString());
			}

			GetMethod httpget = new GetMethod(url.toString());

			List<NameValuePair> paramsList = new ArrayList<NameValuePair>();

			paramsList.add(new NameValuePair("_query", query));
			paramsList.add(new NameValuePair("_wrap", "no"));

			NameValuePair[] params = new NameValuePair[paramsList.size()];

			for (int i = 0; i < paramsList.size(); i++) {
				params[i] = paramsList.get(i);
			}

			httpget.setQueryString(params);

			try {
				client.executeMethod(httpget);
				if (httpget.getStatusCode() == HttpStatus.SC_OK) {
					response = httpget.getResponseBodyAsString();
				} else {
					logger.error("Unexpected failure: "
							+ httpget.getStatusLine().toString());

					throw new SoapFaultException(new XmlDbErrorResultException(
							httpget.getResponseBodyAsString()));
				}
			} finally {
				httpget.releaseConnection();
			}
			logger.debug("Got response: " + response);

		} catch (HttpException e) {
			logger.error("problems", e);
		} catch (IOException e) {
			logger.error("problems", e);
		}

		return response;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.service.XmlDbService#addToXmlDb(java.util.List)
	 */
	public String addToXmlDb(List<String> pids) {

		for (String pid : pids) {

			if (logger.isDebugEnabled())
				logger.debug(pid);

			addToXmlDb(pid);
		}

		// TODO Auto-generated method stub
		return null;
	}

	private String addToXmlDb(String pid) {
		String result = null;

		String path = idService.getUrlFromPid(pid);

		String repositoryPath = xmlDbCollectionUrl + path;

		if (logger.isDebugEnabled())
			logger.debug(repositoryPath);

		// TODO: 'concat' xml datastreams using JDOM into one xml doc and put
		// into eXist
		Element dc = getXmlStream(pid, "DC");

		XMLOutputter outputter = new XMLOutputter();

		String output = outputter.outputString(dc);

		result = putInXmlDb(output, path + ".xml");

		return result;
	}

	/*
	 */
	private String putInXmlDb(String xml, String path) {
		String response = null;
		StringBuffer url = new StringBuffer();
		url.append(this.xmlDbCollectionUrl).append(path);

		try {
			HttpClientParams params = new HttpClientParams();
			params.setContentCharset("UTF-8");
			HttpClient client = new HttpClient();
			client.setParams(params);

			UsernamePasswordCredentials cred = new UsernamePasswordCredentials(
					this.xmlDbUser, this.xmlDbPass);
			client.getState().setCredentials(new AuthScope(null, 443), cred);
			client.getState().setCredentials(new AuthScope(null, 80), cred);

			if (logger.isDebugEnabled()) {
				logger.debug(url.toString());
			}

			PutMethod httpput = new PutMethod(url.toString());

			List<NameValuePair> paramsList = new ArrayList<NameValuePair>();

			RequestEntity requestEntity = new StringRequestEntity(xml,
					"text/xml", "UTF-8");

			httpput.setRequestEntity(requestEntity);

			try {
				client.executeMethod(httpput);
				if (httpput.getStatusCode() == HttpStatus.SC_OK) {
					response = httpput.getResponseBodyAsString();
				} else {
					logger.error("Unexpected failure: "
							+ httpput.getStatusLine().toString());
				}
			} finally {
				httpput.releaseConnection();
			}
			logger.debug("Got response: " + response);

		} catch (Exception e) {
			logger.error("problems", e);
		}

		// TODO Auto-generated method stub
		return response;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.service.XmlDbService#putInXmlDb(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public String putInXmlDb(String pid, String ds, String path) {

		// TODO: Get data from datastream

		// TODO: Should pid determine path completely? Pass in irUrlInfo instead

		StringBuffer url = new StringBuffer();
		url.append(this.xmlDbBaseUrl).append(path);

		try {
			HttpClientParams params = new HttpClientParams();
			params.setContentCharset("UTF-8");
			HttpClient client = new HttpClient();
			client.setParams(params);

			UsernamePasswordCredentials cred = new UsernamePasswordCredentials(
					this.xmlDbUser, this.xmlDbPass);
			client.getState().setCredentials(new AuthScope(null, 443), cred);
			client.getState().setCredentials(new AuthScope(null, 80), cred);

			if (logger.isDebugEnabled()) {
				logger.debug(url.toString());
			}

			PutMethod httpput = new PutMethod(url.toString());

			List<NameValuePair> paramsList = new ArrayList<NameValuePair>();

			String testData = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">  <dc:title>Digital Curation Tutorial (JCDL 2007)</dc:title>  <dc:identifier>changeme:18</dc:identifier></oai_dc:dc>";

			RequestEntity requestEntity = new StringRequestEntity(testData,
					"text/xml", "UTF-8");

			httpput.setRequestEntity(requestEntity);

			String literalResponse = null;
			try {
				client.executeMethod(httpput);
				if (httpput.getStatusCode() == HttpStatus.SC_OK) {
					literalResponse = httpput.getResponseBodyAsString();
				} else {
					logger.error("Unexpected failure: "
							+ httpput.getStatusLine().toString());
				}
			} finally {
				httpput.releaseConnection();
			}
			logger.debug("Got response: " + literalResponse);

		} catch (Exception e) {
			logger.error("problems", e);
		}

		// TODO Auto-generated method stub
		return null;
	}

	private Element getXmlStream(String pid, String xmlStream) {
		Element result = null;

		DataResponse response = dataService.getData(pid, xmlStream);

		// convert response to JDOM Document
		SAXBuilder builder = new SAXBuilder();
		try {
			Document doc = builder.build(new ByteArrayInputStream(response
					.getDissemination().getStream()));

			result = doc.detachRootElement();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// return contents

		return result;
	}

	public void setXmlDbBaseUrl(String xmlDbBaseUrl) {
		this.xmlDbBaseUrl = xmlDbBaseUrl;
	}

	public void setXmlDbUser(String xmlDbUser) {
		this.xmlDbUser = xmlDbUser;
	}

	public void setXmlDbPass(String xmlDbPass) {
		this.xmlDbPass = xmlDbPass;
	}

	public void setBaseIrUrl(String baseIrUrl) {
		this.baseIrUrl = baseIrUrl;
	}

	public void setDataService(DataService dataService) {
		this.dataService = dataService;
	}

	public void setIdService(IdService idService) {
		this.idService = idService;
	}

	public void setXmlDbCollectionUrl(String xmlDbCollectionUrl) {
		this.xmlDbCollectionUrl = xmlDbCollectionUrl;
	}

	public void setItemInfoService(ItemInfoService itemInfoService) {
		this.itemInfoService = itemInfoService;
	}

}
