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
package edu.unc.lib.dl.acl.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DateTimeUtil;

/**
 * Encapsulates the complete set of access controls that apply to a particular object.
 * 
 * @author count0
 * 
 */
public class ObjectAccessControlsBean {
	private static final Logger LOG = LoggerFactory.getLogger(ObjectAccessControlsBean.class);

	PID object = null;
	Map<UserRole, Set<String>> baseRoleGroups = null;
	Map<UserRole, Set<String>> activeRoleGroups = null;
	List<Date> activeEmbargoes = null;

	public ObjectAccessControlsBean(PID pid, Map<UserRole, Set<String>> baseRoleGroups) {
		this.object = pid;
		this.baseRoleGroups = baseRoleGroups;
		this.activeRoleGroups = this.mergeRoleGroupsAndEmbargoes();
	}

	/**
	 * Constructs a new ObjectAccessControlsBean object from a collection of pipe delimited role uri/group pairings,
	 * representing all role/group relationships assigned to this object
	 * 
	 * @param pid
	 * @param roleGroups
	 */
	public ObjectAccessControlsBean(PID pid, Collection<String> roleGroups) {
		this.object = pid;
		this.baseRoleGroups = new HashMap<UserRole, Set<String>>();
		for (String roleGroup : roleGroups) {
			LOG.debug("roleGroup: " + roleGroup);
			String[] roleGroupArray = roleGroup.split("\\|");
			if (roleGroupArray.length == 2) {
				UserRole userRole = UserRole.getUserRole(roleGroupArray[0]);
				if (userRole == null) {
					continue;
				}
				Set<String> groupSet = baseRoleGroups.get(userRole);
				if (groupSet == null) {
					groupSet = new HashSet<String>();
					baseRoleGroups.put(userRole, groupSet);
				}
				groupSet.add(roleGroupArray[1]);
			}
		}

		this.activeRoleGroups = this.mergeRoleGroupsAndEmbargoes();
	}

	/**
	 * Constructs a new ObjectAccessControlsBean object from a map of role/group relations and active embargoes
	 * 
	 * @param pid
	 * @param roles
	 * @param embargoes
	 */
	@SuppressWarnings("unchecked")
	public ObjectAccessControlsBean(PID pid, Map<String, ? extends Collection<String>> roles,
			Collection<String> embargoes) {
		this.object = pid;
		this.baseRoleGroups = new HashMap<UserRole, Set<String>>();
		Iterator<?> roleIt = roles.entrySet().iterator();
		while (roleIt.hasNext()) {
			Map.Entry<String, Collection<String>> entry = (Map.Entry<String, Collection<String>>) roleIt.next();
			UserRole userRole = UserRole.getUserRole(entry.getKey());
			if (userRole != null) {
				Set<String> groups = new HashSet<String>((Collection<String>) entry.getValue());
				baseRoleGroups.put(userRole, groups);
			}
		}

		if (embargoes != null) {
			this.activeEmbargoes = new ArrayList<Date>(embargoes.size());
			for (String embargo : embargoes) {
				try {
					this.activeEmbargoes.add(DateTimeUtil.parsePartialUTCToDate(embargo));
				} catch (ParseException e) {
					LOG.warn("Failed to parse embargo " + embargo + " for object " + pid, e);
				}
			}
		}

		this.activeRoleGroups = this.mergeRoleGroupsAndEmbargoes();
	}

	public Map<UserRole, Set<String>> getActiveRoleGroups() {
		return this.activeRoleGroups;
	}

	/**
	 * Generates a new role/group mapping by filtering out role mappings that do not have administrative viewing rights
	 * if there are any active embargoes.
	 * 
	 * @return
	 */
	private Map<UserRole, Set<String>> mergeRoleGroupsAndEmbargoes() {
		// Check to see if there are active embargoes, and if there are that their window has not passed
		Date lastActiveEmbargo = getLastActiveEmbargoUntilDate();
		Map<UserRole, Set<String>> activeRoleGroups = null;
		if (lastActiveEmbargo != null) {
			activeRoleGroups = new HashMap<UserRole, Set<String>>();
			for (Map.Entry<UserRole, Set<String>> roleGroups : this.baseRoleGroups.entrySet()) {
				if (roleGroups.getKey().getPermissions().contains(Permission.viewAdminUI)) {
					activeRoleGroups.put(roleGroups.getKey(), roleGroups.getValue());
				}
			}
		} else {
			activeRoleGroups = this.baseRoleGroups;
		}
		return activeRoleGroups;
	}

