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
package edu.unc.lib.dl.service.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.log4j.Logger;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schema.Id;
import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.service.IdService;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class IdServiceImpl implements IdService {
	private final Logger logger = Logger.getLogger(getClass());
	private TripleStoreQueryService tripleStoreQueryService;
	private String baseURL;
	private String name;
	private String pass;
	Pattern getPid = Pattern.compile("<(info:fedora.+)>");
	Pattern getUid = Pattern.compile("\"(.+)\"");

	public PID fetchByRepositoryPath(String path) {
		return tripleStoreQueryService.fetchByRepositoryPath(path);
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(
			TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public Id getId(IrUrlInfo irUrlInfo) {
		Id id = new Id();
		id.setUid("unknown");
		id.setType(id.getUid());
		
		PID pid = tripleStoreQueryService.fetchByRepositoryPath(irUrlInfo
				.getFedoraUrl());
		
		id.setPid(pid.getPid());

		id.setIrUrlInfo(irUrlInfo);

		return id;
	}
	
	public String getPidFromRiPid(String riPid) {
		// Example format: "info:fedora/test:4"
		if (logger.isDebugEnabled())
			logger.debug("getPidFromRiPid " + riPid);

		if (riPid == null) {
			return null;
		}

		if (logger.isDebugEnabled())
			logger.debug("getPidFromRiPid "
					+ riPid.substring(riPid.indexOf('/') + 1));

		return riPid.substring(riPid.indexOf('/') + 1);
	}

	public String getUrlFromPid(String pid) {
		String result = null;
		StringBuffer query = new StringBuffer();
		query.append("select $uid from <#ri> where <info:fedora/").append(pid)
				.append("> <").append(Constants.RI_REPOSITORY_PATH).append(
						"> $uid");
		StringBuffer url = new StringBuffer();
		url.append(this.baseURL).append("risearch");
		try {
			HttpClientParams params = new HttpClientParams();
			params.setContentCharset("UTF-8");
			HttpClient client = new HttpClient();
			client.setParams(params);

			UsernamePasswordCredentials cred = new UsernamePasswordCredentials(
					this.name, this.pass);
			client.getState().setCredentials(new AuthScope(null, 443), cred);
			client.getState().setCredentials(new AuthScope(null, 80), cred);

			if (logger.isDebugEnabled())
				logger.debug(url.toString());
			PostMethod httppost = new PostMethod(url.toString());
			httppost.addParameter("type", "tuples");
			httppost.addParameter("lang", "itql");
			httppost.addParameter("format", "Simple");
			if (logger.isDebugEnabled())
				logger.debug("Query: " + query);
			httppost.addParameter("query", query.toString());
			String literalResponse = null;
			try {
				client.executeMethod(httppost);
				if (httppost.getStatusCode() == HttpStatus.SC_OK) {
					literalResponse = httppost.getResponseBodyAsString();
				} else {
					logger.error("Unexpected failure: "
							+ httppost.getStatusLine().toString());
				}
			} finally {
				httppost.releaseConnection();
			}
			logger.debug("getUrlFromPid Got response: " + literalResponse);

			if (!"".equals(literalResponse.trim())) {
				Matcher matcher = getUid.matcher(literalResponse);
				if (matcher.find()) {
					result = matcher.group(1);
				}
			}
			logger.debug("getUrlFromPid Got result: " + result);
		} catch (Exception e) {
			logger.error("problems", e);
		}
		return result;
	}

	public String getBaseURL() {
		return baseURL;
	}

	public void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}
}
