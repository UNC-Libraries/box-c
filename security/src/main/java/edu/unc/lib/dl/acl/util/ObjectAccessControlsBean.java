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

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates the complete set of access controls that apply to a particular object.
 * 
 * @author bbpennel
 * 
 */
public interface ObjectAccessControlsBean {
    /**
     * Find the last active embargo date, or null of no embargo is set.
     *
     * @return the embargo date or null
     */
    public Date getLastActiveEmbargoUntilDate();

    /**
     * Builds a set of all user roles granted to the given groups.
     *
     * @param groups
     * @return
     */
    public Set<UserRole> getRoles(AccessGroupSet groups);

    /**
     * Returns true if a user has the specified permission on this object, given
     * a set of groups.
     *
     * @param groups
     *            user memberships
     * @param permission
     *            the permission requested
     * @return if permitted
     */
    public boolean hasPermission(AccessGroupSet groups, Permission permission);

    public static boolean hasPermission(AccessGroupSet groups, Permission permission, Set<UserRole> roles) {
        for (UserRole r : roles) {
            if (r.getPermissions().contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the set of all permissions granted to a set of access groups
     *
     * @param groups
     * @return
     */
    public Set<String> getPermissionsByGroups(AccessGroupSet groups);

    /**
     * Returns all groups assigned to this object that possess the given permission
     *
     * @param permission
     * @return
     */
    public Set<String> getGroupsByPermission(Permission permission);

    /**
     * Returns all groups assigned to the given role
     *
     * @param userRole
     * @return
     */
    public Set<String> getGroupsByUserRole(UserRole userRole);

    /**
     * Returns a list where each entry contains a single role name + group pairing assigned to this object. Values are
     * pipe delimited
     * 
     * @return
     */
    public List<String> roleGroupsToUnprefixedList();
}
