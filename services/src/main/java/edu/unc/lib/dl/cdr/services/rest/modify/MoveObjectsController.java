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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.move.MoveObjectsService;

/**
 *
 * @author bbpennel
 *
 */
@Controller
public class MoveObjectsController {
    private static final Logger log = LoggerFactory.getLogger(MoveObjectsController.class);

    @Autowired
    private MoveObjectsService moveObjectsService;

    public MoveObjectsController() {
    }

    @RequestMapping(value = "edit/move", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity<Object> moveObjects(@RequestBody MoveRequest moveRequest) {
        Map<String, Object> results = new HashMap<>();
        // Validate that the request contains the newPath and ids fields.
        if (moveRequest == null || moveRequest.moved == null || moveRequest.moved.size() == 0
                || moveRequest.getDestination() == null || moveRequest.getDestination().length() == 0) {
            results.put("error", "Request must provide a destination destination and a list of ids");
            return new ResponseEntity<>(results, HttpStatus.BAD_REQUEST);
        }

        AgentPrincipals agent = AgentPrincipals.createFromThread();
        PID destPid = PIDs.get(moveRequest.getDestination());
        try {
            List<PID> movePids = moveRequest.getMoved().stream().map(p -> PIDs.get(p)).collect(Collectors.toList());
            String moveId = moveObjectsService.moveObjects(agent, destPid, movePids);

            results.put("id", moveId);
            results.put("message", "Operation to move " + moveRequest.moved.size() + " objects into container "
                    + moveRequest.getDestination() + " has begun");
            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            results.put("error", e.getMessage());
            return new ResponseEntity<>(results, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            results.put("error", "Failed to perform move operation");
            log.error("Failed to perform move for user {}", agent.getUsername(), e);
            return new ResponseEntity<>(results, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static class MoveRequest {
        private String destination;
        private List<String> moved;
        private String user;

        public String getDestination() {
            return destination;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }

        public void setMoved(List<String> moved) {
            this.moved = moved;
        }

        public List<String> getMoved() {
            return moved;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }
    }
}
