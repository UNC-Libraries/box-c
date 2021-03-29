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

import static edu.unc.lib.dl.acl.util.EmbargoUtil.isEmbargoActive;
import static edu.unc.lib.dl.acl.util.PrincipalClassifier.getPatronPrincipals;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.AccessPrincipalConstants;
import edu.unc.lib.dl.acl.util.RoleAssignment;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fedora.ContentPathFactory;
import edu.unc.lib.dl.fedora.PID;

/**
 * Factory which provides access control details that apply to particular
 * objects, either via direct definition or inherited from parent objects
 *
 * @author bbpennel
 *
 */
public class InheritedAclFactory implements AclFactory {
    private static final Logger log = LoggerFactory.getLogger(InheritedAclFactory.class);

    private static final int UNIT_PATH_DEPTH = 0;
    private static final int COLLECTION_PATH_DEPTH = 1;
    private static final int CONTENT_STARTING_DEPTH = 2;

    private ObjectAclFactory objectAclFactory;

    private ContentPathFactory pathFactory;

    private static final List<String> PATRON_ROLE_PRECEDENCE = asList(
            UserRole.none.getPropertyString(),
            UserRole.canViewMetadata.getPropertyString(),
            UserRole.canViewAccessCopies.getPropertyString(),
            UserRole.canViewOriginals.getPropertyString()
            );

    private static final int EMBARGO_ROLE_PRECEDENCE = 1;

    @Override
    public Map<String, Set<String>> getPrincipalRoles(PID target) {

        // Retrieve the path of objects up to and including the target
        List<PID> path = getPidPath(target);

        Map<String, Set<String>> inheritedPrincRoles = new HashMap<>();
        Set<String> customPatronPrincs = null;

        // Iterate through each step in the path except for the root content node
        int depth = 0;
        for (; depth < path.size(); depth++) {
            PID pathPid = path.get(depth);

            Map<String, Set<String>> objectPrincipalRoles = objectAclFactory.getPrincipalRoles(pathPid);

            // For the first two objects (unit, collection), staff roles should be considered
            if (depth < CONTENT_STARTING_DEPTH) {
                // Add this object's principals/roles to the result
                mergePrincipalRoles(inheritedPrincRoles, objectPrincipalRoles);
            }
            if (depth >= COLLECTION_PATH_DEPTH) {
                // No patron assignments with permissions inherited, nothing further may be added
                Set<String> inheritedPatronPrincipals = getPatronPrincipals(inheritedPrincRoles.keySet());
                if (!hasActivePatronRole(inheritedPatronPrincipals, inheritedPrincRoles)) {
                    return removeNoneRoles(inheritedPrincRoles);
                }

                if (depth == COLLECTION_PATH_DEPTH) {
                    customPatronPrincs = getCustomPatronPrincs(objectPrincipalRoles);
                }

                // Apply any further patron restrictions to inherited patron principals
                adjustPatronPrincipalRoles(pathPid, inheritedPrincRoles,
                        inheritedPatronPrincipals, objectPrincipalRoles, customPatronPrincs);
            }
        }

        // Units cannot be assigned patron roles, but have an assumed non-inheritable everyone permission
        // Checking for unit depth + 1 since the counter will be one past the object checked
        if (depth == UNIT_PATH_DEPTH + 1) {
            Set<String> roles = new HashSet<>();
            roles.add(UserRole.canViewOriginals.getPropertyString());
            inheritedPrincRoles.put(AccessPrincipalConstants.PUBLIC_PRINC, roles);
        }

        return inheritedPrincRoles;
    }

    private Set<String> getCustomPatronPrincs(Map<String, Set<String>> objectPrincipalRoles) {
        Set<String> princs = new HashSet<>(objectPrincipalRoles.keySet());
        princs.remove(AccessPrincipalConstants.PUBLIC_PRINC);
        princs.remove(AccessPrincipalConstants.AUTHENTICATED_PRINC);
        return princs;
    }

    @Override
    public List<RoleAssignment> getStaffRoleAssignments(PID pid) {
        return getRoleAssignments(pid, true);
    }

    @Override
    public List<RoleAssignment> getPatronRoleAssignments(PID pid) {
        return getRoleAssignments(pid, false);
    }

    private List<RoleAssignment> getRoleAssignments(PID pid, boolean retrieveStaffRoles) {
        List<PID> path = getPidPath(pid);

        if (retrieveStaffRoles) {
            return path.stream()
                    .limit(2) // Only need to check the first two tiers for staff
                    .flatMap(pathPid -> objectAclFactory.getStaffRoleAssignments(pathPid).stream())
                    .collect(Collectors.toList());
        } else {
            return path.stream()
                    .flatMap(pathPid -> objectAclFactory.getPatronRoleAssignments(pathPid).stream())
                    .collect(Collectors.toList());
        }
    }

    private void mergePrincipalRoles(Map<String, Set<String>> basePrincRoles, Map<String,
            Set<String>> addPrincRoles) {
        if (basePrincRoles.isEmpty()) {
            basePrincRoles.putAll(addPrincRoles);
        } else {
            // Add to inherited data.  If principals overlap, extra roles are added (not overridden)
            addPrincRoles.forEach((principal, roles) -> {
                if (basePrincRoles.containsKey(principal)) {
                    basePrincRoles.get(principal).addAll(roles);
                } else {
                    basePrincRoles.put(principal, roles);
                }
            });
        }
    }

