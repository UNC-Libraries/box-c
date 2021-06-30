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

import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.markedForDeletion;
import static edu.unc.lib.dl.acl.util.Permission.markForDeletion;
import static edu.unc.lib.dl.acl.util.Permission.markForDeletionUnit;
import static edu.unc.lib.dl.sparql.SparqlUpdateHelper.createSparqlDelete;

import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPIDs;
import edu.unc.lib.boxc.model.fcrepo.objects.AbstractContentObject;
import edu.unc.lib.boxc.model.fcrepo.objects.AdminUnitImpl;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.sparql.SparqlUpdateService;

/**
 * Operation which restores a content object which was marked for deletion
 *
 * @author bbpennel
 *
 */
public class RestoreDeletedJob implements Runnable {

    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private SparqlUpdateService sparqlUpdateService;

    private AgentPrincipals agent;
    private PID pid;

    public RestoreDeletedJob(PID pid, AgentPrincipals agent,
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
        aclService.assertHasAccess("Insufficient privileges to restore object " + pid.getUUID(),
                pid, agent.getPrincipals(), markForDeletion);

        RepositoryObject repoObj = repositoryObjectLoader.getRepositoryObject(pid);

        if (repoObj instanceof AdminUnitImpl) {
            aclService.assertHasAccess("Insufficient privileges to restore admin unit " + pid.getUUID(),
                    pid, agent.getPrincipals(), markForDeletionUnit);
        }

        if (!(repoObj instanceof AbstractContentObject)) {
            throw new InvalidOperationForObjectType("Cannot perform restore on object " + pid.getUUID()
                    + ", objects of type " + repoObj.getClass().getName() + " are not eligible.");
        }

        String updateString = createSparqlDelete(pid.getRepositoryPath(), markedForDeletion, null);

        sparqlUpdateService.executeUpdate(repoObj.getMetadataUri().toString(), updateString);

        repoObj.getPremisLog().buildEvent(Premis.Accession)
                .addImplementorAgent(AgentPIDs.forPerson(agent))
                .addEventDetail("Item restored from deletion")
                .writeAndClose();
    }

}
