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

import java.text.DateFormat;
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

/**
 * Encapsulates the complete set of access controls that apply to a particular object.
 * @author count0
 *
 */
public class ObjectAccessControlsBean {
	private static final Logger LOG = LoggerFactory.getLogger(ObjectAccessControlsBean.class);
	
	PID object = null;
	Map<UserRole, Set<String>> role2groups = null;
	List<Date> activeEmbargoes = null;
	static DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
	
	public ObjectAccessControlsBean(PID pid, Map<UserRole, Set<String>> role2groups) {
		this.object = pid;
		this.role2groups = role2groups;
	}
	
	public ObjectAccessControlsBean(PID pid, Collection<String> roleGroups) {
		this.object = pid;
		this.role2groups = new HashMap<UserRole, Set<String>>();
		for (String roleGroup: roleGroups) {
			LOG.debug("roleGroup: " + roleGroup);
			String[] roleGroupArray = roleGroup.split("\\|");
			if (roleGroupArray.length == 2) {
				UserRole userRole = UserRole.getUserRole(roleGroupArray[0]);
				Set<String> groupSet = role2groups.get(userRole);
				if (groupSet == null) {
					groupSet = new HashSet<String>();
					role2groups.put(userRole, groupSet);
				}
				groupSet.add(roleGroupArray[1]);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public ObjectAccessControlsBean(PID pid, Map<String, ? extends Collection<String>> roles, Collection<String> embargoes) {
		
		this.object = pid;
		Iterator<?> roleIt = roles.entrySet().iterator(); 
		while (roleIt.hasNext()) {
			Map.Entry<String, Collection<String>> entry = (Map.Entry<String, Collection<String>>)roleIt.next();
			UserRole userRole = UserRole.getUserRole(entry.getKey());
			Set<String> groups = new HashSet<String>((Collection<String>)entry.getValue());
			role2groups.put(userRole, groups);
		}
		
		if (embargoes != null) {
			this.activeEmbargoes = new ArrayList<Date>(embargoes.size());
			for (String embargo: embargoes) {
				try {
					this.activeEmbargoes.add(ObjectAccessControlsBean.format.parse(embargo));
				} catch (ParseException e) {
					LOG.warn("Failed to parse embargo " + embargo + " for object " + pid, e);
				}
			}
		}
	}
	
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Object Access Controls (").append(object.getPid()).append(")")
		.append("\nRoles granted to groups:");
		for(UserRole r : role2groups.keySet()) {
			result.append(r.getPredicate()).append("\n");
			for(String g : role2groups.get(r)) {
				result.append(g).append("\t");
			}
		}
		result.append("\nActive embargo dates:");
		for(Date d : activeEmbargoes) {
			result.append(format.format(d));
		}
		return result.toString();
	}
	
	public PID getObject() {
		return object;
	}
	
	/**
	 * Builds a set of all the user roles granted to the given groups.
	 * @param groups
	 * @return
	 */
	public Set<UserRole> getRoles(String [] groups) {
		Set<UserRole> result = new HashSet<UserRole>();
		for(String group : groups) { // get all user roles
			for(UserRole r : role2groups.keySet()) {
				if(role2groups.get(r).contains(group)) {
					result.add(r);
					break;
				}
			}
		}
		return result;
	}
	
	public Set<UserRole> getRoles(AccessGroupSet groups) {
		Set<UserRole> result = new HashSet<UserRole>();
		for(String group : groups) { // get all user roles
			for(UserRole r : role2groups.keySet()) {
				if(role2groups.get(r).contains(group)) {
					result.add(r);
					break;
				}
			}
		}
		return result;
	}
	
	public boolean hasPermission(AccessGroupSet groups, Permission permission) {
		Set<UserRole> roles = this.getRoles(groups);
		for(UserRole r : roles) {
			if(r.getPermissions().contains(permission)) return true;
		}
		
		// TODO incorporate embargoes into this computation
		return false;
	}
	
	/**
	 * Determines a user permission, given a set of groups.
	 * @param groups user memberships
	 * @param permission the permission requested
	 * @return true if permitted
	 */
	public boolean hasPermission(String[] groups, Permission permission) {
		Set<UserRole> roles = this.getRoles(groups);
		for(UserRole r : roles) {
			if(r.getPermissions().contains(permission)) return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @param permission
	 * @return
	 */
	public Set<String> getGroupsByPermission(Permission permission) {
		Set<String> groups = new HashSet<String>();
		for(Map.Entry<UserRole, Set<String>> r2g : this.role2groups.entrySet()) {
			if (r2g.getKey().getPermissions().contains(permission)) {
				groups.addAll(r2g.getValue());
			}
		}
		return groups;
	}
	
	public Set<String> getGroupsByUserRole(UserRole userRole) {
		return this.role2groups.get(userRole);
	}
	
	public List<String> roleGroupsToList() {
		List<String> result = new ArrayList<String>();
		for(Map.Entry<UserRole, Set<String>> r2g : this.role2groups.entrySet()) {
			String roleName = r2g.getKey().getURI().toString();
			for (String group: r2g.getValue()) {
				result.add(roleName + "|" + group);
			}
		}
		return result;
	}
}
