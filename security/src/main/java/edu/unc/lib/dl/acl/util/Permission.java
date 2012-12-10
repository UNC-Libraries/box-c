package edu.unc.lib.dl.acl.util;

public enum Permission {
	addRemoveContents, editAccessControl, editDescription, moveToTrash, publish, purgeForever, viewAdminUI, viewDerivative, viewDescription, viewEmbargoed, viewOriginal;
	private Permission() {}
	
	public static Permission getPermission(String permissionName) {
		for (Permission permission: Permission.values()) {
			if (permission.name().equals(permissionName))
				return permission;
		}
		return null;
	}
}