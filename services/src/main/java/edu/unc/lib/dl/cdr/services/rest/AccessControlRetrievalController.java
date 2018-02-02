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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.cdr.services.processing.AccessControlRetrievalService;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.fedora.PID;

/**
 * API controller for retrieving access-control information about an object and its children, if it has any
 *
 * @author lfarrell
 * @author harring
 *
 */
@Controller
public class AccessControlRetrievalController {
    private static final Logger log = LoggerFactory.getLogger(AccessControlRetrievalController.class);

    @Autowired
    private AccessControlRetrievalService aclRetrievalService;

    @RequestMapping(value = "/acl/getPermissions/{uuid}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody ResponseEntity<Object> getAclPermssions(@PathVariable("uuid") String uuid) {

        return getPermissions(uuid);
    }

    private ResponseEntity<Object> getPermissions(String uuid) {
        PID pid = PIDs.get(uuid);

        Map<String, Object> result = new HashMap<>();
        result.put("action", "retrieve acls");
        result.put("pid", pid.getId());

        try {
            result.put("accessControls", aclRetrievalService.getPermissions(AgentPrincipals.createFromThread(), pid));
        } catch (Exception e) {
            result.put("error", e.getMessage());
            if (e instanceof AuthorizationException || e instanceof AccessRestrictionException) {
                return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
            } else {
                log.error("Failed to retrieve access controls for object with pid " + pid, e);
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}