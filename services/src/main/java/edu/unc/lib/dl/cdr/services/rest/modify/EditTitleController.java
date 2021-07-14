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

import java.util.HashMap;
import java.util.Map;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.fcrepo.model.AgentPrincipals;
import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.persist.services.edit.EditTitleService;

/**
 * API controller that updates the dc:title of a given object
 *
 * @author lfarrell
 *
 */
@Controller
public class EditTitleController {
    private static final Logger log = LoggerFactory.getLogger(EditTitleController.class);

    @Autowired
    private EditTitleService service;

    @RequestMapping(value = "/edit/title/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<Object> editLabel(@PathVariable("id") String id, @RequestParam("title") String title) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "editTitle");
        result.put("pid", id);

        PID pid = PIDs.get(id);

        try {
            service.editTitle(AgentPrincipals.createFromThread(), pid, title);
        } catch (Exception e) {
            result.put("error", e.getMessage());

            if (e instanceof AuthorizationException || e instanceof AccessRestrictionException) {
                return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
            } else {
                log.error("Failed to edit title for {}", pid, e);
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
