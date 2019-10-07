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

import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.dl.acl.util.RoleAssignment;
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

        List<String> status = new ArrayList<>();
        Set<String> readPrincipals = new HashSet<>();
        Set<String> staffPrincipals = new HashSet<>();
        List<String> roleGroups = new ArrayList<>();

        List<RoleAssignment> staffAssignments = aclFactory.getStaffRoleAssignments(dip.getPid());
        staffAssignments.forEach(assignment -> {
            addRoleGroup(roleGroups, assignment);
            readPrincipals.add(assignment.getPrincipal());
            staffPrincipals.add(assignment.getPrincipal());
        });

        List<RoleAssignment> patronAssignments = aclFactory.getPatronAccess(dip.getPid());
        patronAssignments.forEach(assignment -> {
            addRoleGroup(roleGroups, assignment);
            readPrincipals.add(assignment.getPrincipal());
        });

        idb.setRoleGroup(roleGroups);
        idb.setAdminGroup(new ArrayList<>(staffPrincipals));
        idb.setReadGroup(new ArrayList<>(readPrincipals));
        idb.setStatus(status);
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
