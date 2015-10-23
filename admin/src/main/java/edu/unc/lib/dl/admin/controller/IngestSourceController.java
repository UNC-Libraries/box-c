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

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.admin.collect.IngestSourceManager;
import edu.unc.lib.dl.fedora.PID;

/**
 * @author bbpennel
 * @date Oct 23, 2015
 */
@Controller
public class IngestSourceController {
	@Autowired
	private AccessControlService aclService;
	
	@Autowired
	private IngestSourceManager sourceManager;

	@RequestMapping(value = "listSourceCandidates/{pid}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody Object listCandidatesPackages(@PathVariable("pid") String pid,
			HttpServletResponse resp) {
		PID destination = new PID(pid);
		
		AccessGroupSet groups = GroupsThreadStore.getGroups();
		// Check that the user has permission to deposit to the destination
		if (!aclService.hasAccess(destination, groups, Permission.addRemoveContents)) {
			resp.setStatus(401);
			return null;
		}
		
		return sourceManager.listCandidates(destination);
	}
	
	@RequestMapping(value = "listSources/{pid}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody Object listIngestSources(@PathVariable("pid") String pid,
			HttpServletResponse resp) {
		PID destination = new PID(pid);
		
		AccessGroupSet groups = GroupsThreadStore.getGroups();
		// Check that the user has permission to deposit to the destination
		if (!aclService.hasAccess(destination, groups, Permission.addRemoveContents)) {
			resp.setStatus(401);
			return null;
		}
		
		return sourceManager.listSources(destination);
	}
}
