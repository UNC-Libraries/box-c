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
package edu.unc.lib.boxc.operations.impl.move;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.search.solr.services.ObjectPathFactory;
import edu.unc.lib.dl.fcrepo4.TransactionManager;

/**
 * Service which moves content objects between containers.
 *
 * @author bbpennel
 *
 */
public class MoveObjectsService {
    private static final Logger log = LoggerFactory.getLogger(MoveObjectsService.class);

    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private TransactionManager transactionManager;
    private OperationsMessageSender operationsMessageSender;
    private ObjectPathFactory objectPathFactory;
    private boolean asynchronous;
    private ExecutorService moveExecutor;

    /**
     * Move a list of objects to the destination container as the provided
     * agent.
     *
     * @param agent
     * @param destinationPid
     * @param pids
     */
    public String moveObjects(AgentPrincipals agent, PID destinationPid, List<PID> pids) {
        if (destinationPid == null || pids == null || pids.isEmpty()) {
            throw new IllegalArgumentException("Must provide a destination and at least one object to move.");
        }
        if (agent == null || agent.getUsername() == null || agent.getPrincipals() == null) {
            throw new IllegalArgumentException("Must provide agent identification information");
        }

        // Verify that the destination is a content container
        RepositoryObject destObj = repositoryObjectLoader.getRepositoryObject(destinationPid);
        if (!(destObj instanceof ContentContainerObject)) {
            throw new IllegalArgumentException("Destination " + destinationPid + " was not a content container");
        }
        for (PID pid : pids) {
            verifyValidDestination(destObj, pid);
        }

        MoveObjectsJob job = new MoveObjectsJob(agent, destinationPid, pids);
        job.setAclService(aclService);
        job.setRepositoryObjectLoader(repositoryObjectLoader);
        job.setTransactionManager(transactionManager);
        job.setOperationsMessageSender(operationsMessageSender);
        job.setObjectPathFactory(objectPathFactory);

        if (asynchronous) {
            log.info("User {} is queueing move operation {} of {} objects to destination {}",
                    new Object[] { agent.getUsername(), job.getMoveId(), pids.size(), destinationPid });
            moveExecutor.submit(job);
        } else {
            job.run();
        }

        return job.getMoveId();
    }

    private void verifyValidDestination(RepositoryObject destObj, PID pid) {
        RepositoryObject moveObj = repositoryObjectLoader.getRepositoryObject(pid);
        if (destObj instanceof AdminUnit) {
            if (!(moveObj instanceof CollectionObject)) {
                throw new IllegalArgumentException(
                        "Object with pid: " + pid + " is not a collection and cannot be added to an admin unit");
            }
        } else if (destObj instanceof CollectionObject) {
            if (!(moveObj instanceof FolderObject) && !(moveObj instanceof WorkObject)) {
                throw new IllegalArgumentException(
                        "Object with pid: " + pid + " is not a folder or a work and cannot be added to a collection");
            }
        } else if (destObj instanceof FolderObject) {
            if (!(moveObj instanceof FolderObject) && !(moveObj instanceof WorkObject)) {
                throw new IllegalArgumentException(
                        "Object with pid: " + pid + " is not a folder or a work and cannot be added to a folder");
            }
        } else if (destObj instanceof WorkObject) {
            if (!(moveObj instanceof FileObject)) {
                throw new IllegalArgumentException(
                        "Object with pid: " + pid + " is not a file and cannot be added to a work");
            }
        } else {
            throw new IllegalArgumentException(
                    "Object with pid: " + pid + " cannot be added to destination");
        }

    }

    /**
     * @param aclService the aclService to set
     */
    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    /**
     * @param repositoryObjectLoader the repositoryObjectLoader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    /**
     * @param transactionManager the transactionManager to set
     */
    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * @param asynchronous the asynchronous to set
     */
    public void setAsynchronous(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }

    /**
     * @param operationsMessageSender the operationsMessageSender to set
     */
    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }

    /**
     * @param objectPathFactory the objectPathFactory to set
     */
    public void setObjectPathFactory(ObjectPathFactory objectPathFactory) {
        this.objectPathFactory = objectPathFactory;
    }

    /**
     * @param moveExecutor the moveExecutor to set
     */
    public void setMoveExecutor(ExecutorService moveExecutor) {
        this.moveExecutor = moveExecutor;
    }
}
