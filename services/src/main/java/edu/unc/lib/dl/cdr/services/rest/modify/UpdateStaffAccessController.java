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
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

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

import edu.unc.lib.dl.acl.exception.InvalidAssignmentException;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.RoleAssignment;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.acl.StaffRoleAssignmentService;

/**
 * API endpoint for setting access control for objects
 *
 * @author bbpennel
 *
 */
@Controller
public class UpdateStaffAccessController {
    private static final Logger log = LoggerFactory.getLogger(UpdateStaffAccessController.class);

    @Autowired
    private StaffRoleAssignmentService staffRoleService;

    @PutMapping(value = "/edit/acl/staff/{id}", produces = APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public ResponseEntity<Object> updateStaffRoles(@PathVariable("id") String id,
            @RequestBody UpdateStaffRequest assignments) {

        PID pid = PIDs.get(id);

        Map<String, Object> result = new HashMap<>();
        result.put("action", "editStaffRoles");
        result.put("pid", pid.getId());

        for (RoleAssignment ra: assignments.getRoles()) {
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
            String jobId = staffRoleService.updateRoles(agent, pid, assignments.getRoles());
            result.put("job", jobId);
        } catch (InvalidAssignmentException e) {
            result.put("error", e.getMessage());
            log.debug("Invalid role assignment to {}", pid.getId(), e);
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
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

    public static class UpdateStaffRequest {
        private List<RoleAssignment> roles;

        public List<RoleAssignment> getRoles() {
            return roles;
        }

        public void setRoles(List<RoleAssignment> roles) {
            this.roles = roles;
        }
    }
}
