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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.update.UpdateException;

/**
 * @author bbpennel
 * @date Jun 26, 2015
 */
@Controller
public class EditDefaultWebObjectController {

    private static final Logger log = LoggerFactory.getLogger(EditDefaultWebObjectController.class);

    @Autowired
    private DigitalObjectManager dom;

    @RequestMapping(value = "edit/editDefaultWebObject", method = RequestMethod.POST)
    public @ResponseBody Object editDefaultWebObject(@RequestBody EditDWORequest editRequest,
            HttpServletResponse response) {

        Map<String, Object> results = new HashMap<>();

        EditDWORunnable editDWO = new EditDWORunnable(editRequest);
        editRequest.setUser(GroupsThreadStore.getUsername());
        editRequest.setGroupSet(GroupsThreadStore.getGroups());
        Thread editThread = new Thread(editDWO);
        editThread.start();

        results.put("message", "Operation to change the primary access object for  " + editRequest.getPids().size()
                + " objects has begun");

        response.setStatus(200);
        return results;
    }

    public static class EditDWORequest {
        private List<PID> pids;
        private String user;
        private AccessGroupSet groupSet;
        private Boolean clear = false;

        public void setPids(List<String> pids) {
            this.pids = new ArrayList<PID>(pids.size());
            for (String id : pids) {
                this.pids.add(new PID(id));
            }
        }

        public List<PID> getPids() {
            return this.pids;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public AccessGroupSet getGroupSet() {
            return groupSet;
        }

        public void setGroupSet(AccessGroupSet groupSet) {
            this.groupSet = groupSet;
        }

        public Boolean getClear() {
            return clear;
        }

        public void setClear(Boolean clear) {
            this.clear = clear;
        }
    }

    private class EditDWORunnable implements Runnable {

        private final EditDWORequest editRequest;

        public EditDWORunnable(EditDWORequest editRequest) {
            this.editRequest = editRequest;
        }

        @Override
        public void run() {
            Long start = System.currentTimeMillis();

            try {
                GroupsThreadStore.storeGroups(editRequest.getGroupSet());
                GroupsThreadStore.storeUsername(editRequest.getUser());

                dom.editDefaultWebObject(editRequest.getPids(), editRequest.getClear(), editRequest.getUser());
            } catch (UpdateException e) {
                log.warn("Failed to change default web object", e);
            } finally {
                GroupsThreadStore.clearStore();
            }

            log.info("Finished changing default web object for {} object(s) in {}ms", editRequest.pids.size(),
                    (System.currentTimeMillis() - start));
        }

    }
}
