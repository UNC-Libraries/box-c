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
package edu.unc.lib.dl.ui.controller;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.ui.service.LorisContentService;
import edu.unc.lib.dl.ui.util.UserAccessUtil;
import edu.unc.lib.dl.util.ContentModelHelper;

/**
 * Controller for requests related to accessing jp2's through loris. Applies cdr access control as a prerequisite to
 * connecting with loris.
 * 
 * @author bbpennel
 */
@Controller
public class LorisContentController extends AbstractSolrSearchController {
	private static final Logger LOG = LoggerFactory.getLogger(LorisContentController.class);

	@Autowired
	private LorisContentService lorisContentService;

	@Autowired
	@Qualifier("lorisUserAccessUtil")
	private UserAccessUtil userAccessUtil;

	/**
	 * Determines if the user is allowed to access a specific datastream on the selected object. If so, then the result
	 * is cached for future use.
	 * 
	 * @param id
	 * @param datastream
	 * @param request
	 * @return
	 */
	private boolean hasAccess(String id, String datastream) {
		// Defaults to jp2 surrogate if no datastream specified
		if (datastream == null) {
			datastream = ContentModelHelper.Datastream.IMAGE_JP2000.toString();
		}
		
		id = id + "/" + datastream;

		LOG.debug("Checking if user " + GroupsThreadStore.getUsername() + " with groups " + GroupsThreadStore.getGroups() + " has access to " + id);
		// TODO check publication status in Solr
		return userAccessUtil.hasAccess(id, GroupsThreadStore.getUsername(), GroupsThreadStore.getGroups());
	}

	/**
	 * Handles requests for jp2 metadata
	 * 
	 * @param model
	 * @param request
	 * @param response
	 */
	@RequestMapping("/jp2Metadata/{id}/{datastream}")
	public void getMetadata(@PathVariable("id") String id,
			@PathVariable("datastream") String datastream, HttpServletResponse response) {
		// Check if the user is allowed to view this object
		if (this.hasAccess(id, datastream)) {
			try {
				lorisContentService.getMetadata(id, datastream, response.getOutputStream(), response);
			} catch (IOException e) {
				LOG.error("Error retrieving JP2 metadata content for " + id, e);
			}
		} else {
			LOG.debug("Access was forbidden to " + id + " for user " + GroupsThreadStore.getUsername());
			response.setStatus(HttpStatus.SC_FORBIDDEN);
		}
	}

	/**
	 * Handles requests for individual region tiles.
	 * 
	 * @param model
	 * @param request
	 * @param response
	 */
	@RequestMapping("/jp2Region/{id}/{datastream}")
	public void getRegion(@PathVariable("id") String id,
			@PathVariable("datastream") String datastream, @RequestParam("svc.region") String region,
			@RequestParam("svc.level") String scale, @RequestParam("svc.rotate") String rotate,
			HttpServletResponse response) {
		// Check if the user is allowed to view this object
		if (this.hasAccess(id, datastream)) {
			try {
				lorisContentService
						.streamJP2(id, region, scale, rotate, datastream, response.getOutputStream(), response);
			} catch (IOException e) {
				LOG.error("Error retrieving streaming JP2 content for " + id, e);
			}
		} else {
			LOG.debug("Access was forbidden to " + id + " for user " + GroupsThreadStore.getUsername());
			response.setStatus(HttpStatus.SC_FORBIDDEN);
		}
	}

	public void setlorisContentService(LorisContentService lorisContentService) {
		this.lorisContentService = lorisContentService;
	}

	public void setUserAccessUtil(UserAccessUtil userAccessUtil) {
		this.userAccessUtil = userAccessUtil;
	}
}
