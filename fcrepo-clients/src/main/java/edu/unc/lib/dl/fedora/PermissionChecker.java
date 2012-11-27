package edu.unc.lib.dl.fedora;

import java.util.Set;

import edu.unc.lib.dl.acl.util.Permission;

public class PermissionChecker {
	public boolean hasPermission(Permission perm, Set<String> groups) {
		// TODO get roles from cdr fesl service
		// TODO check if any roles have the requested permission
		// TODO find a way to save a round trip.. return all permissions and roles at once?
		return false;
	}
}
