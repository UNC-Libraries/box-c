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
package edu.unc.lib.dl.persist.services.delete;

import static edu.unc.lib.dl.acl.util.Permission.markForDeletion;
import static edu.unc.lib.dl.acl.util.Permission.markForDeletionUnit;
import static edu.unc.lib.dl.rdf.CdrAcl.markedForDeletion;
import static edu.unc.lib.dl.sparql.SparqlUpdateHelper.createSparqlReplace;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.InvalidOperationForObjectType;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.sparql.SparqlUpdateService;

/**
 * Operation to mark a repository content object as deleted
 *
 * @author bbpennel
 *
 */
public class MarkForDeletionJob implements Runnable {

    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private SparqlUpdateService sparqlUpdateService;

    private AgentPrincipals agent;
    private PID pid;


    public MarkForDeletionJob(PID pid, AgentPrincipals agent,
            RepositoryObjectLoader repositoryObjectLoader, SparqlUpdateService sparqlUpdateService,
            AccessControlService aclService) {
        this.pid = pid;
        this.repositoryObjectLoader = repositoryObjectLoader;
        this.sparqlUpdateService = sparqlUpdateService;
        this.aclService = aclService;
        this.agent = agent;
    }

    @Override
    public void run() {
        aclService.assertHasAccess("Insufficient privileges to delete object " + pid.getUUID(),
                pid, agent.getPrincipals(), markForDeletion);

        RepositoryObject repoObj = repositoryObjectLoader.getRepositoryObject(pid);

        if (repoObj instanceof AdminUnit) {
            aclService.assertHasAccess("Insufficient privileges to delete admin unit " + pid.getUUID(),
                    pid, agent.getPrincipals(), markForDeletionUnit);
        }

        if (!(repoObj instanceof ContentObject)) {
            throw new InvalidOperationForObjectType("Cannot mark object " + pid.getUUID()
                    + " for deletion, objects of type " + repoObj.getClass().getName() + " are not eligible.");
        }

        String updateString = createSparqlReplace(pid.getRepositoryPath(), markedForDeletion, true);

        sparqlUpdateService.executeUpdate(repoObj.getMetadataUri().toString(), updateString);

        repoObj.getPremisLog().buildEvent(Premis.Deletion)
                .addImplementorAgent(agent.getUsernameUri())
                .addEventDetail("Item marked for deletion and not available without permissions")
                .write();
    }

}
