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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.auth.api.AccessPrincipalConstants;
import edu.unc.lib.boxc.auth.fcrepo.models.RoleAssignment;
import edu.unc.lib.boxc.auth.fcrepo.services.InheritedAclFactory;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

/**
 * Filter which sets access control related fields for a document.
 *
 * @author bbpennel
 *
 */
public class SetAccessControlFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetAccessControlFilter.class);

    private InheritedAclFactory aclFactory;

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        log.debug("Performing set access control filter on {}", dip.getPid());

        IndexDocumentBean idb = dip.getDocument();

        Set<String> readPrincipals = new HashSet<>();
        Set<String> staffPrincipals = new HashSet<>();
        List<String> roleGroups = new ArrayList<>();

        long start = System.nanoTime();
        List<RoleAssignment> staffAssignments = aclFactory.getStaffRoleAssignments(dip.getPid());
        staffAssignments.forEach(assignment -> {
            addRoleGroup(roleGroups, assignment);
            readPrincipals.add(assignment.getPrincipal());
            staffPrincipals.add(assignment.getPrincipal());
        });
        log.debug("Staff role assignments for {} retrieved in {}",
                idb.getPid(), System.nanoTime() - start);

        // Grant visibility to the collections object
        if (dip.getContentObject() instanceof ContentRootObject
                || dip.getContentObject() instanceof AdminUnit) {
            staffPrincipals.add(AccessPrincipalConstants.ADMIN_ACCESS_PRINC);
            readPrincipals.add(AccessPrincipalConstants.PUBLIC_PRINC);
        } else {
            // Retrieve patron settings for all other object types
            start = System.nanoTime();
            List<RoleAssignment> patronAssignments = aclFactory.getPatronAccess(dip.getPid());
            patronAssignments.forEach(assignment -> {
                addRoleGroup(roleGroups, assignment);
                readPrincipals.add(assignment.getPrincipal());
            });
            log.debug("Patron role assignments for {} retrieved in {}",
                    idb.getPid(), System.nanoTime() - start);
        }

        idb.setRoleGroup(roleGroups);
        idb.setAdminGroup(new ArrayList<>(staffPrincipals));
        idb.setReadGroup(new ArrayList<>(readPrincipals));
    }

    private void addRoleGroup(List<String> roleGroups, RoleAssignment assignment) {
        roleGroups.add(assignment.getRole().name() + "|" + assignment.getPrincipal());
    }

    /**
     * Set acl factory
     *
     * @param aclFactory an inherited acl factory
     */
    public void setAclFactory(InheritedAclFactory aclFactory) {
        this.aclFactory = aclFactory;
    }
}
