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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fcrepo.server.errors.ObjectNotFoundException;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap.Builder;

import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * A factory for getting the groups assigned a specific role on an specific pid. This factory doesn't not supply
 * inherited roles. This factory is implemented with a small cache.
 * 
 * @author count0
 * 
 */
public class GroupRolesFactory {
	private Map<String, Map<String, Set<String>>> pids2Roles2Groups;
	private TripleStoreQueryService tripleStoreQueryService = null;

	public GroupRolesFactory() {
		Builder<String, Map<String, Set<String>>> mapBuilder = new Builder<String, Map<String, Set<String>>>();
		mapBuilder.maximumWeightedCapacity(256);
		this.pids2Roles2Groups = mapBuilder.build();
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	/**
	 * Get the roles for a particular pid
	 * 
	 * @param pid
	 * @return the set of all the roles
	 * */
	public Set<String> getGroupsInRole(PID pid, String role) throws ObjectNotFoundException {
		if (!pids2Roles2Groups.containsKey(pid.getPidAsString())) {
			updateCache(pid.getPidAsString());
		}
		Map<String, Set<String>> roles2Groups = pids2Roles2Groups.get(pid.getPidAsString());
		if (roles2Groups != null) {
			return roles2Groups.get(role);
		} else {
			return Collections.emptySet();
		}
	}

	/**
	 * Get the roles and groups for a particular pid
	 * 
	 * @param pid
	 * @return the map of all roles and groups assigned
	 * */
	public Map<String, Set<String>> getAllRolesAndGroups(PID pid) throws ObjectNotFoundException {
		if (!pids2Roles2Groups.containsKey(pid.getPidAsString())) {
			updateCache(pid.getPidAsString());
		}
		Map<String, Set<String>> roles2Groups = pids2Roles2Groups.get(pid.getPidAsString());
		return roles2Groups;
	}

	/**
	 * Destroy the cached pointers to a parent when it is edited.
	 * 
	 * @param pid
	 */
	public void invalidate(PID pid) {
		pids2Roles2Groups.remove(pid.getPidAsString());
	}

	private void updateCache(String pid) throws ObjectNotFoundException {
		Map<String, List<String>> triples = getTripleStoreQueryService().fetchAllTriples(new PID(pid));
		
		// Filter triples down to just group roles
		Map<String, Set<String>> roles2Groups = new HashMap<String, Set<String>>();
		if (triples != null) {
			for (UserRole role : UserRole.values()) {
				String roleUri = role.toString();
				
				if (triples.containsKey(roleUri)) {
					roles2Groups.put(roleUri, new HashSet<String>(triples.get(roleUri)));
				}
			}
		}
		// Cache the roles to groups mapping for this object
		pids2Roles2Groups.put(pid, roles2Groups);
	}
}
