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

import static edu.unc.lib.dl.acl.util.PrincipalClassifier.classifyPrincipals;
import static edu.unc.lib.dl.acl.util.PrincipalClassifier.getPatronPrincipals;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.exceptions.OrphanedObjectException;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fedora.ContentPathFactory;
import edu.unc.lib.dl.fedora.PID;

/**
 * Evaluates the permissions of agents on specific objects, taking into account
 * inherited access control information
 *
 * @author bbpennel
 *
 */
public class InheritedPermissionEvaluator {

    private static final int UNIT_PATH_DEPTH = 1;
    private static final int COLLECTION_PATH_DEPTH = 2;
    private static final int CONTENT_STARTING_DEPTH = 3;

    private ContentPathFactory pathFactory;

    private ObjectAclFactory objectAclFactory;

    /**
     * Returns true if the given principals are granted the specified permission
     * on the object identified by the given PID, using the following
     * inheritance model:
     *
     * Patron roles inherit from the parent collection until reduced or removed
     * by patron access settings on the ancestors or the object itself. Patron
     * access is always granted on a Unit.
     *
     * Staff roles inherit from the point of definition, which can occur on
     * either the Unit or Collection in a path.
     *
     * @param target
     * @param agentPrincipals
     * @param permission
     * @return
     */
    public boolean hasPermission(PID target, Set<String> agentPrincipals, Permission permission) {

        // Separate agents into patron and staff agents
        Set<String> agentPatronPrincipals = new HashSet<>();
        Set<String> agentStaffPrincipals = new HashSet<>();
        classifyPrincipals(agentPrincipals, agentPatronPrincipals, agentStaffPrincipals);

        // Retrieve the path of objects up to and including the target
        List<PID> path = getObjectPath(target);

        if (hasStaffPermission(path, agentStaffPrincipals, permission)) {
            return true;
        }
        // Perform additional processing for patron permissions
        if (isPatronPermission(permission)) {
            return hasPatronPermission(path, agentPatronPrincipals, permission);
        } else {
            return false;
        }
    }

    private boolean hasStaffPermission(List<PID> path, Set<String> agentStaffPrincipals, Permission permission) {
        Set<String> rolesWithPermission = getRolesWithPermission(permission);

        for (int depth = UNIT_PATH_DEPTH; depth < path.size(); depth++) {
            PID pathPid = path.get(depth);

            // Only consider the first two levels (unit and collection)
            if (depth < CONTENT_STARTING_DEPTH) {
                Map<String, Set<String>> princRoles = objectAclFactory.getPrincipalRoles(pathPid);
                if (hasRoleWithPermission(princRoles, agentStaffPrincipals, rolesWithPermission)) {
                    return true;
                }
            } else {
                return false;
            }
        }

        return false;
    }

    /**
     * Evaluate if any of the agent's patron principals have been granted the specified
     * permission within the given object path.
     *
     * @param path
     * @param agentPatronPrincipals
     * @param permission
     * @return
     */
    private boolean hasPatronPermission(List<PID> path, Set<String> agentPatronPrincipals,
            Permission permission) {

        // Patron permissions don't apply to units
        if (path.size() <= UNIT_PATH_DEPTH + 1) {
            return true;
        }

        // Determine which roles would grant the sought after permission
        Set<String> rolesWithPermission = getRolesWithPermission(permission);
        Set<String> activePatronPrincipals = null;

        // Ignore embargoes if the agent is requesting metadata
        boolean ignoreEmbargoes = Permission.viewMetadata.equals(permission);

        for (int depth = COLLECTION_PATH_DEPTH; depth < path.size(); depth++) {
            PID pathPid = path.get(depth);

            // Deny permission if the object is deleted or embargoed
            if (objectAclFactory.isMarkedForDeletion(pathPid) ||
                    (!ignoreEmbargoes && hasActiveEmbargo(pathPid))) {
                return false;
            }

            Map<String, Set<String>> princRoles = objectAclFactory.getPrincipalRoles(pathPid);
            // kickstart active patron principals with collection granted principals that overlap with the agent
            if (depth == COLLECTION_PATH_DEPTH) {
                activePatronPrincipals = getPatronPrincipals(princRoles.keySet());
                activePatronPrincipals.retainAll(agentPatronPrincipals);

                // no active patrons, so permission cannot be granted
                if (activePatronPrincipals.isEmpty()) {
                    return false;
                }
            }

            // Remove any active patron principals that do not grant the permission
            revokePatronPermissions(princRoles, activePatronPrincipals, rolesWithPermission);

            if (activePatronPrincipals.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Removes entries from activePrincipals if the principal is defined a role in princRoles
     * which is not in the set of roles granting the sought permission.
     *
     * @param princRoles
     * @param activePrincipals
     * @param rolesWithPermission
     */
    private void revokePatronPermissions(Map<String, Set<String>> princRoles, Set<String> activePrincipals,
            Set<String> rolesWithPermission) {
        Iterator<String> activeIt = activePrincipals.iterator();
        while (activeIt.hasNext()) {
            String princ = activeIt.next();

            Set<String> roles = princRoles.get(princ);
            // If no new role assignment for active principal, then it is not revoked.
            if (roles == null) {
                continue;
            }
            // None of the roles for this principal have this permission, so it was revoked
            if (rolesWithPermission.stream().noneMatch(roles::contains)) {
                activeIt.remove();
            }
        }
    }

    /**
     * Determines if any principals in agentPrincipals are assigned a role in princRoles which
     * are known to grant the permission being sought.
     *
     * @param princRoles
     * @param agentPrincipals
     * @param rolesWithPermission
     * @return
     */
    private boolean hasRoleWithPermission(Map<String, Set<String>> princRoles, Set<String> agentPrincipals,
            Set<String> rolesWithPermission) {
        for (String staffPrinc: agentPrincipals) {
            Set<String> roles = princRoles.get(staffPrinc);
            if (roles == null) {
                continue;
            }
            if (rolesWithPermission.stream().anyMatch(roles::contains)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> getRolesWithPermission(Permission permission) {
        return UserRole.getUserRolesWithPermission(permission)
                .stream()
                .map(UserRole::getPropertyString)
                .collect(Collectors.toSet());
    }

    private boolean hasActiveEmbargo(PID pid) {
        Date embargoUntil = objectAclFactory.getEmbargoUntil(pid);
        if (embargoUntil != null) {
            Date currentDate = new Date();
            if (currentDate.before(embargoUntil)) {
                return true;
            }
        }
        return false;
    }

    private List<PID> getObjectPath(PID pid) {
        List<PID> path = pathFactory.getAncestorPids(pid);
        if (path.indexOf(RepositoryPaths.getContentRootPid()) != 0) {
            throw new OrphanedObjectException("Cannot evaluate permissions for orphaned object " + pid.getId());
        }
        // Add the target to the end of the path so it will be evaluated too
        path.add(pid);

        return path;
    }

    private boolean isPatronPermission(Permission permission) {
        return permission.equals(Permission.viewMetadata)
                || permission.equals(Permission.viewAccessCopies)
                || permission.equals(Permission.viewOriginal);
    }

    public void setPathFactory(ContentPathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }

    /**
     * @param objectAclFactory the objectAclFactory to set
     */
    public void setObjectAclFactory(ObjectAclFactory objectAclFactory) {
        this.objectAclFactory = objectAclFactory;
    }
}
