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
