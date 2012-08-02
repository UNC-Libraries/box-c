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
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.httpclient.HttpClientUtil;

public class GetACController {
	private String servicesUrl;
	private String username;
	private String password;
	protected final Logger logger = Logger.getLogger(getClass());

	@RequestMapping(value = "/admin/ajax/ac", method = RequestMethod.GET)
	public @ResponseBody
	String getAc(@RequestParam String pid) {
		String ac = null;
		String dataUrl = servicesUrl + "/em/" + pid + "/RELS_EXT";

		HttpClient client = HttpClientUtil.getAuthenticatedClient(dataUrl, username, password);
		client.getParams().setAuthenticationPreemptive(true);
		GetMethod method = new GetMethod(dataUrl);

		try {
			client.executeMethod(method);
			if (method.getStatusCode() == HttpStatus.SC_OK) {
				try {
					ac = method.getResponseBodyAsString();
				} catch (IOException e) {
					logger.info("Problem retrieving " + dataUrl + " for " + pid + ": " + e.getMessage());
				} finally {
					method.releaseConnection();
				}
			} else {
				// Retry server errors
				if (method.getStatusCode() == 500) {
					try {
						logger.warn("Failed to retrieve " + method.getURI().getURI());
					} catch (URIException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					ac = "<?xml version='1.0' encoding='iso-8859-1'?><accessControl><embargo>2015-04-19</embargo><grant group='public' role='curator'></grant></accessControl>";
				} else {
					throw new Exception("Failure to fedora content due to response of: " + method.getStatusLine().toString()
							+ "\nPath was: " + method.getURI().getURI());
				}
			}
		} catch (Exception e) {
			logger.error("Error while attempting to stream Fedora content for " + pid, e);
		}
		logger.warn(ac);
		return ac;
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

}
