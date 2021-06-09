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
package edu.unc.lib.dl.data.ingest.solr.filter;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.acl.util.EmbargoUtil.isEmbargoActive;
import static edu.unc.lib.dl.acl.util.UserRole.canViewOriginals;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.dl.acl.fcrepo4.ObjectAclFactory;
import edu.unc.lib.dl.acl.util.AccessPrincipalConstants;
import edu.unc.lib.dl.acl.util.RoleAssignment;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.util.FacetConstants;

/**
 * Sets access-related status tags
 *
 * @author harring
 *
 */
public class SetAccessStatusFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetAccessStatusFilter.class);
    private InheritedAclFactory inheritedAclFactory;
    private ObjectAclFactory objAclFactory;

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        log.debug("Performing set access status filter on {}", dip.getPid());
        dip.getDocument().setStatus(determineAccessStatus(dip));
    }

    /**
     * Sets inherited acl factory
     *
     * @param iaf an inherited acl factory
     */
    public void setInheritedAclFactory(InheritedAclFactory iaf) {
        this.inheritedAclFactory = iaf;
    }

    /**
     * Sets non-inherited acl factory
     *
     * @param oaf an inherited acl factory
     */
    public void setObjectAclFactory(ObjectAclFactory oaf) {
        this.objAclFactory = oaf;
    }

    private List<String> determineAccessStatus(DocumentIndexingPackage dip)
            throws IndexingException {

        PID pid = dip.getPid();
        List<String> status = new ArrayList<>();

        boolean isMarkedForDeletion = inheritedAclFactory.isMarkedForDeletion(pid);
        if (isMarkedForDeletion) {
            status.add(FacetConstants.MARKED_FOR_DELETION);
        }

        // No need to continue with patron statuses if object is an AdminUnit
        if (dip.getContentObject() instanceof AdminUnit) {
            return status;
        }

        Date objEmbargo = objAclFactory.getEmbargoUntil(pid);
        boolean isEmbargoed = isEmbargoActive(objEmbargo);
        if (isEmbargoed) {
            status.add(FacetConstants.EMBARGOED);
        } else {
            Date parentEmbargo = inheritedAclFactory.getEmbargoUntil(pid);
            if (isEmbargoActive(parentEmbargo)) {
                status.add(FacetConstants.EMBARGOED_PARENT);
                isEmbargoed = true;
            }
        }

        // Don't mark as public access if embargoes or deletion were present
        boolean restrictionsApplied = !status.isEmpty();

        List<RoleAssignment> inheritedAssignments = inheritedAclFactory.getPatronAccess(pid);
        List<RoleAssignment> objPatronRoles = objAclFactory.getPatronRoleAssignments(pid);

        boolean isDirectlyStaffOnly = allPatronsRevoked(objPatronRoles);
        if (isDirectlyStaffOnly) {
            status.add(FacetConstants.STAFF_ONLY_ACCESS);
        } else if (!hasPatronAssignments(inheritedAssignments)) {
            status.add(FacetConstants.PARENT_HAS_STAFF_ONLY_ACCESS);
        } else if (!restrictionsApplied && containsAssignment(PUBLIC_PRINC, canViewOriginals, inheritedAssignments)) {
            status.add(FacetConstants.PUBLIC_ACCESS);
        }

        boolean isCollection = dip.getContentObject() instanceof CollectionObject;
        if (hasPatronSettings(objPatronRoles, isCollection)) {
            status.add(FacetConstants.PATRON_SETTINGS);
        }
        if (!isCollection && !isMarkedForDeletion && !isDirectlyStaffOnly
                && inheritingPatronRestrictions(objPatronRoles, inheritedAssignments, isEmbargoed)) {
            status.add(FacetConstants.INHERITED_PATRON_RESTRICTIONS);
        }

        return status;
    }

    private boolean inheritingPatronRestrictions(List<RoleAssignment> objRoles, List<RoleAssignment> inheritedRoles,
            boolean isEmbargoed) {
        // No roles defined any any level, so inheriting staff only
        if (inheritedRoles.isEmpty() && objRoles.isEmpty()) {
            return true;
        }
        UserRole maxInheritedRole = isEmbargoed ? UserRole.canViewMetadata : canViewOriginals;
        Set<String> encounteredPrincipals = new HashSet<>();
        // Determine if any inherited roles are more restrictive than direct object roles
        for (RoleAssignment inherited: inheritedRoles) {
            encounteredPrincipals.add(inherited.getPrincipal());
            // Inherited role is maxed, so cannot be restricting
            if (maxInheritedRole.equals(inherited.getRole())) {
                continue;
            }
            Optional<RoleAssignment> objMatchOpt = objRoles.stream()
                    .filter(o -> o.getPrincipal().equals(inherited.getPrincipal())).findFirst();
            // No role defined for the principal at the object level, so the inherited role is restricting it
            if (!objMatchOpt.isPresent()) {
                return true;
            }
            // Check if the inherited role is lower precedence (more restrictive) than the direct role
            UserRole objRole = objMatchOpt.get().getRole();
            if (UserRole.PATRON_ROLE_PRECEDENCE.indexOf(inherited.getRole().getPropertyString())
                    < UserRole.PATRON_ROLE_PRECEDENCE.indexOf(objRole.getPropertyString())) {
                return true;
            }
        }
        // Catch cases where role is not present in inherited set, but is present for object. This implies
        // that the inherited permission is staff only, but the object is set to something less restrictive
        for (RoleAssignment objRole: objRoles) {
            encounteredPrincipals.add(objRole.getPrincipal());
            // If object is set to none role, inherited can't be more restrictive
            if (UserRole.none.equals(objRole.getRole())) {
                continue;
            }
            // No inherited role for principal, which implies it is staff only and therefore restricting
            if (inheritedRoles.stream().noneMatch(i -> i.getPrincipal().equals(objRole.getPrincipal()))) {
                return true;
            }
        }
        // Check if any protected patron principals are missing, indicating that they are restricted
        if (!encounteredPrincipals.containsAll(AccessPrincipalConstants.PROTECTED_PATRON_PRINCIPALS)) {
            return true;
        }
        return false;
    }

    private boolean hasPatronSettings(List<RoleAssignment> objPatronRoles, boolean isCollection) {
        int numRoles = objPatronRoles.size();

        if (!isCollection && numRoles == 0) {
            return false;
        } else if (isCollection && numRoles < 2) {
            return true;
        } else if (numRoles > 2) {
            return true;
        }

        for (RoleAssignment role : objPatronRoles) {
            if (!role.getRole().equals(canViewOriginals) || !isProtectedPatronPrincipal(role.getPrincipal())) {
                return true;
            }
        }

        return false;
    }

    private boolean allPatronsRevoked(List<RoleAssignment> objPatronRoles) {
        // If either public or authenticated principals aren't present, then can't be total revoke
        long protectedPrincipalCount = objPatronRoles.stream()
            .filter(ra -> isProtectedPatronPrincipal(ra.getPrincipal()))
            .count();
        if (protectedPrincipalCount < 2) {
            return false;
        }
        // If any principals have a role other than 'none', then not a total revocation
        for (RoleAssignment roleAssignment : objPatronRoles) {
            if (!UserRole.none.equals(roleAssignment.getRole())) {
                return false;
            }
        }
        return true;
    }

    private boolean isProtectedPatronPrincipal(String principal) {
        return PUBLIC_PRINC.equals(principal) || AUTHENTICATED_PRINC.equals(principal);
    }

    private boolean containsAssignment(String principal, UserRole role, List<RoleAssignment> assignments) {
        return assignments.stream()
                .anyMatch(p -> p.getPrincipal().equals(principal) && p.getRole().equals(role));
    }

    private boolean hasPatronAssignments(List<RoleAssignment> assignments) {
        return assignments.stream().anyMatch(a -> a.getRole().isPatronRole());
    }
}
