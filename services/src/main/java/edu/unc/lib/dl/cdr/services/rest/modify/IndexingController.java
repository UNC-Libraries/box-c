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

import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupConstants;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 * 
 * @author bbpennel
 *
 */
@Controller
public class IndexingController {
    private static final Logger log = LoggerFactory.getLogger(IndexingController.class);

    @Autowired
    private OperationsMessageSender operationsMessageSender;
    @Autowired
    private AccessControlService accessControlService;

    /**
     * Perform a deep reindexing operation on the specified id and all of its children.
     * 
     * @param id
     * @param inplace
     * @return
     */
    @RequestMapping(value = "edit/solr/reindex/{id}", method = RequestMethod.POST)
    public void reindex(@PathVariable("id") String id,
            @RequestParam(value = "inplace", required = false) Boolean inplace, HttpServletResponse response) {
        PID pid = new PID(id);

        if (!hasPermission(id)) {
            response.setStatus(401);
            return;
        }

        if (inplace == null || inplace) {
            log.info("Reindexing " + id + ", inplace reindex mode");
            operationsMessageSender.sendIndexingOperation(GroupsThreadStore.getUsername(), Arrays.asList(pid),
                    IndexingActionType.RECURSIVE_REINDEX);
        } else {
            log.info("Reindexing " + id + ", clean reindex mode");
            operationsMessageSender.sendIndexingOperation(GroupsThreadStore.getUsername(), Arrays.asList(pid),
                    IndexingActionType.CLEAN_REINDEX);
        }
    }

    /**
     * Perform a shallow reindexing of the object specified by id
     * 
     * @param id
     * @param response
     * @return
     */
    @RequestMapping(value = "edit/solr/update/{id}", method = RequestMethod.POST)
    public void reindex(@PathVariable("id") String id, HttpServletResponse response) {
        PID pid = new PID(id);

        if (!hasPermission(id)) {
            response.setStatus(401);
            return;
        }

        log.info("Updating " + id);
        operationsMessageSender.sendIndexingOperation(GroupsThreadStore.getUsername(),
                Arrays.asList(pid), IndexingActionType.ADD);
    }

    private boolean hasPermission(String id) {
        // Disallow requests by users that are not at least curators for pid
        AccessGroupSet groups = GroupsThreadStore.getGroups();
        if (log.isDebugEnabled()) {
            log.debug("hasPermission for groups " + groups.joinAccessGroups(";"));
        }
        return groups.contains(AccessGroupConstants.ADMIN_GROUP);
    }

    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }

    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }
}
