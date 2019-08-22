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
package edu.unc.lib.dl.cdr.services.rest.modify;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.USER_NAMESPACE;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.exception.InvalidAssignmentException;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.RoleAssignment;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.acl.StaffRoleAssignmentService;

/**
 * API endpoint for setting access control for objects
 *
 * @author bbpennel
 *
 */
@Controller
public class UpdateAccessControlController {
    private static final Logger log = LoggerFactory.getLogger(UpdateAccessControlController.class);

    @Autowired
    private StaffRoleAssignmentService staffRoleService;

    @PutMapping(value = "/edit/acl/staff/{id}", produces = "application/json; charset=UTF-8")
    @ResponseBody
    public ResponseEntity<Object> updateStaffRoles(@PathVariable("id") String id,
            @RequestBody List<RoleAssignment> assignments) {

        PID pid = PIDs.get(id);

        Map<String, Object> result = new HashMap<>();
        result.put("action", "editStaffRoles");
        result.put("pid", pid.getId());

        for (RoleAssignment ra: assignments) {
            // Catch any incomplete role assignments
            if (isEmpty(ra.getPrincipal()) || ra.getRole() == null) {
                result.put("error", "Invalid role assignments");
                return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
            }
            // Expand user principal assignments into uris
            addUserPrefixIfMissing(ra);
        }

        try {
            AgentPrincipals agent = AgentPrincipals.createFromThread();
            String jobId = staffRoleService.updateRoles(agent, pid, assignments);
            result.put("job", jobId);
        } catch (InvalidAssignmentException e) {
            result.put("error", e.getMessage());
            log.debug("Invalid role assignment to {}", pid.getId(), e);
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        } catch (AccessRestrictionException | AuthorizationException e) {
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            result.put("error", "An unexpected problem occurred while setting staff roles");
            log.error("Failed to update staff roles for {}", pid.getId(), e);
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private void addUserPrefixIfMissing(RoleAssignment ra) {
        String principal = ra.getPrincipal();
        if (!principal.matches("\\w+:.+")) {
            ra.setPrincipal(USER_NAMESPACE + principal);
        }
    }
}
