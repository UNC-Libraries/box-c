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

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.operations.impl.delete.MarkForDeletionService;
import edu.unc.lib.dl.fedora.AuthorizationException;

/**
 * API controller for specifying whether or not resources are marked for deletion.
 *
 * @author bbpennel
 *
 */
@Controller
public class MarkForDeletionController {
    private static final Logger log = LoggerFactory.getLogger(MarkForDeletionController.class);

    @Autowired
    private MarkForDeletionService markForDeletionService;

    @RequestMapping(value = "edit/restore/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Object> restore(@PathVariable("id") String id) {
        return update(false, AgentPrincipalsImpl.createFromThread(), null, id);
    }

    @RequestMapping(value = "edit/delete/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Object> markForDeletion(@PathVariable("id") String id,
            @RequestParam("message") String message) {
        return update(true, AgentPrincipalsImpl.createFromThread(), message, id);
    }

    @RequestMapping(value = "edit/restore", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Object> restoreBatch(@RequestParam("ids") String ids) {
        return update(false, AgentPrincipalsImpl.createFromThread(), null, ids.split("\n"));
    }

    @RequestMapping(value = "edit/delete", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Object> markBatchForDeletion(@RequestParam("ids") String ids,
            @RequestParam("message") String message) {
        return update(true, AgentPrincipalsImpl.createFromThread(), message, ids.split("\n"));
    }

    private ResponseEntity<Object> update(boolean markAsDeleted, AgentPrincipals agent, String message, String... ids) {
        Map<String, Object> result = new HashMap<>();

        if (ids.length == 1) {
            result.put("pid", ids[0]);
        } else {
            result.put("pids", ids);
        }
        result.put("action", (markAsDeleted) ? "delete" : "restore");

        try {
            if (markAsDeleted) {
                markForDeletionService.markForDeletion(agent, message, ids);
            } else {
                markForDeletionService.restoreMarked(agent, ids);
            }
        } catch (AccessRestrictionException | AuthorizationException e) {
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
        } catch (FedoraException e) {
            log.error("Failed to update mark for deletion flag to {}", markAsDeleted, e);
            result.put("error", e.toString());
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalArgumentException e) {
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
