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

import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Premis;

/**
 *
 * @author harring
 *
 */
public class AddContainerService {

    private AccessControlService aclService;
    private RepositoryObjectFactory repoObjFactory;

    /**
     * Mark each pid for deletion using the agent principals provided.
     *
     * @param agent security principals of the agent making request.
     * @param ids ids of objects to mark for deletion
     */
    public void addContainer(AgentPrincipals agent, PID childPid, Resource containerType) {

     // Create the new container
        Model model = ModelFactory.createDefaultModel();
        Resource childResc = model.getResource(childPid.getRepositoryPath());
        ContentContainerObject parent = null;
        // Create the appropriate container
        if (Cdr.AdminUnit.equals(containerType)) {
            aclService.assertHasAccess(
                    "User does not have permissions to create admin units",
                    childPid, agent.getPrincipals(), Permission.createAdminUnit);

            parent = repoObjFactory.createAdminUnit(model);
            repoObjFactory.createMemberLink(parent.getPid().getRepositoryUri(), URI.create(childResc.getURI()));
        } else if (Cdr.Collection.equals(containerType)) {
            aclService.assertHasAccess(
                    "User does not have permissions to create collections",
                    childPid, agent.getPrincipals(), Permission.createCollection);

            parent = repoObjFactory.createCollectionObject(model);
            repoObjFactory.createMemberLink(parent.getPid().getRepositoryUri(), URI.create(childResc.getURI()));
        } else if (Cdr.Folder.equals(containerType)) {
            aclService.assertHasAccess(
                    "User does not have permissions to create collections",
                    childPid, agent.getPrincipals(), Permission.ingest);

            parent = repoObjFactory.createFolderObject(model);
            repoObjFactory.createMemberLink(parent.getPid().getRepositoryUri(), URI.create(childResc.getURI()));
        } else if (Cdr.Work.equals(containerType)) {
            aclService.assertHasAccess(
                    "User does not have permissions to create collections",
                    childPid, agent.getPrincipals(), Permission.ingest);

            parent = repoObjFactory.createWorkObject(model);
            repoObjFactory.createMemberLink(parent.getPid().getRepositoryUri(), URI.create(childResc.getURI()));
        }

        parent.getPremisLog().buildEvent(Premis.Creation)
        .addImplementorAgent(agent.getUsernameUri())
        .addEventDetail("Container added for pid " + childPid)
        .write();

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

}
