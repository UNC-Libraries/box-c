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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.fedora.AuthorizationException;
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

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.cdr.services.processing.RunEnhancementsService;

/**
 * @author bbpennel
 * @author lfarrell
 */
@Controller
public class RunEnhancementsController {
    private static final Logger log = LoggerFactory.getLogger(RunEnhancementsController.class);

    @Autowired
    private RunEnhancementsService enhService;

    @RequestMapping(value = "runEnhancements", method = RequestMethod.POST, produces = "application/json")
    public @ResponseBody ResponseEntity<Object> runEnhancements(@RequestBody RunEnhancementsRequest data,
            HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();

        try {
            enhService.run(GroupsThreadStore.getAgentPrincipals(), data.getPids(), data.isForce());
            result.put("message", "Enhancement of " + data.getPids().size()
                    + " object(s) and their children has begun");
            result.put("action", "runEnhancements");
        } catch (Exception e) {
            result.put("message", "Unable to run enhancements of " + data.getPids().size()
                    + " object(s) and their children");
            result.put("error", e.getMessage());
            if (e instanceof AuthorizationException || e instanceof AccessRestrictionException) {
                return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
            } else {
                log.error("Failed to run enhancements for {}", data.getPids().toString(), e);
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    public static class RunEnhancementsRequest {
        private ArrayList<HashMap> pids;
        private boolean force;

        public ArrayList<HashMap> getPids() {
            return pids;
        }

        public boolean isForce() {
            return force;
        }
    }
}
