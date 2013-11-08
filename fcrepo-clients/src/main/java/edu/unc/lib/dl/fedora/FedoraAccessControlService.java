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
package edu.unc.lib.dl.fedora;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.httpclient.HttpClientUtil;

/**
 * Retrieves the access control information for objects stored in Fedora.
 * 
 * @author bbpennel
 * 
 */
public class FedoraAccessControlService implements AccessControlService {
	private static final Logger log = LoggerFactory.getLogger(FedoraAccessControlService.class);

	private MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager;
	private HttpClient httpClient;
	private ObjectMapper mapper;
	private String aclEndpointUrl;
	private String username;
	private String password;

	public FedoraAccessControlService() {
		this.multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();
		this.httpClient = new HttpClient(this.multiThreadedHttpConnectionManager);
		this.mapper = new ObjectMapper();
	}

	public void init() {
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
		httpClient.getState().setCredentials(HttpClientUtil.getAuthenticationScope(aclEndpointUrl), creds);
	}

	public void destroy() {
		this.httpClient = null;
		this.mapper = null;
		if (this.multiThreadedHttpConnectionManager != null)
			this.multiThreadedHttpConnectionManager.shutdown();
	}

	/**
	 * @Inheritdoc
	 * 
	 *             Retrieves the access control from a Fedora JSON endpoint, represented by role to group relations.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ObjectAccessControlsBean getObjectAccessControls(PID pid) {
		GetMethod method = new GetMethod(this.aclEndpointUrl + pid.getPid() + "/getAccess");
		try {
			int statusCode = httpClient.executeMethod(method);

			if (statusCode == HttpStatus.SC_OK) {
				Map<?, ?> result = (Map<?, ?>) mapper.readValue(method.getResponseBodyAsStream(), Object.class);
				Map<String, List<String>> roles = (Map<String, List<String>>) result.get("roles");
				Map<String, List<String>> globalRoles = (Map<String, List<String>>) result.get("globals");
				List<String> embargoes = (List<String>) result.get("embargoes");
				List<String> publicationStatus = (List<String>) result.get("publicationStatus");
				Boolean isActive = (Boolean) result.get("objectActive");

				return new ObjectAccessControlsBean(pid, roles, globalRoles, embargoes, publicationStatus, isActive);
			}
		} catch (HttpException e) {
			log.error("Failed to retrieve object access control for " + pid, e);
		} catch (IOException e) {
			log.error("Failed to retrieve object access control for " + pid, e);
		} finally {
			method.releaseConnection();
		}

		return null;
	}

	public boolean hasAccess(PID pid, AccessGroupSet groups, Permission permission) {
		GetMethod method = new GetMethod(this.aclEndpointUrl + pid.getPid() + "/hasAccess/" + permission.name());
		try {
			method.setQueryString(new NameValuePair[] { new NameValuePair("groups", groups.joinAccessGroups(";")) });
			int statusCode = httpClient.executeMethod(method);
			if (statusCode == HttpStatus.SC_OK) {
				String response = method.getResponseBodyAsString();
				Boolean hasAccess = Boolean.parseBoolean(response);
				return hasAccess != null && hasAccess;
			}
		} catch (HttpException e) {
			log.error("Failed to check hasAccess for " + pid, e);
		} catch (IOException e) {
			log.error("Failed to check hasAccess for " + pid, e);
		} finally {
			method.releaseConnection();
		}

		return false;
	}

	public void setAclEndpointUrl(String aclEndpointUrl) {
		this.aclEndpointUrl = aclEndpointUrl;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
