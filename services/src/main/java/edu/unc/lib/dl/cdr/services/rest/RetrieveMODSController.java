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

import java.io.IOException;

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
import edu.unc.lib.dl.cdr.services.processing.MODSRetrievalService;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.fedora.PID;

/**
 * An API controller for retrieving an object's MODS description, if available
 *
 * @author harring
 *
 */
@Controller
public class RetrieveMODSController {
    private static final Logger log = LoggerFactory.getLogger(RetrieveMODSController.class);

    @Autowired
    private MODSRetrievalService modsService;

    @RequestMapping(value = "/description/{id}", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<String> retrieveMODS(@PathVariable("id") String id) throws IOException {

        PID pid = PIDs.get(id);

        AgentPrincipals agent = AgentPrincipals.createFromThread();

        String modsString;
        try {
            modsString = modsService.retrieveMODS(agent, pid);
            if (modsString == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            if (e instanceof AuthorizationException || e instanceof AccessRestrictionException) {
                log.warn("User {} does not have permission to view metadata for {}", agent.getUsername(), pid);
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            } else {
                log.error("Failed to retrieve MODS for object with pid {}", pid, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<String>(modsString, HttpStatus.OK);
    }
}
