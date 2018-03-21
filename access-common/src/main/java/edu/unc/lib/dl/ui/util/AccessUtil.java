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

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.acl.util.UserRole.canViewOriginals;

/**
 * @author bbpennel
 */
import java.util.Set;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AccessPrincipalConstants;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

/**
 * Utility methods for determining access state of objects, for use in UIs.
 *
 * @author bbpennel
 *
 */
public class AccessUtil {
    // RoleGroup value used to identify patron full public access
    private static final String PUBLIC_ROLE_VALUE = canViewOriginals.getPredicate() + "|" + PUBLIC_PRINC;

    private AccessUtil() {
    }

    public static boolean permitDatastreamAccess(AccessGroupSet groups, String datastream,
            BriefObjectMetadata metadata) {
        return AccessUtil.permitDatastreamAccess(groups, Datastream.getDatastream(datastream), metadata);
    }

    public static boolean permitDatastreamAccess(AccessGroupSet groups, Datastream datastream,
            BriefObjectMetadata metadata) {
        if (groups == null || datastream == null || metadata == null) {
            return false;
        }

        if (metadata.getDatastreamObjects() == null
                || !containsDatastream(metadata, datastream)) {
            return false;
        }

        if (groups.contains(AccessPrincipalConstants.AUTHENTICATED_PRINC)) {
            return true;
        }

        // Thumbnails are accessible to users with the list role
        if ((Datastream.THUMB_LARGE.equals(datastream) || Datastream.THUMB_SMALL.equals(datastream))
                && metadata.getAccessControlBean().getRoles(groups).contains(UserRole.list)) {
            return true;
        }

        return metadata.getAccessControlBean().hasPermission(groups,
                Permission.getPermissionByDatastreamCategory(datastream.getCategory()));
    }

    private static boolean containsDatastream(BriefObjectMetadata metadata, Datastream datastream) {
        return metadata.getDatastreamObjects().stream()
                .anyMatch(d -> d.getName().equals(datastream.getName()));
    }

    public static boolean hasAccess(AccessGroupSet groups, BriefObjectMetadata metadata, String permissionName) {
        if (metadata == null) {
            return false;
        }
        Permission permission = Permission.getPermission(permissionName);
        if (permission == null) {
            return false;
        }
        return hasAccess(groups, metadata, permission);
    }

    public static boolean hasAccess(AccessGroupSet groups, BriefObjectMetadata metadata, Permission permission) {
        if (metadata == null) {
            return false;
        }
        ObjectAccessControlsBean accessControlBean = metadata.getAccessControlBean();
        if (metadata.getAccessControlBean() == null) {
            return false;
        }
        return accessControlBean.hasPermission(groups, permission);
    }

    /**
     * Returns true if the user has list and no higher permissions for the given object
     *     * @param groups group membership
     * @param metadata object to determine permissions against
     * @return
     */
    public static boolean hasListAccessOnly(AccessGroupSet groups, BriefObjectMetadata metadata) {
        if (groups.contains(AccessPrincipalConstants.AUTHENTICATED_PRINC)) {
            return false;
        }

        Set<UserRole> userRoles = metadata.getAccessControlBean().getRoles(groups);
        if (userRoles.size() == 0 || !userRoles.contains(UserRole.list)) {
            return false;
        }

        // If the user has view description, the lowest level patron access, then they have more than list
        return !ObjectAccessControlsBean.hasPermission(groups, Permission.viewDescription, userRoles);
    }

    /**
     * Determines if full public access is allowed for the provided object.
     *
     * @param metadata object
     * @return true if full public access is allow for the object
     */
    public static boolean hasPatronRoleForPublicGroup(BriefObjectMetadata metadata) {
        if (metadata.getRoleGroup() == null) {
            return false;
        }
        return metadata.getRoleGroup().contains(PUBLIC_ROLE_VALUE);
    }
}
