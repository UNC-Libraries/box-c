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
package edu.unc.lib.dl.cdr.sword.server.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordServerException;

import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.AccessControlUtils;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * 
 * @author bbpennel
 *
 */
public abstract class AbstractFedoraManager {
	private static Logger LOG = Logger.getLogger(AbstractFedoraManager.class);
	
	@Autowired
	protected AccessClient accessClient;
	@Autowired
	protected TripleStoreQueryService tripleStoreQueryService;
	@Autowired
	protected String swordPath;
	@Autowired
	protected AccessControlUtils accessControlUtils;
	protected PID collectionsPidObject;
	
	public void init(){
		collectionsPidObject = this.tripleStoreQueryService.fetchByRepositoryPath("/Collections");
	}

	protected String readFileAsString(String filePath) throws java.io.IOException {
		StringBuffer fileData = new StringBuffer(1000);
		java.io.InputStream inStream = this.getClass().getResourceAsStream(filePath);
		java.io.InputStreamReader inStreamReader = new InputStreamReader(inStream);
		BufferedReader reader = new BufferedReader(inStreamReader);
		// BufferedReader reader = new BufferedReader(new
		// InputStreamReader(this.getClass().getResourceAsStream(filePath)));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}

	public void authenticate(AuthCredentials auth) throws SwordAuthException, SwordServerException {
		HttpClient client = new HttpClient();
		UsernamePasswordCredentials cred = new UsernamePasswordCredentials(
				auth.getUsername(), auth.getPassword());
		client.getState().setCredentials(new AuthScope(null, 443), cred);
		client.getState().setCredentials(new AuthScope(null, 80), cred);

		GetMethod method = new GetMethod(accessClient.getFedoraContextUrl());
		
		try {
			method.setDoAuthentication(true);
			client.executeMethod(method);
			if (method.getStatusCode() == HttpStatus.SC_OK) {
				return;
			} else if (method.getStatusCode() == HttpStatus.SC_UNAUTHORIZED){
				throw new SwordAuthException(true);
			} else {
				throw new SwordServerException("Server responded with status " + method.getStatusCode());
			}
		} catch (HttpException e){
			throw new SwordServerException(e);
		} catch (IOException e) {
			throw new SwordServerException(e);
		} finally {
			method.releaseConnection();
		}
	}

	public AccessClient getAccessClient() {
		return accessClient;
	}

	public void setAccessClient(AccessClient accessClient) {
		this.accessClient = accessClient;
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public PID getCollectionsPidObject() {
		return collectionsPidObject;
	}

	public String getSwordPath() {
		return swordPath;
	}

	public void setSwordPath(String swordPath) {
		this.swordPath = swordPath;
	}

	public AccessControlUtils getAccessControlUtils() {
		return accessControlUtils;
	}

	public void setAccessControlUtils(AccessControlUtils accessControlUtils) {
		this.accessControlUtils = accessControlUtils;
	}
}
