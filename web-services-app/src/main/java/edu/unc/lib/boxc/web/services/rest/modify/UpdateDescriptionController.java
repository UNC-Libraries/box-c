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
package edu.unc.lib.boxc.web.services.rest.modify;

import java.io.IOException;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.exceptions.MetadataValidationException;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;

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

    @PostMapping(value = "edit/description/{id}")
    @ResponseBody
    public ResponseEntity<Object> updateDescription(@PathVariable("id") String id, HttpServletRequest request) {
        return update(id, request);
    }

    private ResponseEntity<Object> update(String id, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "updateDescription");
        result.put("pid", id);

        PID pid = PIDs.get(id);

        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        try (InputStream modsStream = request.getInputStream()) {
            updateService.updateDescription(new UpdateDescriptionRequest(agent, pid, modsStream));
        } catch (MetadataValidationException e) {
            if (e.getMessage() != null) {
                result.put("error", e.getMessage());
            }
            return new ResponseEntity<>(result, HttpStatus.UNPROCESSABLE_ENTITY);
        } catch (IOException e) {
            log.error("Failed to parse input", e);
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
