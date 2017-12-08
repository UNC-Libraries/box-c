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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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
import edu.unc.lib.dl.cdr.services.processing.UpdateDescriptionService;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.validation.MetadataValidationException;

/**
 * API controller for updating MODS records
 *
 * @author harring
 *
 */
@Controller
public class UpdateDescriptionController {
    private static final Logger log = LoggerFactory.getLogger(UpdateDescriptionController.class);

    @Autowired
    private UpdateDescriptionService updateService;

    @RequestMapping(value = "edit/description/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Object> updateDescription(@PathVariable("id") String id, HttpServletRequest request) {
        return update(id, request);
    }

    private ResponseEntity<Object> update(String id, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "updateDescription");
        result.put("pid", id);

        PID pid = PIDs.get(id);

        try (InputStream modsStream = request.getInputStream()) {
            updateService.updateDescription(AgentPrincipals.createFromThread(), pid, modsStream);
        } catch (Exception e) {
            log.error("Failed to update MODS: {}",  e);
            result.put("error", e.getMessage());
            if (e instanceof AuthorizationException || e instanceof AccessRestrictionException) {
                return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
            } else if (e instanceof MetadataValidationException) {
                return new ResponseEntity<>(result, HttpStatus.UNPROCESSABLE_ENTITY);
            } else if (e instanceof IllegalArgumentException) {
                return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
            } else {
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
