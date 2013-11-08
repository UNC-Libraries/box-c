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
package edu.unc.lib.dl.security.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.security.AccessControlUtils;
import edu.unc.lib.dl.security.AncestorFactory;

@Controller
public class AccessLookupController {
	private static final Logger log = LoggerFactory.getLogger(AccessLookupController.class);

	@Autowired
	private AncestorFactory ancestorFactory = null;
	@Autowired
	private AccessControlUtils accessControlUtils;

	/**
	 * Returns a JSON representation of all the roles and groups for the provided pid
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "fesl/{id}/getAccess", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getAccess(@PathVariable("id") String id) {
		log.debug("Retrieving ACLs for " + id);
		return accessControlUtils.getAllCdrAccessControls(new PID(id));
	}

	/**
	 * Returns true or false depending on if the provided groups have the specified permission on the selected pid. The
	 * groups can either be forwarded via headers or as a GET parameter.
	 * 
	 * @param id
	 * @param permissionName
	 * @param groups
	 * @return
	 */
	@RequestMapping(value = "fesl/{id}/hasAccess/{permissionName}", method = RequestMethod.GET)
	public @ResponseBody
	boolean hasAccess(@PathVariable("id") String id, @PathVariable("permissionName") String permissionName,
			@RequestParam("groups") String groups) {

		AccessGroupSet accessGroups;
		if (groups != null) {
			accessGroups = new AccessGroupSet(groups);
		} else {
			accessGroups = GroupsThreadStore.getGroups();
		}
		return this.hasAccess(id, permissionName, accessGroups);
	}

	private boolean hasAccess(String id, String permissionName, AccessGroupSet accessGroups) {
		PID pid = new PID(id);
		Permission permission = Permission.getPermission(permissionName);
		if (permission == null)
			return false;
		Map<String, Set<String>> roles = accessControlUtils.getRoles(pid);
		List<String> activeEmbargoes = accessControlUtils.getEmbargoes(pid);
		List<String> publicationStatus = accessControlUtils.getPublished(pid);
		boolean isActive = accessControlUtils.isActive(pid);

		return (new ObjectAccessControlsBean(pid, roles, accessControlUtils.getGlobalRoles(), activeEmbargoes,
				publicationStatus, isActive)).hasPermission(accessGroups, permission);
	}

	public void setAncestorFactory(AncestorFactory ancestorFactory) {
		this.ancestorFactory = ancestorFactory;
	}

	public void setAccessControlUtils(AccessControlUtils accessControlUtils) {
		this.accessControlUtils = accessControlUtils;
	}
}
