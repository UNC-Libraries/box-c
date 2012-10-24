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
package edu.unc.lib.dl.security;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fcrepo.server.errors.ObjectNotFoundException;
import org.jdom.Content;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;

public class AccessControlUtils {
	private PID collectionsPid;
	private static final Logger LOG = LoggerFactory
			.getLogger(AccessControlUtils.class);
	private edu.unc.lib.dl.util.TripleStoreQueryService tripleStoreQueryService = null;
	private AncestorFactory ancestoryFactory = null;
	private GroupRolesFactory groupRolesFactory = null;

	public AncestorFactory getAncestoryFactory() {
		return ancestoryFactory;
	}

	public void setAncestoryFactory(AncestorFactory ancestoryFactory) {
		this.ancestoryFactory = ancestoryFactory;
	}

	public GroupRolesFactory getGroupRolesFactory() {
		return groupRolesFactory;
	}

	public void setGroupRolesFactory(GroupRolesFactory groupRolesFactory) {
		this.groupRolesFactory = groupRolesFactory;
	}

	public edu.unc.lib.dl.util.TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(
			edu.unc.lib.dl.util.TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public void init() {
		collectionsPid = tripleStoreQueryService
				.fetchByRepositoryPath("/Collections");
		if (collectionsPid == null)
			throw new Error("Cannot find collections pid, failing.");
	}

	public AccessControlUtils() {
	}

	/**
	 * Given a PID, return the group permissions organized by rights type in an
	 * XML structure: <permissions><roles><originalsRead>rla</originalsRead>
	 * <originalsRead>abc</originalsRead></roles></permissions>
	 * 
	 * @param inputPid
	 * @return XML representation of the access control for this PID.
	 */
	public Content getAllCdrAccessControls(PID pid) {
		LOG.debug("getAllCdrAccessControls: " + pid);
		
		// TODO add embargo date

		// build a consolidated list of groups in each role
		Map<String, Set<String>> summary = new HashMap<String, Set<String>>();
		try {
			Map<String, Set<String>> local = groupRolesFactory.getAllRolesAndGroups(pid.getPid());
			summary.putAll(local);

			// list of ancestors from which this pid inherits access controls
			List<PID> ancestors = this.ancestoryFactory.getAncestry(pid
					.getPid());
			for (PID ancestor : ancestors) {
				Map<String, Set<String>> inherited = groupRolesFactory.getAllRolesAndGroups(ancestor.getPid());
				if (inherited != null && !inherited.isEmpty()) {
					for(String role : inherited.keySet()) {
						if(summary.containsKey(role)) {
							summary.get(role).addAll(inherited.get(role));
						} else {
							summary.put(role, inherited.get(role));
						}
					}
				}
			}
		} catch (ObjectNotFoundException e) {
			LOG.error("Cannot find object in question", e);
		}

		Element permsEl = new Element("permissions");
		Element rolesEl = new Element("roles");
		permsEl.addContent(rolesEl);
		for(String role : summary.keySet()) {
			Element roleEl = new Element("role");
			rolesEl.addContent(roleEl);
			roleEl.setAttribute("roleId", role);
			for(String groupName : summary.get(role)) {
				Element groupEl = new Element("group");
				roleEl.addContent(groupEl);
				groupEl.setText(groupName);
			}
		}
		return permsEl;
	}

	/**
	 * Retrieves a map containing the set of permission groups for each resource
	 * type.
	 */
	public Set<String> getGroupsInRole(PID pid, String role) {
		Set<String> groups = new HashSet<String>();
		LOG.debug("getGroupsForRole: " + pid + " " + role);

		try {
			Set<String> local = groupRolesFactory.getGroupsInRole(pid.getPid(),
					role);
			if (local != null) {
				groups.addAll(local);
			}

			// list of ancestors from which this pid inherits access controls
			List<PID> ancestors = this.ancestoryFactory.getAncestry(pid
					.getPid());
			for (PID ancestor : ancestors) {
				Set<String> additions = groupRolesFactory.getGroupsInRole(
						ancestor.getPid(), role);
				if (additions != null) {
					groups.addAll(additions);
				}
			}
		} catch (ObjectNotFoundException e) {
			LOG.error("Cannot find object in question", e);
		}
		return groups;
	}
}