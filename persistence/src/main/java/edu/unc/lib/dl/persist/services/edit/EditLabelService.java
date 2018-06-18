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
package edu.unc.lib.dl.persist.services.edit;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.Premis;

/**
 * Service that manages editing of the dc:title property on an object
 *
 * @author harring
 *
 */
public class EditLabelService {

    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private RepositoryObjectFactory repoObjFactory;

    public EditLabelService() {
    }

    /**
     * Changes an object's dc:title and creates a premis event marking the change
     *
     * @param agent security principals of the agent making request
     * @param pid the pid of the object whose label is to be changed
     * @param label the new label (dc:title) of the given object
     */
    public void editLabel(AgentPrincipals agent, PID pid, String label) {
        aclService.assertHasAccess(
                "User does not have permissions to edit labels",
                pid, agent.getPrincipals(), Permission.editDescription);

        RepositoryObject obj = repoObjLoader.getRepositoryObject(pid);
        Model objModel = obj.getModel();
        Resource resc = objModel.getResource(obj.getUri().toString());

        String oldLabel = replaceOldLabel(objModel, resc, label);

        repoObjFactory.createExclusiveRelationship(obj, DcElements.title, (Resource) objModel.createLiteral(label));

        obj.getPremisLog()
        .buildEvent(Premis.Migration)
        .addImplementorAgent(agent.getUsernameUri())
        .addEventDetail("Object renamed from " + oldLabel + " to " + label)
        .write();
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setRepoObjLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setRepoObjFactory(RepositoryObjectFactory repoObjFactory) {
        this.repoObjFactory = repoObjFactory;
    }

    private String replaceOldLabel(Model objModel,Resource resc, String label) {
        String oldLabel = "no dc:title";
        if (objModel.contains(resc, DcElements.title)) {
        Statement s = objModel.getRequiredProperty(resc, DcElements.title);
        oldLabel = s.getLiteral().getString();
        objModel = objModel.remove(s);
        }
        return oldLabel;
     }
}
