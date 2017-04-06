/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.acl.fcrepo4;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fedora.PID;

/**
 * Utility which evaluates the permissions of agents on individual objects
 * 
 * @author bbpennel
 *
 */
public class ObjectPermissionEvaluator {

	private ObjectACLFactory aclFactory;

	private final Map<String, Set<String>> staffRolesToPermissions;

	private final Map<String, Set<String>> patronRolesToPermissions;

	public ObjectPermissionEvaluator() {
		// Cache mappings of staff/patron roles to permissions for quick lookup
		staffRolesToPermissions = UserRole.getStaffRoles().stream()
				.collect(Collectors.toMap(UserRole::getPropertyString,
						p -> p.getPermissions().stream()
								.map(q -> q.name()).collect(Collectors.toSet())));
		patronRolesToPermissions = UserRole.getPatronRoles().stream()
				.collect(Collectors.toMap(UserRole::getPropertyString,
						p -> p.getPermissions().stream()
								.map(q -> q.name()).collect(Collectors.toSet())));
	}

	/**
	 * Returns true if any of the provided principals for an agent are granted the
	 * requested permission on the object identified by pid.
	 * 
	 * @param pid
	 *            PID identifying the object to evaluate permissions against
	 * @param agentPrincipals
	 *            Set of principals for the agent requesting permission
	 * @param permission
	 *            Permission requested by agent
	 * @return
	 */
	public boolean hasStaffPermission(PID pid, Set<String> agentPrincipals,
			Permission permission) {
		if (permission == null || pid == null || agentPrincipals == null) {
			throw new IllegalArgumentException("Parameters must not be null");
		}
		if (agentPrincipals.size() == 0) {
			return false;
		}

		Map<String, List<String>> objectPrincipalRoles = aclFactory.getPrincipalRoles(pid);

		// Check staff principals against permissions granted by roles assigned to the object
		return agentPrincipals.stream().anyMatch(p -> {
			return objectPrincipalRoles.containsKey(p)
					&& objectPrincipalRoles.get(p).stream()
					.anyMatch(r -> staffRolesToPermissions.containsKey(r)
							&& staffRolesToPermissions.get(r).contains(permission.name()));
		});
	}

	/**
	 * Returns a subset of agent principals which are granted the requested
	 * patron permission on the specified object
	 * 
	 * @param pid
	 *            PID identifying the object to evaluate permissions against
	 * @param agentPrincipals
	 *            Set of principals for the agent requesting permission
	 * @param permission
	 *            Permission requested by agent
	 * @return
	 */
	public List<String> getPatronPrincipalsWithPermission(PID pid, Set<String> agentPrincipals,
			Permission permission) {
		if (permission == null || pid == null || agentPrincipals == null) {
			throw new IllegalArgumentException("Parameters must not be null");
		}
		if (agentPrincipals.size() == 0) {
			return Collections.emptyList();
		}

		Map<String, List<String>> objectPrincipalRoles = aclFactory.getPrincipalRoles(pid);

		// Get a list of patron principals which are granted the requested permission
		return agentPrincipals.stream().filter(p -> {
			return objectPrincipalRoles.containsKey(p)
					// Check if any roles for this principal grant the requested permission
					&& objectPrincipalRoles.get(p).stream()
					.anyMatch(r -> patronRolesToPermissions.containsKey(r)
							&& patronRolesToPermissions.get(r).contains(permission.name()));
		}).collect(Collectors.toList());
	}

	public void setAclFactory(ObjectACLFactory aclFactory) {
		this.aclFactory = aclFactory;
	}
}
