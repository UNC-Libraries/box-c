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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.httpclient.HttpClientUtil;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.ui.exception.InvalidRecordRequestException;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.TripleStoreQueryService;

@Controller
public class MODSController extends AbstractSwordController {
	private static final Logger log = LoggerFactory.getLogger(MODSController.class);

	@Autowired
	private String swordUrl;
	@Autowired
	private String swordUsername;
	@Autowired
	private String swordPassword;
	@Autowired
	private TripleStoreQueryService tripleStoreQueryService;

	/**
	 * Forwards user to the MODS editor page with the
	 * 
	 * @param idPrefix
	 * @param id
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "describe/{prefix}/{id}", method = RequestMethod.GET)
	public String editDescription(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id, Model model,
			HttpServletRequest request) {

		String pid = idPrefix + ":" + id;
		AccessGroupSet accessGroups = GroupsThreadStore.getGroups();

		// Retrieve the record for the container being reviewed
		SimpleIdRequest objectRequest = new SimpleIdRequest(pid, accessGroups);
		BriefObjectMetadataBean resultObject = queryLayer.getObjectById(objectRequest);
		if (resultObject == null) {
			throw new InvalidRecordRequestException();
		}
		model.addAttribute("resultObject", resultObject);

		return "edit/description";
	}

	/**
	 * Retrieves the MD_DESCRIPTIVE datastream, containing MODS, for this item if one is present. If it is not present,
	 * then returns a blank MODS document.
	 * 
	 * @param idPrefix
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "{prefix}/{id}/mods", method = RequestMethod.GET)
	public @ResponseBody
	String getMods(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id) {
		String pid = idPrefix + ":" + id;

		String mods = null;
		String dataUrl = swordUrl + "em/" + pid + "/" + ContentModelHelper.Datastream.MD_DESCRIPTIVE;

		HttpClient client = HttpClientUtil.getAuthenticatedClient(dataUrl, swordUsername, swordPassword);
		client.getParams().setAuthenticationPreemptive(true);
		GetMethod method = new GetMethod(dataUrl);

		// Pass the users groups along with the request
		AccessGroupSet groups = GroupsThreadStore.getGroups();
		method.addRequestHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER, groups.joinAccessGroups(";"));

		try {
			client.executeMethod(method);
			if (method.getStatusCode() == HttpStatus.SC_OK) {
				try {
					mods = method.getResponseBodyAsString();
				} catch (IOException e) {
					log.info("Problem uploading MODS for " + pid + ": " + e.getMessage());
				} finally {
					method.releaseConnection();
				}
			} else {
				if (method.getStatusCode() == HttpStatus.SC_BAD_REQUEST || method.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
					// Ensure that the object actually exists
					PID existingPID = tripleStoreQueryService.verify(new PID(pid));
					if (existingPID != null) {
						// MODS doesn't exist, so pass back an empty record.
						mods = "<mods:mods xmlns:mods=\"http://www.loc.gov/mods/v3\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"></mods:mods>";
					} else {
						throw new Exception("Unable to retrieve MODS.  Object " + pid + " does not exist in the repository.");
					}
				} else {
					throw new Exception("Failure to retrieve fedora content due to response of: "
							+ method.getStatusLine().toString() + "\nPath was: " + method.getURI().getURI());
				}
			}
		} catch (Exception e) {
			log.error("Error while attempting to stream Fedora content for " + pid, e);
		}
		return mods;
	}

	/**
	 * Pushes a MODS document to the target object
	 * 
	 * @param idPrefix
	 * @param id
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "describe/{prefix}/{id}", method = RequestMethod.POST)
	public @ResponseBody
	String updateDescription(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id, Model model,
			HttpServletRequest request, HttpServletResponse response) {

		String pid = idPrefix + ":" + id;
		String datastream = Datastream.MD_DESCRIPTIVE.getName();

		return this.updateDatastream(pid, datastream, request, response);
	}

	public void setSwordUrl(String swordUrl) {
		this.swordUrl = swordUrl;
	}

	public void setSwordUsername(String swordUsername) {
		this.swordUsername = swordUsername;
	}

	public void setSwordPassword(String swordPassword) {
		this.swordPassword = swordPassword;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}
