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

import static edu.unc.lib.dl.acl.util.Permission.editResourceType;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.InvalidOperationForObjectType;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Premis;

/**
 * Service that manages setting a primary object on a work
 *
 * @author harring
 *
 */
public class SetPrimaryObjectService {

    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private RepositoryObjectFactory repoObjFactory;

    /**
     * Sets file object with the given pid as the primary object for its work using the agent principals provided.
     *
     * @param agent security principals of the agent making request.
     * @param fileObjPid the file object to be set as primary object
     */
    public void setPrimaryObject(AgentPrincipals agent, PID fileObjPid) {

        aclService.assertHasAccess("Insufficient privileges to set " + fileObjPid.getUUID() + " as primary object",
                fileObjPid, agent.getPrincipals(), editResourceType);

        RepositoryObject repoObj = repoObjLoader.getRepositoryObject(fileObjPid);

        if (!(repoObj instanceof FileObject)) {
            throw new InvalidOperationForObjectType("Cannot set " + fileObjPid.getUUID()
                    + " as primary object, since objects of type " + repoObj.getClass().getName()
                    + " are not eligible.");
        }

        RepositoryObject parent = repoObj.getParent();
        if(!(parent instanceof WorkObject)) {
            throw new InvalidOperationForObjectType("Object of type " + parent.getClass().getName()
            + " cannot have a primary object.");
        }
        repoObjFactory.createRelationship(parent.getPid(), Cdr.primaryObject, parent.getModel()
                .createResource(fileObjPid.getRepositoryPath()));

        repoObj.getPremisLog()
        .buildEvent(Premis.Creation)
        .addImplementorAgent(agent.getUsernameUri())
        .addEventDetail(fileObjPid + " set as primary object of " + parent.getPid())
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

    /**
     * @param repoObjLoader the object loader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

}
