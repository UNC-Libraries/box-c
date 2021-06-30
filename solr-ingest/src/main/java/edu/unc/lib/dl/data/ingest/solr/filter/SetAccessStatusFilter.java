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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.objects.AbstractContentObject;
import edu.unc.lib.boxc.model.fcrepo.objects.AdminUnitImpl;
import edu.unc.lib.boxc.model.fcrepo.objects.CollectionObjectImpl;
import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.dl.acl.fcrepo4.ObjectAclFactory;
import edu.unc.lib.dl.acl.util.AccessPrincipalConstants;
import edu.unc.lib.dl.acl.util.RoleAssignment;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
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

        AbstractContentObject contentObj = dip.getContentObject();
        // No need to continue with patron statuses if object is an AdminUnit
        if (contentObj instanceof AdminUnitImpl) {
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

        boolean isCollection = contentObj instanceof CollectionObjectImpl;
        if (hasPatronSettings(objPatronRoles, isCollection)) {
            status.add(FacetConstants.PATRON_SETTINGS);
        }
        if (!isCollection && !isMarkedForDeletion
                && inheritingPatronRestrictions(contentObj, objPatronRoles, inheritedAssignments, isEmbargoed)) {
            status.add(FacetConstants.INHERITED_PATRON_SETTINGS);
        }

        return status;
    }

    private boolean inheritingPatronRestrictions(AbstractContentObject contentObj, List<RoleAssignment> objRoles,
            List<RoleAssignment> inheritedRoles, boolean isEmbargoed) {
        boolean noObjAssignments = objRoles.isEmpty();
        // No roles defined at any level, so inheriting staff only
        if (inheritedRoles.isEmpty() && noObjAssignments) {
            return true;
        }
        UserRole maxInheritedRole = isEmbargoed ? UserRole.canViewMetadata : canViewOriginals;
        // If no assignments on object, then then make decision based off the object's inherited roles
        if (noObjAssignments) {
            return !hasDefaultRoleAssignments(maxInheritedRole, inheritedRoles);
        }
        // Since inherited roles incorporate the object's roles, move up one level to the parent's inherited roles
        PID parentPid = contentObj.getParentPid();
        List<RoleAssignment> parentRoles = inheritedAclFactory.getPatronAccess(parentPid);
        if (parentRoles.size() != AccessPrincipalConstants.PROTECTED_PATRON_PRINCIPALS.size()) {
            return true;
        }
        // Adjust default role depending on if an embargo is being applied
        Date parentEmbargo = inheritedAclFactory.getEmbargoUntil(parentPid);
        boolean parentEmbargoed = isEmbargoActive(parentEmbargo);
        UserRole maxParentRole = parentEmbargoed ? UserRole.canViewMetadata : canViewOriginals;

        return !hasDefaultRoleAssignments(maxParentRole, parentRoles);
    }

    private boolean hasDefaultRoleAssignments(UserRole defaultRole, List<RoleAssignment> roles) {
        // assignments cannot match defaults if there are more or fewer rows than the protected set
        if (roles.size() != AccessPrincipalConstants.PROTECTED_PATRON_PRINCIPALS.size()) {
            return false;
        }
        // Check to see if any protected principals do not have the default role in the provided assignment list
        for (String principal: AccessPrincipalConstants.PROTECTED_PATRON_PRINCIPALS) {
            if (roles.stream().noneMatch(ir -> ir.getPrincipal().equals(principal)
                    && defaultRole.equals(ir.getRole()))) {
                return false;
            }
        }
        return true;
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
