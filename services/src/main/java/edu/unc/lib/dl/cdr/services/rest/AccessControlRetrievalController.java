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
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

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
import edu.unc.lib.dl.acl.util.PatronPrincipalProvider;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.RoleAssignment;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.acl.PatronAccessDetails;

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
    public static final String ALLOWED_ADDITIONAL_PRINCIPALS = "allowedPrincipals";
    public static final String ROLES_KEY = "roles";
    public static final String EMBARGO_KEY = "embargo";
    public static final String DELETED_KEY = "deleted";

    @Autowired
    private AccessControlService aclService;
    @Autowired
    private ObjectAclFactory objectAclFactory;
    @Autowired
    private InheritedAclFactory inheritedAclFactory;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private PatronPrincipalProvider patronPrincipalProvider;

    @GetMapping(value = "/acl/staff/{id}", produces = APPLICATION_JSON_VALUE)
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

        Map<String, Object> inheritedInfo = new HashMap<>();
        inheritedInfo.put(ROLES_KEY, inherited);
        Map<String, Object> assignedInfo = new HashMap<>();
        assignedInfo.put(ROLES_KEY, assigned);

        result.put(INHERITED_ROLES, inheritedInfo);
        result.put(ASSIGNED_ROLES, assignedInfo);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Returns all details that impact patron access, including inherited and
     * directly assigned patron roles, embargoes and deletion status.
     *
     * Inherited roles are computed to reflect inheritance decisions, embargoes and
     * deletions.
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/acl/patron/{id}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getPatronAccess(@PathVariable("id") String id) {
        log.debug("Retrieving patron access details for {}", id);
        PID pid = PIDs.get(id);

        AgentPrincipals agent = AgentPrincipals.createFromThread();
        aclService.assertHasAccess("Insufficient permissions to retrieve patron access for " + id,
                pid, agent.getPrincipals(), Permission.viewHidden);

        RepositoryObject repoObj = repoObjLoader.getRepositoryObject(pid);

        Map<String, Object> result = new HashMap<>();

        if (repoObj instanceof AdminUnit) {
            result.put("error", "Cannot retrieve patron access for a unit");
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        } else if (repoObj instanceof ContentObject) {
            result.put(INHERITED_ROLES, addInheritedPatronInfo(repoObj));
            result.put(ASSIGNED_ROLES, addAssignedPatronInfo(pid));
            result.put(ALLOWED_ADDITIONAL_PRINCIPALS, patronPrincipalProvider.getConfiguredPatronPrincipals());

            return new ResponseEntity<>(result, HttpStatus.OK);
        } else {
            result.put("error", "Cannot retrieve staff roles for object " + id
                    + " of type " + repoObj.getClass().getName());
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }
    }

    private PatronAccessDetails addAssignedPatronInfo(PID pid) {
        PatronAccessDetails assignedInfo = new PatronAccessDetails();
        assignedInfo.setRoles(objectAclFactory.getPatronRoleAssignments(pid));
        assignedInfo.setDeleted(objectAclFactory.isMarkedForDeletion(pid));
        assignedInfo.setEmbargo(objectAclFactory.getEmbargoUntil(pid));

        return assignedInfo;
    }

    private PatronAccessDetails addInheritedPatronInfo(RepositoryObject repoObj) {
        PatronAccessDetails inheritedInfo = new PatronAccessDetails();

        RepositoryObject parent = repoObj.getParent();
        PID pid = parent.getPid();
        if (!(parent instanceof AdminUnit)) {
            inheritedInfo.setRoles(inheritedAclFactory.getPatronAccess(pid));
            inheritedInfo.setEmbargo(inheritedAclFactory.getEmbargoUntil(pid));
        }
        inheritedInfo.setDeleted(inheritedAclFactory.isMarkedForDeletion(pid));

        return inheritedInfo;
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