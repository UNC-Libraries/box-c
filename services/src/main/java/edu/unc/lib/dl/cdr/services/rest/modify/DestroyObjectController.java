/**
 * Copyright © 2008 The University of North Carolina at Chapel Hill (cdr@unc.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.services.DigitalObjectManager;

/**
 * API controller which permanently deletes/destroys objects in the repository.
 * 
 * @author bbpennel
 *
 */
@Controller
public class DestroyObjectController {
    private static final Logger log = LoggerFactory.getLogger(DestroyObjectController.class);

    @Autowired(required = true)
    private DigitalObjectManager digitalObjectManager;

    @RequestMapping(value = "edit/destroy/{id}", method = RequestMethod.POST)
    public @ResponseBody
    Map<String, ? extends Object> moveToTrash(@PathVariable("id") String id) {
        PID pid = new PID(id);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("pid", pid);
        result.put("action", "destroy");

        try {
            log.debug("Destroying object {}", id);
            digitalObjectManager.delete(pid, GroupsThreadStore.getUsername(), "Object destroyed by CDR API");
            result.put("timestamp", System.currentTimeMillis());
        } catch (FedoraException e) {
            log.error("Failed to destroy object {}", pid, e);
            result.put("error", e.toString());
        } catch (IngestException e) {
            log.error("Failed to destroy object {}", pid, e);
            result.put("error", e.toString());
        }

        return result;
    }

    public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
        this.digitalObjectManager = digitalObjectManager;
    }
}
