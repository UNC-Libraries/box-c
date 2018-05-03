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
package edu.unc.lib.dl.cdr.services.processing;

import java.util.Arrays;

import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.FedoraTransaction;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.services.OperationsMessageSender;
import io.dropwizard.metrics5.Timer;

/**
 * Service that manages the creation of containers
 *
 * @author harring
 *
 */
public class AddContainerService {
    private static final Logger log = LoggerFactory.getLogger(AddContainerService.class);

    private AccessControlService aclService;
    private RepositoryObjectFactory repoObjFactory;
    private RepositoryObjectLoader repoObjLoader;
    private TransactionManager txManager;
    private OperationsMessageSender operationsMessageSender;

    private static final Timer timer = TimerFactory.createTimerForClass(AddContainerService.class);

    /**
     * Creates a new container as a child of the given parent using the agent principals provided.
     *
     * @param agent security principals of the agent making request.
     * @param parentPid pid of parent obj to add child container to
     * @param containerType the type of new container to be created
     */
    public void addContainer(AgentPrincipals agent, PID parentPid, Resource containerType) {
        ContentContainerObject child = null;
        FedoraTransaction tx = txManager.startTransaction();

        try (Timer.Context context = timer.time()) {
            // Create the appropriate container
            if (Cdr.AdminUnit.equals(containerType)) {
                aclService.assertHasAccess(
                        "User does not have permissions to create admin units",
                        parentPid, agent.getPrincipals(), Permission.createAdminUnit);
                child = repoObjFactory.createAdminUnit(null);
            } else if (Cdr.Collection.equals(containerType)) {
                aclService.assertHasAccess(
                        "User does not have permissions to create collections",
                        parentPid, agent.getPrincipals(), Permission.createCollection);
                child = repoObjFactory.createCollectionObject(null);
            } else if (Cdr.Folder.equals(containerType)) {
                aclService.assertHasAccess(
                        "User does not have permissions to create folders",
                        parentPid, agent.getPrincipals(), Permission.ingest);
                child = repoObjFactory.createFolderObject(null);
            } else if (Cdr.Work.equals(containerType)) {
                aclService.assertHasAccess(
                        "User does not have permissions to create works",
                        parentPid, agent.getPrincipals(), Permission.ingest);
                child = repoObjFactory.createWorkObject(null);
            }

            ContentContainerObject parent = (ContentContainerObject) repoObjLoader.getRepositoryObject(parentPid);
            parent.addMember(child);

            child.getPremisLog()
            .buildEvent(Premis.Creation)
            .addImplementorAgent(agent.getUsernameUri())
            .addEventDetail("Container added at destination " + parentPid)
            .write();
        } catch (Exception e) {
            tx.cancel(e);
        } finally {
            tx.close();
        }

        // Send message that the action completed
        operationsMessageSender.sendAddOperation(agent.getUsername(), Arrays.asList(parentPid),
                Arrays.asList(child.getPid()), null, null);
    }

    /**
     * @param aclService the aclService to set
     */
    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    /**
     * @param repoObjFactory the factory to set
     */
    public void setRepositoryObjectFactory(RepositoryObjectFactory repoObjFactory) {
        this.repoObjFactory = repoObjFactory;
    }

    /**
     * @param repoObjLoader the object loader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    /**
     * @param txManager the transaction manager to set
     */
    public void setTransactionManager(TransactionManager txManager) {
        this.txManager = txManager;
    }

    /**
     *
     * @param operationsMessageSender
     */
    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }

}
