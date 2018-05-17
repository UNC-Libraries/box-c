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
import java.util.List;
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
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.persist.services.destroy.DestroyObjectsService;

/**
 * API controller for destroying objects
 *
 * @author harring
 *
 */
@Controller
public class DestroyObjectsController {
    private static final Logger log = LoggerFactory.getLogger(DestroyObjectsController.class);

    @Autowired
    private DestroyObjectsService service;

    @RequestMapping(value = "edit/destroy/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Object> destroyObjects(@PathVariable("id") List<String> ids) {
        Map<String, Object> result = new HashMap<>();
        result.put("object ids", ids.toString());
        result.put("action", "destroy");

        service.destroyObjects(AgentPrincipals.createFromThread(), ids);

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }



}
