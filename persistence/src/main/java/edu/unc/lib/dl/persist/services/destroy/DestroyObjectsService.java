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

import org.fcrepo.client.FcrepoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.search.solr.service.ObjectPathFactory;

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
    private RepositoryObjectFactory repoObjFactory;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private TransactionManager txManager;
    @Autowired
    private ObjectPathFactory pathFactory;
    @Autowired
    private FcrepoClient fcrepoClient;

    /**
     * Checks whether the active user has permissions to destroy the listed objects,
     * and then kicks off a job to destroy the permitted ones asynchronously
     *
     * @param agent security principals of the agent making request
     * @param ids list of objects to destroy
     */
    public void destroyObjects(AgentPrincipals agent, String... ids) {
        List<PID> objsToDestroy = new ArrayList<>();

        for (String id : ids) {
            PID pid = PIDs.get(id);
            RepositoryObject obj = repoObjLoader.getRepositoryObject(pid);
            if (obj instanceof AdminUnit) {
                aclService.assertHasAccess("User does not have permission to destroy admin unit", pid,
                        agent.getPrincipals(), Permission.destroyUnit);
            } else {
                aclService.assertHasAccess("User does not have permission to destroy this object", pid,
                        agent.getPrincipals(), Permission.destroy);
            }
            if (obj.getResource().hasProperty(CdrAcl.markedForDeletion)) {
                objsToDestroy.add(pid);
            }
        }
        if (!objsToDestroy.isEmpty()) {
            DestroyObjectsJob job = initializeJob(objsToDestroy);
            log.info("Destroy job for {} objects started by {}", objsToDestroy.size(), agent.getUsernameUri());
            job.run();
        } else {
            log.info("Destroy job submitted by {} provided no eligable candidates, skipping", agent.getUsernameUri());
        }
    }

    private DestroyObjectsJob initializeJob(List<PID> objsToDestroy) {
        DestroyObjectsJob job = new DestroyObjectsJob(objsToDestroy);
        job.setFcrepoClient(fcrepoClient);
        job.setPathFactory(pathFactory);
        job.setRepoObjFactory(repoObjFactory);
        job.setRepoObjLoader(repoObjLoader);
        job.setTransactionManager(txManager);

        return job;
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

    /**
     *
     * @param repoObjFactory the repository object factory to set
     */
    public void setRepositoryObjectFactory(RepositoryObjectFactory repoObjFactory) {
        this.repoObjFactory = repoObjFactory;
    }

    /**
     *
     * @param txManager the transaction manager to set
     */
    public void setTransactionManager(TransactionManager txManager) {
        this.txManager = txManager;
    }

    /**
     *
     * @param pathFactory the path factory to set
     */
    public void setPathFactory(ObjectPathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }

    /**
     *
     * @param fcrepoClient the fcrepo client to set
     */
    public void setFcrepoClient(FcrepoClient fcrepoClient) {
        this.fcrepoClient = fcrepoClient;
    }

}
