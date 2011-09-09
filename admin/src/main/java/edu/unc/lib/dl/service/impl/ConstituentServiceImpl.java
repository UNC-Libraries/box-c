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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.log4j.Logger;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.schema.PathInfoDao;
import edu.unc.lib.dl.schema.PathInfoResponse;
import edu.unc.lib.dl.service.ConstituentService;
import edu.unc.lib.dl.service.IdService;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.util.TripleStoreQueryService.PathInfo;

/**
 * 
 * 
 */
public class ConstituentServiceImpl implements ConstituentService {
	private final Logger logger = Logger.getLogger(getClass());
	private String baseURL;
	private String name;
	private String pass;
	private IdService idService;
	Pattern getPid = Pattern.compile("<(info:fedora.+)>");
	Pattern getUid = Pattern.compile("\"(.+)\"");
	TripleStoreQueryService tripleStoreQueryService;

	public PathInfoResponse getBreadcrumbs(IrUrlInfo irUrlInfo) {
		PID pid = tripleStoreQueryService.fetchByRepositoryPath(irUrlInfo.getFedoraUrl());
				
		return getBreadcrumbs(pid.getPid());
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.unc.lib.dl.service.ConstituentService#getBreadcrumbs(java.lang.String)
	 */
	public PathInfoResponse getBreadcrumbs(String pid) {
		PathInfoResponse response = new PathInfoResponse();

		logger.debug("getBreadcrumbs entry");
		
		PID childPid = new PID(pid);

		List<PathInfo> paths = tripleStoreQueryService
				.lookupRepositoryPathInfo(childPid);

		for (int i = 0; i < paths.size(); i++) {
			PathInfoDao temp = new PathInfoDao();

			temp.setLabel(paths.get(i).getLabel());
			temp.setPath(paths.get(i).getPath());
			temp.setPid(paths.get(i).getPid().getPid());
			temp.setSlug(paths.get(i).getSlug());

			response.getPaths().add(temp);
		}

		logger.debug("getBreadcrumbs exit");
		
		return response;
	}

	/**
	 * @see edu.unc.lib.dl.service.ConstituentService#getOrderedConstituentPids(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 * 
	 * @param firstConstituent -
	 *            must be plain pid
	 * @param parentPid -
	 *            must be plain pid
	 */

	public List<String> getOrderedConstituentPids(String parentPid,
			String firstConstituentPid, String lastConstituentPid) {

		// sanity check on input
		if ((parentPid == null) || (firstConstituentPid == null)
				|| (lastConstituentPid == null)) {

			return null;
		}

		// only one constituent
		if (firstConstituentPid.equals(lastConstituentPid)) {
			List<String> result = new ArrayList<String>(1);
			result.add(firstConstituentPid);

			return result;
		}

		return getOrderedConstituentPidsFromParentPid(parentPid,
				firstConstituentPid);
	}

	/*
	 * select $s $next from <#ri> where $s <fedora-rels-ext:isConstituentOf>
	 * <info:fedora/test:1248> and $s <fedora-rels-ext:nextConstituent> $next
	 */

	private List<String> getOrderedConstituentPidsFromParentPid(
			String parentPid, String firstConstituentPid) {
		List<String> result = null;

		StringBuffer query = new StringBuffer();
		query
				.append("select $s $next from <#ri> where $s <fedora-rels-ext:isConstituentOf> ");
		query.append("<").append(Constants.RI_PID_PREFIX).append(parentPid)
				.append("> and $s <fedora-rels-ext:nextConstituent> $next");
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
			httppost.addParameter("format", "CSV");
			if (logger.isDebugEnabled())
				logger.debug("Query: " + query);
			httppost.addParameter("query", query.toString());
			InputStream is = null;
			List<String> temp = null;

			try {
				client.executeMethod(httppost);
				if (httppost.getStatusCode() == HttpStatus.SC_OK) {
					temp = getRawConstituentList(httppost
							.getResponseBodyAsStream());
				} else {
					logger.error("Unexpected failure: "
							+ httppost.getStatusLine().toString());
				}
			} finally {
				httppost.releaseConnection();
			}

			result = getOrderedConstituentList(firstConstituentPid, temp);

		} catch (Exception e) {
			logger.error("problems", e);
		}

		return result;
	}

	private List<String> getOrderedConstituentList(String firstConstituent,
			List<String> rawList) {
		List<String> result = new ArrayList<String>(rawList.size());
		Map<String, String> tempMap = new HashMap<String, String>(rawList
				.size());

		rawList.remove(0); // remove "s","next" header

		// parse info:fedora/test:85,info:fedora/test:86 to populate map with
		// key of test:85 and value of test:86
		for (int i = 0; i < rawList.size(); i++) {

			String[] keyvalue = new String[2];
			String temp = rawList.get(i);
			int separator = temp.indexOf(',');
			keyvalue[0] = temp.substring(0, separator);
			keyvalue[1] = temp.substring(separator + 1);

			if (logger.isDebugEnabled())
				logger.debug("prev: " + keyvalue[0] + " next: " + keyvalue[1]);

			tempMap.put(idService.getPidFromRiPid(keyvalue[0]), idService
					.getPidFromRiPid(keyvalue[1]));
		}

		// using firstConstituent, start populating list of first, next, next,
		// etc. constituents

		String nextConstituent = firstConstituent;

		if (logger.isDebugEnabled())
			logger.debug("nextConstituent: " + nextConstituent);

		while (nextConstituent != null) {
			result.add(nextConstituent);
			nextConstituent = tempMap.get(nextConstituent);
			if (logger.isDebugEnabled())
				logger.debug("nextConstituent: " + nextConstituent);
		}

		return result;
	}

	private List<String> getRawConstituentList(InputStream is)
			throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		List<String> result = new ArrayList<String>();
		String temp = null;

		while ((temp = br.readLine()) != null) {
			result.add(temp);
		}

		br.close();

		return result;
	}

	public void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	public void setIdService(IdService idService) {
		this.idService = idService;
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(
			TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}
