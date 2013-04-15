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
package edu.unc.lib.dl.admin.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.httpclient.HttpClientUtil;

@Controller
public class DeleteObjectController {
	private static final Logger log = LoggerFactory.getLogger(DeleteObjectController.class);
	@Autowired
	private String swordUrl;
	@Autowired
	private String swordUsername;
	@Autowired
	private String swordPassword;

	// TODO This controller should be replaced by a direct call to SWORD once group forwarding is fully up and running
	@RequestMapping(value = "delete/{prefix}/{id}", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> deleteObject(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id) {
		String pid = idPrefix + ":" + id;
		return this.deleteObject(pid);
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	public @ResponseBody
	List<? extends Object> deleteObjects(@RequestParam("ids") String ids) {
		HttpClient client = HttpClientUtil.getAuthenticatedClient(swordUrl + "object/", swordUsername, swordPassword);
		client.getParams().setAuthenticationPreemptive(true);

		List<Object> results = new ArrayList<Object>();
		for (String id : ids.split("\n")) {
			String dataUrl = swordUrl + "object/" + id;
			results.add(this.deleteObject(id, client, dataUrl));
		}

		return results;
	}

	private Map<String, ? extends Object> deleteObject(String pid) {
		String dataUrl = swordUrl + "object/" + pid;
		HttpClient client = HttpClientUtil.getAuthenticatedClient(dataUrl, swordUsername, swordPassword);
		client.getParams().setAuthenticationPreemptive(true);
		return this.deleteObject(pid, client, dataUrl);
	}

	private Map<String, ? extends Object> deleteObject(String pid, HttpClient client, String dataUrl) {
		String responseString;
		DeleteMethod method;

		method = new DeleteMethod(dataUrl);
		// Pass the users groups along with the request
		method.addRequestHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER, GroupsThreadStore.getGroupString());

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("pid", pid);
		result.put("action", "delete");

		try {
			client.executeMethod(method);
			if (method.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
				// success
				responseString = method.getResponseBodyAsString();
				result.put("timestamp", System.currentTimeMillis());
			} else if (method.getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
				responseString = method.getResponseBodyAsString();
				result.put("error", responseString);
			} else {
				// Retry server errors
				if (method.getStatusCode() == 500) {
					try {
						log.error("Failed to delete " + method.getURI().getURI() + ":\n" + method.getResponseBodyAsString());
					} catch (URIException e) {
						log.error("Bad URI", e);
					}
				} else {
					throw new IOException("Failure to fedora content due to response of: " + method.getStatusLine().toString()
							+ "\nPath was: " + method.getURI().getURI());
				}
			}
		} catch (IOException e) {
			log.error("Error while attempting to delete Fedora content at " + dataUrl + " for " + pid, e);
		} finally {
			method.releaseConnection();
		}
		return result;
	}
}