	/**
	 * Find the last active embargo date, if applicable
	 * @return the embargo date or null
	 */
	public Date getLastActiveEmbargoUntilDate() {
		Date result = null;
		if (this.activeEmbargoes != null) {
			Date dateNow = new Date();
			for (Date embargoDate : this.activeEmbargoes) {
				if (embargoDate.after(dateNow)) {
					if(result == null || embargoDate.after(result)) {
						result = embargoDate;
					}
				}
			}
		}
		return result;
	}

	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Object Access Controls (").append(object.getPid()).append(")")
				.append("\nRoles granted to groups:\n");
		for (UserRole r : baseRoleGroups.keySet()) {
			result.append(r.getPredicate()).append("\n");
			for (String g : baseRoleGroups.get(r)) {
				result.append(g).append("\t");
			}
		}
		result.append("\nActive embargo dates:");
		if (activeEmbargoes != null) {
			for (Date d : activeEmbargoes) {
				try {
					result.append(DateTimeUtil.formatDateToUTC(d));
				} catch (ParseException e) {
					LOG.error("Failed to parse date " + d, e);
				}
			}
		}
		return result.toString();
	}

	public PID getObject() {
		return object;
	}

	/**
	 * Builds a set of all the active user roles granted to the given groups.
	 * 
	 * @param groups
	 * @return
	 */
	public Set<UserRole> getRoles(String[] groups) {
		return this.getRoles(groups, this.activeRoleGroups);
	}

	public Set<UserRole> getBaseRoles(String[] groups) {
		return this.getRoles(groups, this.baseRoleGroups);
	}

	/**
	 * Builds a set of all the user roles granted to the given groups.
	 * 
	 * @param groups
	 * @param roleGroups
	 * @return
	 */
	private Set<UserRole> getRoles(String[] groups, Map<UserRole, Set<String>> roleGroups) {
		Set<UserRole> result = new HashSet<UserRole>();
		for (String group : groups) { // get all user roles
			for (UserRole r : roleGroups.keySet()) {
				if (roleGroups.get(r).contains(group)) {
					result.add(r);
				}
			}
		}
		return result;
	}

	public Set<UserRole> getRoles(AccessGroupSet groups) {
		return this.getRoles(groups, this.activeRoleGroups);
	}

	public Set<UserRole> getBaseRoles(AccessGroupSet groups) {
		return this.getRoles(groups, this.baseRoleGroups);
	}

	private Set<UserRole> getRoles(AccessGroupSet groups, Map<UserRole, Set<String>> roleGroups) {
		Set<UserRole> result = new HashSet<UserRole>();
		for (String group : groups) { // get all user roles
			for (UserRole r : roleGroups.keySet()) {
				if (roleGroups.get(r).contains(group)) {
					result.add(r);
				}
			}
		}
		return result;
	}

	/**
	 * Determines if this access object contains roles matching any of the groups in the supplied access group set
	 * 
	 * @param groups group membershps
	 * @return true if any of the groups are associated with a role for this object
	 */
	public boolean containsAny(AccessGroupSet groups) {
		Map<UserRole, Set<String>> roleGroups = this.activeRoleGroups;
		for (String group : groups) {
			for (UserRole r : roleGroups.keySet()) {
				if (roleGroups.get(r).contains(group)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Determines if a user has a specific type of permission on this object, given a set of groups.
	 * @param groups user memberships
	 * @param permission the permission requested
	 * @return if permitted
	 */
	public boolean hasPermission(AccessGroupSet groups, Permission permission) {
		Set<UserRole> roles = this.getRoles(groups);
		return hasPermission(groups, permission, roles);
	}
	
	public static boolean hasPermission(AccessGroupSet groups, Permission permission, Set<UserRole> roles) {
		for (UserRole r : roles) {
			if (r.getPermissions().contains(permission))
				return true;
		}
		return false;
	}

	
	/**
	 * Determines if a user has a specific type of permission on this object, given a set of groups.
	 * @param groups user memberships
	 * @param permission the permission requested
	 * @return if permitted
	 */
	public boolean hasPermission(String[] groups, Permission permission) {
		Set<UserRole> roles = this.getRoles(groups);
		for (UserRole r : roles) {
			if (r.getPermissions().contains(permission))
				return true;
		}
		return false;
	}

	/**
	 * Returns all groups assigned to this object that possess the given permission
	 * 
	 * @param permission
	 * @return
	 */
	public Set<String> getGroupsByPermission(Permission permission) {
		Set<String> groups = new HashSet<String>();
		for (Map.Entry<UserRole, Set<String>> r2g : this.activeRoleGroups.entrySet()) {
			if (r2g.getKey().getPermissions().contains(permission)) {
				groups.addAll(r2g.getValue());
			}
		}
		return groups;
	}

	/**
	 * Returns all groups assigned to the given role
	 * 
	 * @param userRole
	 * @return
	 */
	public Set<String> getGroupsByUserRole(UserRole userRole) {
		return this.activeRoleGroups.get(userRole);
	}

	/**
	 * Returns a list where each entry contains a single role uri + group pairing assigned to this object. Values are
	 * pipe delimited
	 * 
	 * @return
	 */
	public List<String> roleGroupsToList() {
		List<String> result = new ArrayList<String>();
		for (Map.Entry<UserRole, Set<String>> r2g : this.activeRoleGroups.entrySet()) {
			String roleName = r2g.getKey().getURI().toString();
			for (String group : r2g.getValue()) {
				result.add(roleName + "|" + group);
			}
		}
		return result;
	}
}
