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
import java.util.Map;

import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.cdr.services.processing.AddContainerService;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;

/**
 * API controller for creating new containers
 *
 * @author harring
 *
 */
@Controller
public class AddContainerController {
    private static final Logger log = LoggerFactory.getLogger(AddContainerController.class);

    @Autowired
    private AddContainerService addContainerService;

    @RequestMapping(value = "edit/create/adminUnit/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Object> createAdminUnit(@PathVariable("id") String id, @RequestParam("label") String label) {
        return createContainer(id, label, Cdr.AdminUnit);
    }

    @RequestMapping(value = "edit/create/collection/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Object> createCollection(@PathVariable("id") String id, @RequestParam("label") String label) {
        return createContainer(id, label, Cdr.Collection);
    }

    @RequestMapping(value = "edit/create/folder/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Object> createFolder(@PathVariable("id") String id, @RequestParam("label") String label) {
        return createContainer(id, label, Cdr.Folder);
    }

    @RequestMapping(value = "edit/create/work/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Object> createWork(@PathVariable("id") String id, @RequestParam("label") String label) {
        return createContainer(id, label, Cdr.Work);
    }

    private ResponseEntity<Object> createContainer(String id, String label, Resource containerType) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "create");
        result.put("pid", id);

        PID parentPid = PIDs.get(id);

        try {
            addContainerService.addContainer(AgentPrincipals.createFromThread(), parentPid, label, containerType);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            Throwable t = e.getCause();
            if (t instanceof AuthorizationException || t instanceof AccessRestrictionException) {
                return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
            } else {
                log.error("Failed to create container for {}",  e);
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
