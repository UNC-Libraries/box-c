package edu.unc.lib.dl.acl.util;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.unc.lib.dl.fedora.PID;

/**
 * Encapsulates the complete set of access controls that apply to a particular object.
 * @author count0
 *
 */
public class ObjectAccessControlsBean {
	PID object = null;
	Map<UserRole, Set<String>> role2groups = null;
	List<Date> activeEmbargoes = null;
	static DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
	
	public ObjectAccessControlsBean(PID pid, Map<UserRole, Set<String>> role2groups) {
		this.object = pid;
		this.role2groups = role2groups;
	}
	
	/**
	 * Factory method which generates an ObjectAccessControlBean from a map containing role to groups relations
	 * 
	 * @param pid
	 * @param roleMappings
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static ObjectAccessControlsBean createObjectAccessControlBean(PID pid, Map<String, ? extends Collection<String>> roleMappings) {
		Map<UserRole, Set<String>> role2groups = new HashMap<UserRole, Set<String>>(roleMappings.size());
		
		Iterator<?> roleIt = roleMappings.entrySet().iterator(); 
		while (roleIt.hasNext()) {
			Map.Entry<String, Collection<String>> entry = (Map.Entry<String, Collection<String>>)roleIt.next();
			UserRole userRole = UserRole.getUserRole(entry.getKey());
			Set<String> groups = new HashSet<String>((Collection<String>)entry.getValue());
			role2groups.put(userRole, groups);
		}

		return new ObjectAccessControlsBean(pid, role2groups);
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
}
