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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.persist.services.acl.PatronAccessAssignmentService;
import edu.unc.lib.dl.persist.services.acl.PatronAccessAssignmentService.PatronAccessAssignmentRequest;
import edu.unc.lib.dl.persist.services.acl.PatronAccessDetails;
import edu.unc.lib.dl.persist.services.acl.PatronAccessOperationSender;

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
    @Qualifier("patronAccessOperationSender")
    private PatronAccessOperationSender patronAccessOperationSender;
    @Autowired
    private AccessControlService aclService;

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
            processUpdate(agent, pid, accessDetails, false);
            result.put("status", "Submitted patron access update for " + pid.getId());
        } catch (ServiceException e) {
            result.put("error", e.getMessage());
            log.debug("Invalid access assignment to {}", pid.getId(), e);
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            log.error("Failed to request patron access update for {}", pid.getId(), e);
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private void processUpdate(AgentPrincipals agent, PID pid, PatronAccessDetails accessDetails,
            boolean skipEmbargo) throws IOException {
        aclService.assertHasAccess("Insufficient privileges to assign patron roles for object " + pid.getId(),
                pid, agent.getPrincipals(), Permission.changePatronAccess);
        PatronAccessAssignmentService.assertAssignmentsComplete(accessDetails);
        PatronAccessAssignmentService.assertOnlyPatronRoles(accessDetails);

        patronAccessOperationSender.sendUpdateRequest(new PatronAccessAssignmentRequest(
                agent, pid, accessDetails).withSkipEmbargo(skipEmbargo));
    }

    @PutMapping(value = "/edit/acl/patron", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> updatePatronAccess(@RequestBody BulkPatronAccessDetails bulkAccessDetails) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "editPatronAccess");
        if (CollectionUtils.isEmpty(bulkAccessDetails.getIds()) || bulkAccessDetails.getAccessDetails() == null) {
            result.put("error", "Request must includes ids and access details");
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }

        AgentPrincipals agent = AgentPrincipals.createFromThread();
        List<PID> pids = bulkAccessDetails.getIds().stream().map(PIDs::get).collect(Collectors.toList());
        int count = 0;
        try {
            for (PID pid : pids) {
                processUpdate(agent, pid, bulkAccessDetails.getAccessDetails(), bulkAccessDetails.isSkipEmbargo());
                count++;
            }
        } catch (ServiceException e) {
            result.put("error", e.getMessage());
            log.debug("Invalid access assignment to {}, {} out of {} updates were submitted",
                    pids.get(count), count, pids.size(), e);
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            log.error("Failed to request patron access update for {}, {} out of {} updates were submitted",
                    pids.get(count), count, pids.size(), e);
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        result.put("status", "Submitted patron access update for " + pids.size() + " object(s)");
        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    public static class BulkPatronAccessDetails {
        private List<String> ids;
        private PatronAccessDetails accessDetails;
        private Boolean skipEmbargo;

        public List<String> getIds() {
            return ids;
        }

        public void setIds(List<String> ids) {
            this.ids = ids;
        }

        public PatronAccessDetails getAccessDetails() {
            return accessDetails;
        }

        public void setAccessDetails(PatronAccessDetails accessDetails) {
            this.accessDetails = accessDetails;
        }

        public boolean isSkipEmbargo() {
            return skipEmbargo;
        }

        public void setSkipEmbargo(boolean skipEmbargo) {
            this.skipEmbargo = skipEmbargo;
        }
    }
}