    private boolean hasActivePatronRole(Set<String> patronPrincipals, Map<String, Set<String>> inheritedPrincRoles) {
        if (patronPrincipals.isEmpty()) {
            return false;
        }

        return patronPrincipals.stream()
            .anyMatch(p -> !inheritedPrincRoles.get(p).contains(UserRole.none.getPropertyString()));
    }

    private void adjustPatronPrincipalRoles(PID pid,
            Map<String, Set<String>> inheritedPrincRoles,
            Set<String> inheritedPatronPrincipals,
            Map<String, Set<String>> objectPrincipalRoles,
            Set<String> customPatronPrincs) {

        // Discard any non-patron assignments at this depth
        Set<String> objPatronPrincipals = getPatronPrincipals(objectPrincipalRoles.keySet());
        // Discard principals that aren't also being inherited
        objPatronPrincipals.retainAll(inheritedPatronPrincipals);

        // No new patron assignments for principal we are following, skip
        if (objPatronPrincipals.isEmpty()) {
            return;
        }

        // if both regular patron groups explicitly None, strip away inherited custom groups unless directly added
        if (customPatronPrincs.size() > 0) {
            boolean publicNone = isAssignedNone(AccessPrincipalConstants.PUBLIC_PRINC, objectPrincipalRoles);
            boolean authNone = isAssignedNone(AccessPrincipalConstants.AUTHENTICATED_PRINC, objectPrincipalRoles);
            if (publicNone && authNone) {
                customPatronPrincs.stream()
                        .filter(princ -> !objectPrincipalRoles.containsKey(princ))
                        .forEach(inheritedPrincRoles::remove);
            }
        }

        // If any incoming patron roles are stricter than inherited roles, then override
        for (String patronPrincipal: objPatronPrincipals) {
            Set<String> inheritedRoles = inheritedPrincRoles.get(patronPrincipal);
            Set<String> objRoles = objectPrincipalRoles.get(patronPrincipal);

            if (inheritedRoles.size() > 1 || objRoles.size() > 1) {
                log.warn("Principal {} is assigned multiple roles on object {}", patronPrincipal, pid.getId());
            }

            // Determine if the incoming role is more restrictive than the inherited role for this principal
            String objRole = objRoles.iterator().next();
            // Permission revoked, remove the principal
            if (UserRole.none.equals(objRole)) {
                inheritedPrincRoles.remove(patronPrincipal);
                continue;
            }

            String inherited = inheritedRoles.iterator().next();
            int inheritedIndex = PATRON_ROLE_PRECEDENCE.indexOf(inherited);
            int objIndex = PATRON_ROLE_PRECEDENCE.indexOf(objRole);

            if (objIndex < inheritedIndex) {
                inheritedPrincRoles.put(patronPrincipal, objRoles);
            }
        }
    }

    private boolean isAssignedNone(String principal, Map<String, Set<String>> principalRoles) {
        Set<String> roles = principalRoles.get(principal);
        if (roles == null) {
            return false;
        }
        return roles.contains(UserRole.none.getPropertyString());
    }

    private Map<String, Set<String>> removeNoneRoles(Map<String, Set<String>> princRoles) {
        Iterator<Entry<String, Set<String>>> it = princRoles.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Set<String>> assignment = it.next();
            if (assignment.getValue().contains(UserRole.none.getPropertyString())) {
                it.remove();
            }
        }
        return princRoles;
    }

    @Override
    public List<RoleAssignment> getPatronAccess(PID pid) {
        if (isMarkedForDeletion(pid)) {
            return Collections.emptyList();
        }

        boolean isEmbargoed = isEmbargoActive(getEmbargoUntil(pid));

        Map<String, Set<String>> princRoles = getPrincipalRoles(pid);

        List<RoleAssignment> result = new ArrayList<>();
        Set<String> patronPrincipals = getPatronPrincipals(princRoles.keySet());
        for (String princ: patronPrincipals) {
            Set<String> roles = princRoles.get(princ);
            String roleString = roles.iterator().next();
            UserRole role = UserRole.getRoleByProperty(roleString);

            if (isEmbargoed) {
                int objIndex = PATRON_ROLE_PRECEDENCE.indexOf(roleString);
                if (objIndex > EMBARGO_ROLE_PRECEDENCE) {
                    role = UserRole.canViewMetadata;
                }
            }

            result.add(new RoleAssignment(princ, role));
        }

        return result;
    }

    @Override
    public Date getEmbargoUntil(PID target) {
        return getPidPath(target).stream()
                .map(p -> objectAclFactory.getEmbargoUntil(p))
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    @Override
    public boolean isMarkedForDeletion(PID target) {
        return getPidPath(target).stream()
                .anyMatch(p -> objectAclFactory.isMarkedForDeletion(p));
    }

    private List<PID> getPidPath(PID pid) {
        List<PID> path = pathFactory.getAncestorPids(pid);
        // Looked up either the root or a unit, yielding no ancestors
        if (path.isEmpty()) {
            return Collections.emptyList();
        }
        // Trim off the root object from the beginning of the path
        path = new ArrayList<>(path.subList(1, path.size()));
        path.add(pid);

        return path;
    }

    public void setObjectAclFactory(ObjectAclFactory objectAclFactory) {
        this.objectAclFactory = objectAclFactory;
    }

    public void setPathFactory(ContentPathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }
}
