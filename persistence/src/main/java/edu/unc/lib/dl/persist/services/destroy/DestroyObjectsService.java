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
package edu.unc.lib.dl.persist.services.destroy;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;

/**
 * Service that manages the destruction of objects in the repository and their replacement by tombstones
 *
 * @author harring
 *
 */
public class DestroyObjectsService {

    private static final Logger log = LoggerFactory.getLogger(DestroyObjectsService.class);
    @Autowired
    private AccessControlService aclService;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;

    /**
     * Checks whether the active user has permissions to destroy the listed objects,
     * and then kicks off a job to destroy the permitted ones asynchronously
     *
     * @param agent security principals of the agent making request
     * @param ids list of objects to destroy
     */
    public void destroyObjects(AgentPrincipals agent, List<String> ids) {
        List<PID> objsToDestroy = new ArrayList<>();

        for (String id : ids) {
            PID pid = PIDs.get(id);
            if (repoObjLoader.getRepositoryObject(pid) instanceof AdminUnit) {
                aclService.assertHasAccess("User does not have permission to destroy admin unit", pid,
                        agent.getPrincipals(), Permission.destroyUnit);
            } else {
                aclService.assertHasAccess("User does not have permission to destroy this object", pid,
                        agent.getPrincipals(), Permission.destroy);
            }
            objsToDestroy.add(pid);
        }
        if (!objsToDestroy.isEmpty()) {
            DestroyObjectsJob job = new DestroyObjectsJob(objsToDestroy);
            log.info("Destroy job for {} objects started by {}", objsToDestroy.size(), agent.getUsernameUri());
            job.run();
        }
    }

    /**
     * @param aclService the aclService to set
     */
    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    /**
     * @param repoObjLoader the object loader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

}
