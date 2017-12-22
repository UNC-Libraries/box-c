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

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.cdr.services.processing.IndexingService;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.fedora.PID;

/**
 * API controller for reindexing the repository
 *
 * @author bbpennel
 * @author harring
 *
 */
@Controller
public class IndexingController {
    private static final Logger log = LoggerFactory.getLogger(IndexingController.class);

    @Autowired
    private IndexingService indexingService;

    /**
     * Perform a deep reindexing operation on the object with the specified id and all of its children.
     *
     * @param id the identifier of the object to be reindexed
     * @param inplace whether the reindex should be an in-place recursive reindex (optional)
     * @return
     */
    @RequestMapping(value = "edit/solr/reindex/{id}", method = RequestMethod.POST)
    public ResponseEntity<Object> reindex(@PathVariable("id") String id,
            @RequestParam(value = "inplace", required = false) Boolean inplace) {
        return indexObjectAndChildren(id, inplace);
    }

    /**
     * Perform a shallow reindexing of the object specified by id
     *
     * @param id the identifier of the object to be reindexed
     * @return
     */
    @RequestMapping(value = "edit/solr/update/{id}", method = RequestMethod.POST)
    public ResponseEntity<Object> reindex(@PathVariable("id") String id) {
        return indexObject(id);
    }

    private ResponseEntity<Object> indexObject(String id) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "reindex");
        result.put("pid", id);

        PID objectPid = PIDs.get(id);

        log.info("Indexing object " + id);
        try {
            indexingService.reindexObject(AgentPrincipals.createFromThread(), objectPid);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            if (e instanceof AuthorizationException || e instanceof AccessRestrictionException) {
                return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
            } else {
                log.error("Failed to reindex object {}", id, e);
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private ResponseEntity<Object> indexObjectAndChildren(String id, Boolean inplace) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "reindex");
        result.put("pid", id);

        PID objectPid = PIDs.get(id);

        if (inplace == null || inplace) {
            log.info("Reindexing " + id + ", inplace reindex mode");
        } else {
            log.info("Reindexing " + id + ", clean reindex mode");
        }
        try {
            indexingService.reindexObjectAndChildren(AgentPrincipals.createFromThread(), objectPid, inplace);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            if (e instanceof AuthorizationException || e instanceof AccessRestrictionException) {
                return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
            } else {
                log.error("Failed to reindex object {} and its children", id, e);
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
