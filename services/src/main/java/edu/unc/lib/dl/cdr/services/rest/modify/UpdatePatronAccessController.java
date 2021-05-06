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

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.HashMap;
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
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.persist.services.acl.PatronAccessAssignmentService;
import edu.unc.lib.dl.persist.services.acl.PatronAccessAssignmentService.PatronAccessAssignmentRequest;
import edu.unc.lib.dl.persist.services.acl.PatronAccessDetails;

/**
 * API endpoint for setting patron access control for objects
 *
 * @author bbpennel
 *
 */
@Controller
public class UpdatePatronAccessController {
    private static final Logger log = LoggerFactory.getLogger(UpdatePatronAccessController.class);

    @Autowired
    private PatronAccessAssignmentService patronService;

    @PutMapping(value = "/edit/acl/patron/{id}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> updatePatronAccess(@PathVariable("id") String id,
            @RequestBody PatronAccessDetails accessDetails) {

        PID pid = PIDs.get(id);

        Map<String, Object> result = new HashMap<>();
        result.put("action", "editPatronAccess");
        result.put("pid", pid.getId());

        try {
            AgentPrincipals agent = AgentPrincipals.createFromThread();
            String jobId = patronService.updatePatronAccess(
                    new PatronAccessAssignmentRequest(agent, pid, accessDetails, false));
            if (jobId == null) {
                result.put("status", "No changes made");
            } else {
                result.put("job", jobId);
            }
        } catch (ServiceException | InvalidAssignmentException e) {
            result.put("error", e.getMessage());
            log.debug("Invalid access assignment to {}", pid.getId(), e);
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
