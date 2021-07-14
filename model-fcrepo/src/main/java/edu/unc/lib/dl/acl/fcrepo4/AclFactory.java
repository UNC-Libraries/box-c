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
package edu.unc.lib.dl.acl.fcrepo4;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.auth.fcrepo.model.RoleAssignment;

/**
 * Interface for factories which provide access control details repository
 * resources
 *
 * @author bbpennel
 *
 */
public interface AclFactory {

    /**
     * Returns a map of principals to granted roles which are active for the specified object.
     *
     * Only role assignments are taken into account, no other access restrictions are reflected.
     *
     * @param pid
     * @return A map of principals to sets of roles, where the roles are expressed as URI strings.
     */
    public Map<String, Set<String>> getPrincipalRoles(PID pid);

    /**
     * Retrieve all staff role assignments for the specified object
     *
     * @param pid identifier for the object
     * @return List of RoleAssignments for all the staff roles assigned to the object
     */
    public List<RoleAssignment> getStaffRoleAssignments(PID pid);

    /**
     * Retrieve all patron role assignments for the specified object
     *
     * @param pid identifier for the object
     * @return List of RoleAssignments for all the patron roles assigned to the object
     */
    public List<RoleAssignment> getPatronRoleAssignments(PID pid);

    /**
     * Returns active patron role assignments for an object, accounting for roles,
     * embargoes and deletion state.
     *
     * @param pid identifier for the object
     * @return List of patron role assignments after applying all restrictions.
     */
    public List<RoleAssignment> getPatronAccess(PID pid);

    /**
     * Returns the expiration date of an embargo imposed on the object, or null if no embargo is specified
     *
     * @param pid
     * @return
     */
    public Date getEmbargoUntil(PID pid);

    /**
     * Returns true if the object specified is marked for deletion.
     *
     * @param pid
     * @return
     */
    public boolean isMarkedForDeletion(PID pid);
}
