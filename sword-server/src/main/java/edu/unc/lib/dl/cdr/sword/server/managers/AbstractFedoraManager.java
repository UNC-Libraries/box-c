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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.swordapp.server.AuthCredentials;

import edu.unc.lib.dl.agents.AgentFactory;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.AccessControlRole;
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
	@Autowired
	protected AgentFactory agentFactory;

	public void init() {
		collectionsPidObject = this.tripleStoreQueryService.fetchByRepositoryPath("/Collections");
	}

	protected String readFileAsString(String filePath) throws java.io.IOException {
		LOG.debug("Loading path file " + filePath);
		StringBuffer fileData = new StringBuffer(1000);
		java.io.InputStream inStream = this.getClass().getResourceAsStream(filePath);
		java.io.InputStreamReader inStreamReader = new InputStreamReader(inStream);
		BufferedReader reader = new BufferedReader(inStreamReader);
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

	protected PID extractPID(String uri, String basePath) {
		String pidString = null;
		int pidIndex = uri.indexOf(basePath);
		if (pidIndex > -1) {
			pidString = uri.substring(pidIndex + basePath.length());
		}

		PID targetPID = null;
		if (pidString.trim().length() == 0) {
			targetPID = collectionsPidObject;
		} else {
			targetPID = new PID(pidString);
		}
		return targetPID;
	}

	protected boolean hasAccess(AuthCredentials auth, PID pid, AccessControlRole role, SwordConfigurationImpl config) {
		List<String> groupList = this.getGroups(auth, config);
		return (config.getAdminDepositor() != null && config.getAdminDepositor().equals(auth.getUsername()))
				|| accessControlUtils.hasAccess(pid, groupList, role.getUri().toString());
	}

	protected List<String> getGroups(AuthCredentials auth, SwordConfigurationImpl config) {
		List<String> groupList = new ArrayList<String>();
		groupList.add(config.getDepositorNamespace() + auth.getUsername());
		groupList.add("public");
		return groupList;
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

	public void setAgentFactory(AgentFactory agentFactory) {
		this.agentFactory = agentFactory;
	}
}
