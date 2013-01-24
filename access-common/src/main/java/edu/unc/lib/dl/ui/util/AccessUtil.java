package edu.unc.lib.dl.ui.util;

import java.util.Set;

import edu.unc.lib.dl.acl.util.AccessGroupConstants;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

public class AccessUtil {

	public static boolean permitDatastreamAccess(AccessGroupSet groups, String datastream, BriefObjectMetadata metadata) {
		return AccessUtil.permitDatastreamAccess(groups, Datastream.getDatastream(datastream), metadata);
	}

	public static boolean permitDatastreamAccess(AccessGroupSet groups, Datastream datastream,
			BriefObjectMetadata metadata) {
		if (groups == null || datastream == null || metadata == null)
			return false;

		if (!metadata.getDatastreamObjects().contains(datastream.getName()))
			return false;
		
		if (groups.contains(AccessGroupConstants.ADMIN_GROUP)) {
			return true;
		}

		return metadata.getAccessControlBean().hasPermission(groups,
				Permission.getPermissionByDatastreamCategory(datastream.getCategory()));
	}

	/**
	 * Returns true if the user has list and no higher permissions for the given object
	 * 
	 * @param groups group membership
	 * @param metadata object to determine permissions against
	 * @return
	 */
	public static boolean hasListAccessOnly(AccessGroupSet groups, BriefObjectMetadata metadata) {
		if (groups.contains(AccessGroupConstants.ADMIN_GROUP))
			return false;
		
		Set<UserRole> userRoles = metadata.getAccessControlBean().getRoles(groups);
		if (userRoles.size() == 0 || !userRoles.contains(UserRole.list)) {
			return false;
		}

		// If the user has view description, the lowest level patron access, then they have more than list
		return !ObjectAccessControlsBean.hasPermission(groups, Permission.viewDescription, userRoles);
	}
}
