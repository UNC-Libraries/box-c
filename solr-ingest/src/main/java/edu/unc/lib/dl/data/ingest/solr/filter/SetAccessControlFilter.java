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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.dl.acl.util.UserRole;
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

    private final Set<String> staffRoleNames;

    /**
     * Construct an access control filter
     */
    public SetAccessControlFilter() {
        staffRoleNames = UserRole.getStaffRoles().stream()
                .map(r -> r.getPropertyString())
                .collect(Collectors.toSet());
    }

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        log.debug("Performing set access control filter on {}", dip.getPid());

        IndexDocumentBean idb = dip.getDocument();

        List<String> status = new ArrayList<>();
        List<String> readPrincipals = new ArrayList<>();
        List<String> staffPrincipals = new ArrayList<>();
        Map<String, Set<String>> principalRoles = aclFactory.getPrincipalRoles(dip.getPid());

        List<String> denormalizedRolePrincipals = new ArrayList<>();
        principalRoles.forEach((principal, roles) -> {
            // Populate role -> principal field
            roles.stream()
                .map(role -> UserRole.getRoleByProperty(role))
                .filter(role -> role != null)
                .map(role -> (role.name() + "|" + principal))
                .forEach(denormalizedRolePrincipals::add);

            // Populate list of principals with read permissions with all roles
            if (roles.stream()
                    .findAny().isPresent()) {
                readPrincipals.add(principal);
            }

            // Populate list of principals with staff permissions
            if (roles.stream()
                    .filter(r -> staffRoleNames.contains(r))
                    .findAny().isPresent()) {
                staffPrincipals.add(principal);
            }
        });

        idb.setRoleGroup(denormalizedRolePrincipals);
        idb.setAdminGroup(staffPrincipals);
        idb.setReadGroup(readPrincipals);
        idb.setStatus(status);
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
