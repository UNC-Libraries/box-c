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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @GetMapping(value = "/acl/staff/{id}", produces = "application/json; charset=UTF-8")
    @ResponseBody
    public ResponseEntity<Object> getStaffRoles(@PathVariable("id") String id) {
        log.debug("Retrieving staff roles for {}", id);
        PID pid = PIDs.get(id);

        AgentPrincipals agent = AgentPrincipals.createFromThread();
        aclService.assertHasAccess("Insufficient permissions to retrieve staff roles for " + id,
                pid, agent.getPrincipals(), Permission.viewHidden);

        RepositoryObject repoObj = repoObjLoader.getRepositoryObject(pid);

        Map<String, Object> result = new HashMap<>();
        List<RoleAssignment> inherited = null;
        List<RoleAssignment> assigned = null;

        if (repoObj instanceof AdminUnit) {
            assigned = objectAclFactory.getStaffRoleAssignments(pid);
            inherited = Collections.emptyList();
        } else if (repoObj instanceof CollectionObject) {
            assigned = objectAclFactory.getStaffRoleAssignments(pid);
            RepositoryObject parent = repoObj.getParent();
            inherited = inheritedAclFactory.getStaffRoleAssignments(parent.getPid());
        } else if (repoObj instanceof ContentObject) {
            assigned = Collections.emptyList();
            inherited = inheritedAclFactory.getStaffRoleAssignments(pid);
        } else {
            result.put("error", "Cannot retrieve staff roles for object " + id
                    + " of type " + repoObj.getClass().getName());
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }

        stripUserPrefix(inherited);
        stripUserPrefix(assigned);

        result.put(INHERITED_ROLES, inherited);
        result.put(ASSIGNED_ROLES, assigned);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    // Trim user namespace off of user principals for the response
    private void stripUserPrefix(List<RoleAssignment> assignments) {
        for (RoleAssignment assignment: assignments) {
            String princ = assignment.getPrincipal();
            if (princ.startsWith(USER_NAMESPACE)) {
                princ = princ.substring(USER_NAMESPACE.length());
                assignment.setPrincipal(princ);
            }
        }
    }
}