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
package edu.unc.lib.dl.cdr.services.rest;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.USER_NAMESPACE;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.dl.acl.fcrepo4.ObjectAclFactory;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.RoleAssignment;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;

/**
 * API controller for retrieving access-control information about an object
 *
 * @author lfarrell
 * @author harring
 *
 */
@Controller
public class AccessControlRetrievalController {
    private static final Logger log = LoggerFactory.getLogger(AccessControlRetrievalController.class);
    public static final String INHERITED_ROLES = "inherited";
    public static final String ASSIGNED_ROLES = "assigned";

    @Autowired
    private AccessControlService aclService;
    @Autowired
    private ObjectAclFactory objectAclFactory;
    @Autowired
    private InheritedAclFactory inheritedAclFactory;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;

    private static final List<String> ROLE_PRECEDENCE = asList(
            UserRole.administrator.getPropertyString(),
            UserRole.unitOwner.getPropertyString(),
            UserRole.canManage.getPropertyString(),
            UserRole.canDescribe.getPropertyString(),
            UserRole.canIngest.getPropertyString(),
            UserRole.canAccess.getPropertyString(),
            UserRole.canViewOriginals.getPropertyString(),
            UserRole.canViewAccessCopies.getPropertyString(),
            UserRole.canViewMetadata.getPropertyString(),
            UserRole.canDiscover.getPropertyString()
            );

    @GetMapping(value = "/acl/staff/{id}", produces = "application/json; charset=UTF-8")
    @ResponseBody
    public ResponseEntity<Object> getStaffRoles(@PathVariable("id") String id) {
        PID pid = PIDs.get(id);

        AgentPrincipals agent = AgentPrincipals.createFromThread();
        aclService.assertHasAccess("Insufficient permissions to retrieve staff roles for " + id,
                pid, agent.getPrincipals(), Permission.viewHidden);

        RepositoryObject repoObj = repoObjLoader.getRepositoryObject(pid);

        Map<String, Object> result = new HashMap<>();
        List<RoleAssignment> inherited = null;
        List<RoleAssignment> assigned = null;

        if (repoObj instanceof AdminUnit) {
            assigned = toRoleAssignmentList(pid,
                    objectAclFactory.getPrincipalRoles(pid), true);
            inherited = Collections.emptyList();
        } else if (repoObj instanceof CollectionObject) {
            assigned = toRoleAssignmentList(pid,
                    objectAclFactory.getPrincipalRoles(pid), true);
            RepositoryObject parent = repoObj.getParent();
            inherited = toRoleAssignmentList(parent.getPid(),
                    deduplicateRoles(objectAclFactory.getPrincipalRoles(parent.getPid())),
                    true);
        } else if (repoObj instanceof ContentObject) {
            assigned = Collections.emptyList();
            inherited = toRoleAssignmentList(pid,
                    deduplicateRoles(inheritedAclFactory.getPrincipalRoles(pid)),
                    true);
        } else {
            result.put("error", "Cannot retrieve staff roles for object " + id
                    + " of type " + repoObj.getClass().getName());
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }

        result.put(INHERITED_ROLES, inherited);
        result.put(ASSIGNED_ROLES, assigned);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Deduplicates role assignments, so that a principal will only have one role.
     * If more than one role is assigned to the same principal, the highest level
     * role will be return.
     *
     * @param princToRoles
     * @return
     */
    private Map<String, Set<String>> deduplicateRoles(Map<String, Set<String>> princToRoles) {
        for (Entry<String, Set<String>> princEntry : princToRoles.entrySet()) {
            Set<String> roles = princEntry.getValue();
            if (roles.size() > 1) {
                for (String preferred : ROLE_PRECEDENCE) {
                    if (roles.contains(preferred)) {
                        princEntry.setValue(new HashSet<>(asList(preferred)));
                        break;
                    }
                }
            }
        }
        return princToRoles;
    }

    private List<RoleAssignment> toRoleAssignmentList(PID pid, Map<String, Set<String>> princToRoles,
            boolean staffRoles) {
        List<RoleAssignment> result = new ArrayList<>();
        princToRoles.forEach((princ, roles) -> {
            for (String roleString: roles) {
                UserRole role = UserRole.getRoleByProperty(roleString);
                if (role == null) {
                    log.warn("Invalid role {} assigned to {}", roleString, pid.getId());
                    continue;
                }
                // Skip over either staff or patrons roles, depending on what is being requested
                if (staffRoles != role.isStaffRole()) {
                    log.debug("Skipping role {} on object {}", roleString, pid.getId());
                    continue;
                }
                // Trim user namespace off of user principals for the response
                if (princ.startsWith(USER_NAMESPACE)) {
                    princ = princ.substring(USER_NAMESPACE.length());
                }
                result.add(new RoleAssignment(princ, role));
            }
        });

        return result;
    }
}