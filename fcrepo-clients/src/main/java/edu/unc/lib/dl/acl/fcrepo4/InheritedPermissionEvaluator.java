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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.unc.lib.dl.acl.util.Permission;
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

    private final static int UNIT_PATH_DEPTH = 1;
    private final static int COLLECTION_PATH_DEPTH = 2;
    private final static int CONTENT_STARTING_DEPTH = 3;

    private ContentPathFactory pathFactory;

    private ObjectPermissionEvaluator objectPermissionEvaluator;

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

        boolean requestingPatronPermission = isPatronPermission(permission);

        // Retrieve the path of objects up to and including the target
        List<PID> path = getObjectPath(target);

        Set<String> permittedPatronPrincipals = null;
        // Start on the second entry in the path, the first real object
        for (int depth = UNIT_PATH_DEPTH; depth < path.size(); depth++) {
            PID pathPid = path.get(depth);

            // For the first two objects (unit, collection), staff roles should be considered
            if (depth < CONTENT_STARTING_DEPTH) {
                if (objectPermissionEvaluator.hasStaffPermission(pathPid, agentStaffPrincipals, permission)) {
                    // Staff permission granted, evaluation complete.
                    return true;
                }

                // For collections, evaluate assigned patron permissions
                if (depth == COLLECTION_PATH_DEPTH && requestingPatronPermission) {
                    permittedPatronPrincipals = objectPermissionEvaluator.getPatronPrincipalsWithPermission(
                            pathPid, agentPatronPrincipals, permission);

                    // No principals were found to have the requested permission
                    if (permittedPatronPrincipals.size() == 0) {
                        return false;
                    }
                }
            } else {
                // Finished evaluating staff roles, evaluate patron rights
                if (!requestingPatronPermission
                        || !objectPermissionEvaluator.hasPatronAccess(
                                pathPid, permittedPatronPrincipals, permission)) {
                    // Patron access revoked or permission requested is a staff permission
                    return false;
                }
            }
        }

        // Evaluated all objects in the path, and the permission requested was a
        // patron permission, so grant permission
        return requestingPatronPermission;
    }

    private List<PID> getObjectPath(PID pid) {
        List<PID> path = new ArrayList<>(pathFactory.getAncestorPids(pid));
        // TODO prevent further processing if the object was an orphan
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

    public void setObjectPermissionEvaluator(ObjectPermissionEvaluator objectPermissionEvaluator) {
        this.objectPermissionEvaluator = objectPermissionEvaluator;
    }
}
