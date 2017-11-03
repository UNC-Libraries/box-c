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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.FedoraTransaction;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Premis;

/**
 * Service that manages the creation of containers
 *
 * @author harring
 *
 */
public class AddContainerService {

    private AccessControlService aclService;
    private RepositoryObjectFactory repoObjFactory;
    private RepositoryObjectLoader repoObjLoader;
    private TransactionManager txManager;

    /**
     * Creates a new container as a child of the given parent using the agent principals provided.
     *
     * @param agent security principals of the agent making request.
     * @param parentPid pid of parent obj to add child container to
     * @param containerType the type of new container to be created
     */
    public void addContainer(AgentPrincipals agent, PID parentPid, Resource containerType) {

        FedoraTransaction tx = txManager.startTransaction();
        try {
            // Create the new container
            Model model = ModelFactory.createDefaultModel();
            model.createResource(parentPid.getRepositoryPath());
            ContentContainerObject child = null;
            // Create the appropriate container
            if (Cdr.AdminUnit.equals(containerType)) {
                aclService.assertHasAccess(
                        "User does not have permissions to create admin units",
                        parentPid, agent.getPrincipals(), Permission.createAdminUnit);
                child = repoObjFactory.createAdminUnit(model);
            } else if (Cdr.Collection.equals(containerType)) {
                aclService.assertHasAccess(
                        "User does not have permissions to create collections",
                        parentPid, agent.getPrincipals(), Permission.createCollection);
                child = repoObjFactory.createCollectionObject(model);
            } else if (Cdr.Folder.equals(containerType)) {
                aclService.assertHasAccess(
                        "User does not have permissions to create folders",
                        parentPid, agent.getPrincipals(), Permission.ingest);
                child = repoObjFactory.createFolderObject(model);
            } else if (Cdr.Work.equals(containerType)) {
                aclService.assertHasAccess(
                        "User does not have permissions to create works",
                        parentPid, agent.getPrincipals(), Permission.ingest);
                child = repoObjFactory.createWorkObject(model);
            }

            ContentContainerObject parent = (ContentContainerObject ) repoObjLoader.getRepositoryObject(parentPid);
            parent.addMember(child);

            child.getPremisLog().buildEvent(Premis.Creation)
            .addImplementorAgent(agent.getUsernameUri())
            .addEventDetail("Container added at destination " + parentPid)
            .write();
        } catch(Exception e) {
            tx.cancel(e);
        } finally {
            tx.close();
        }

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

}
