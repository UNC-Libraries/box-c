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
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.dl.acl.fcrepo4.ObjectAclFactory;
import edu.unc.lib.dl.acl.util.RoleAssignment;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
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
     * @param aclFactory an inherited acl factory
     */
    public void setInheritedAclFactory(InheritedAclFactory iaf) {
        this.inheritedAclFactory = iaf;
    }

    /**
     * Sets non-inherited acl factory
     *
     * @param aclFactory an inherited acl factory
     */
    public void setObjectAclFactory(ObjectAclFactory oaf) {
        this.objAclFactory = oaf;
    }

    private List<String> determineAccessStatus(DocumentIndexingPackage dip)
            throws IndexingException {

        PID pid = dip.getPid();
        List<String> status = new ArrayList<>();

        if (inheritedAclFactory.isMarkedForDeletion(pid)) {
            status.add(FacetConstants.MARKED_FOR_DELETION);
        }

        Date objEmbargo = objAclFactory.getEmbargoUntil(pid);
        Date parentEmbargo = inheritedAclFactory.getEmbargoUntil(pid);
        if (isEmbargoActive(objEmbargo)) {
            status.add(FacetConstants.EMBARGOED);
        } else if (isEmbargoActive(parentEmbargo)) {
            status.add(FacetConstants.EMBARGOED_PARENT);
        }

        // Don't mark as public access if embargoes or deletion were present
        boolean restrictionsApplied = !status.isEmpty();

        List<RoleAssignment> inheritedAssignments = inheritedAclFactory.getPatronAccess(pid);
        Map<String, Set<String>> objPrincRoles = objAclFactory.getPrincipalRoles(pid);

        if (allPatronsRevoked(objPrincRoles)) {
            status.add(FacetConstants.STAFF_ONLY_ACCESS);
        } else if (inheritedAssignments.isEmpty()) {
            status.add(FacetConstants.PARENT_HAS_STAFF_ONLY_ACCESS);
        } else if (!restrictionsApplied && containsAssignment(PUBLIC_PRINC, canViewOriginals, inheritedAssignments)) {
            status.add(FacetConstants.PUBLIC_ACCESS);
        }

        return status;
    }

    private boolean allPatronsRevoked(Map<String, Set<String>> objPrincRoles) {
        Set<String> publicRoles = objPrincRoles.get(PUBLIC_PRINC);
        if (publicRoles == null) {
            return false;
        }
        Set<String> authRoles = objPrincRoles.get(AUTHENTICATED_PRINC);
        if (authRoles == null) {
            return false;
        }

        return publicRoles.contains(UserRole.none.getPropertyString())
                && authRoles.contains(UserRole.none.getPropertyString());
    }

    private boolean containsAssignment(String principal, UserRole role, List<RoleAssignment> assignments) {
        return assignments.stream()
                .anyMatch(p -> p.getPrincipal().equals(principal) && p.getRole().equals(role));
    }
}
