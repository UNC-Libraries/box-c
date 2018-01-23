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

import static edu.unc.lib.dl.acl.util.Permission.assignStaffRoles;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.cdr.services.processing.AccessControlRetrievalService;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;

/**
 *
 * @author lfarrell
 *
 */
@Controller
@RequestMapping("/aclRetrieval")
public class AccessControlRetrievalController {
    @Autowired
    private AccessControlRetrievalService aclRetreivaService;

    private AgentPrincipals agent;
    private AccessControlService aclService;

    @RequestMapping(value = "/getPermssions", method = RequestMethod.GET, consumes = "application/JSON", produces = "application/json")
    public @ResponseBody Map<String, Object> getAclPermssions(HttpServletRequest request, HttpServletResponse response) {
        PID pid = PIDs.get(request.getParameter("pid"));

        aclService.assertHasAccess("Insufficient privileges to retrieve permissions for object " + pid.getUUID(),
                pid, agent.getPrincipals(), assignStaffRoles);

        Map<String, Object> objectPermissions = aclRetreivaService.getPermissions(pid);
        Map<String, Object> childPermissions = aclRetreivaService.getChildPermissions(pid);

        Map<String, Object> combinedPermissions = new HashMap<>();
        combinedPermissions.putAll(objectPermissions);
        combinedPermissions.putAll(childPermissions);

        return combinedPermissions;
    }
}