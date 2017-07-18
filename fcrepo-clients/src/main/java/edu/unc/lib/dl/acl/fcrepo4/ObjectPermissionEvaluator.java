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

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fedora.PID;

/**
 * Utility which evaluates the permissions of agents on individual objects
 *
 * @author bbpennel
 *
 */
public class ObjectPermissionEvaluator {

    private ObjectAclFactory aclFactory;

    private final Map<String, Set<String>> staffRolesToPermissions;

    private final Map<String, Set<String>> patronRolesToPermissions;

    public ObjectPermissionEvaluator() {
        // Cache mappings of staff/patron roles to permissions for quick lookup
        staffRolesToPermissions = UserRole.getStaffRoles().stream()
                .collect(Collectors.toMap(UserRole::getPropertyString,
                        p -> p.getPermissions().stream()
                                .map(q -> q.name()).collect(Collectors.toSet())));
        patronRolesToPermissions = UserRole.getPatronRoles().stream()
                .collect(Collectors.toMap(UserRole::getPropertyString,
                        p -> p.getPermissions().stream()
                                .map(q -> q.name()).collect(Collectors.toSet())));
    }

    /**
     * Returns true if any of the provided principals for an agent are granted the
     * requested permission on the object identified by pid.
     *
     * @param pid
     *            PID identifying the object to evaluate permissions against
     * @param agentPrincipals
     *            Set of principals for the agent requesting permission
     * @param permission
     *            Permission requested by agent
     * @return
     */
    public boolean hasStaffPermission(PID pid, Set<String> agentPrincipals,
            Permission permission) {
        if (permission == null || pid == null || agentPrincipals == null) {
            throw new IllegalArgumentException("Parameters must not be null");
        }
        if (agentPrincipals.size() == 0) {
            return false;
        }

        Map<String, Set<String>> objectPrincipalRoles = aclFactory.getPrincipalRoles(pid);

        // Check staff principals against permissions granted by roles assigned to the object
        return agentPrincipals.stream().anyMatch(p -> {
            return objectPrincipalRoles.containsKey(p)
                    && objectPrincipalRoles.get(p).stream()
                    .anyMatch(r -> staffRolesToPermissions.containsKey(r)
                            && staffRolesToPermissions.get(r).contains(permission.name()));
        });
    }

    /**
     * Returns a subset of agent principals which are granted the requested
     * patron permission on the specified object
     *
     * @param pid
     *            PID identifying the object to evaluate permissions against
     * @param agentPrincipals
     *            Set of principals for the agent requesting permission
     * @param permission
     *            Permission requested by agent
     * @return
     */
    public Set<String> getPatronPrincipalsWithPermission(PID pid, Set<String> agentPrincipals,
            Permission permission) {
        if (permission == null || pid == null || agentPrincipals == null) {
            throw new IllegalArgumentException("Parameters must not be null");
        }
        if (agentPrincipals.size() == 0) {
            return Collections.emptySet();
        }

        Map<String, Set<String>> objectPrincipalRoles = aclFactory.getPrincipalRoles(pid);

        // Get a list of patron principals which are granted the requested permission
        return agentPrincipals.stream().filter(p -> {
            return objectPrincipalRoles.containsKey(p)
                    // Check if any roles for this principal grant the requested permission
                    && objectPrincipalRoles.get(p).stream()
                    .anyMatch(r -> patronRolesToPermissions.containsKey(r)
                            && patronRolesToPermissions.get(r).contains(permission.name()));
        }).collect(Collectors.toSet());
    }

    /**
     * Returns true if the provided principal has patron level access to the
     * object represented by the given pid, and false if access is revoked
     *
     * @param pid
     * @param agentPrincipal
     * @return
     */
    public boolean hasPatronAccess(PID pid, String agentPrincipal, Permission permission) {
        return hasPatronAccess(pid, Arrays.asList(agentPrincipal), permission);
    }

    /**
     * Returns true if the provided agents have patron level access to the
     * object represented by the given pid, and false if access is revoked
     *
     * @param pid
     * @param agentPrincipals
     * @param permission
     * @return
     */
    public boolean hasPatronAccess(PID pid, Collection<String> agentPrincipals,
            Permission permission) {
        PatronAccess patronAccess = aclFactory.getPatronAccess(pid);
        if (PatronAccess.none.equals(patronAccess)) {
            // patron access revoked
            return false;
        }

        if (PatronAccess.authenticated.equals(patronAccess)) {
            // Access reduced to authenticated users
            if (!agentPrincipals.contains(AUTHENTICATED_PRINC)) {
                // Roles no longer sufficient, deny access
                return false;
            }
        }

        // If marked for deletion then patron access is revoked
        if (aclFactory.isMarkedForDeletion(pid)) {
            return false;
        }

        // Check for embargoes if the permission being requested is beyond metadata
        if (!Permission.viewMetadata.equals(permission)) {
            // Check for an active embargo
            Date embargoUntil = aclFactory.getEmbargoUntil(pid);
            if (embargoUntil != null) {
                Date currentDate = new Date();
                if (currentDate.before(embargoUntil)) {
                    return false;
                }
            }
        }

        return true;
    }

    public void setAclFactory(ObjectAclFactory aclFactory) {
        this.aclFactory = aclFactory;
    }
}
