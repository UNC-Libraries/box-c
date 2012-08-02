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
package edu.unc.lib.dl.ui;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.httpclient.HttpClientUtil;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class GetModsController {
	private String servicesUrl;
	private String username;
	private String password;
	private TripleStoreQueryService tripleStoreQueryService;
	protected final Logger logger = Logger.getLogger(getClass());

	@RequestMapping(value = "/admin/ajax/mods", method = RequestMethod.GET)
	public @ResponseBody
	String getMods(@RequestParam String pid) {
		String mods = null;
		String dataUrl = servicesUrl + "em/" + pid + "/" + ContentModelHelper.Datastream.MD_DESCRIPTIVE;

		HttpClient client = HttpClientUtil.getAuthenticatedClient(dataUrl, username, password);
		client.getParams().setAuthenticationPreemptive(true);
		GetMethod method = new GetMethod(dataUrl);

		try {
			client.executeMethod(method);
			if (method.getStatusCode() == HttpStatus.SC_OK) {
				try {
					mods = method.getResponseBodyAsString();
				} catch (IOException e) {
					logger.info("Problem uploading MODS for " + pid + ": " + e.getMessage());
				} finally {
					method.releaseConnection();
				}
			} else {
				if (method.getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
					// Ensure that the object actually exists
					PID existingPID = tripleStoreQueryService.verify(new PID(pid));
					if (existingPID != null) {
						// MODS doesn't exist, so pass back an empty record.
						mods = "<mods:mods xmlns:mods=\"http://www.loc.gov/mods/v3\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"></mods:mods>";
					} else {
						throw new Exception("Unable to retrieve MODS.  Object " + pid + " does not exist in the repository.");
					}
				} else {
					throw new Exception("Failure to retrieve fedora content due to response of: " + method.getStatusLine().toString()
							+ "\nPath was: " + method.getURI().getURI());
				}
			}
		} catch (Exception e) {
			logger.error("Error while attempting to stream Fedora content for " + pid, e);
		}
		return mods;
	}

	public String getServicesUrl() {
		return servicesUrl;
	}

	public void setServicesUrl(String servicesUrl) {
		this.servicesUrl = servicesUrl;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

}
