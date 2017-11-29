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
package edu.unc.lib.dl.admin.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fedora.PID;
import net.greghaines.jesque.client.Client;

/**
 * @author bbpennel
 * @date Aug 14, 2015
 */
@Controller
public class RunEnhancementsController {

    @Autowired
    private Client jesqueClient;
    @Autowired
    private String runEnhancementQueueName;
    @Autowired
    private AccessControlService aclService;

    @RequestMapping(value = "runEnhancements", method = RequestMethod.POST, produces = "application/json")
    public @ResponseBody Map<String, Object> reviewJSON(@RequestBody RunEnhancementsRequest data,
            HttpServletRequest request, HttpServletResponse response) {

        Map<String, Object> result = new HashMap<>();

        // Check that the user has permission to all requested objects
        AccessGroupSet groups = GroupsThreadStore.getGroups();
        for (String pid : data.getPids()) {
            if (!aclService.hasAccess(new PID(pid), groups, Permission.editAccessControl)) {
                result.put("error", "Insufficient permissions to perform operation");
                return result;
            }
        }

//        Job job = new Job(RunEnhancementsTreeJob.class.getName(), data.getPids(), data.isForce());
//        jesqueClient.enqueue(runEnhancementQueueName, job);
//
//        result.put("message", "Enhancement of " + data.getPids().size()
//                + " object(s) and their children has begun");
//
//        result.put("success", true);
        return result;
    }

    public static class RunEnhancementsRequest {
        private List<String> pids;
        private boolean force;

        public List<String> getPids() {
            return pids;
        }

        public void setPids(List<String> pids) {
            this.pids = pids;
        }

        public boolean isForce() {
            return force;
        }

        public void setForce(boolean force) {
            this.force = force;
        }

    }
}
