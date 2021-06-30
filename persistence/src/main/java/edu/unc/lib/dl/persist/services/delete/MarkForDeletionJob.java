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
import static edu.unc.lib.dl.sparql.SparqlUpdateHelper.createSparqlReplace;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPIDs;
import edu.unc.lib.boxc.model.fcrepo.objects.AbstractContentObject;
import edu.unc.lib.boxc.model.fcrepo.objects.AdminUnitImpl;
import edu.unc.lib.dl.sparql.SparqlUpdateService;
import io.dropwizard.metrics5.Timer;

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
    private String message;

    private static final Timer timer = TimerFactory.createTimerForClass(MarkForDeletionJob.class);


    public MarkForDeletionJob(PID pid, String message, AgentPrincipals agent,
            RepositoryObjectLoader repositoryObjectLoader, SparqlUpdateService sparqlUpdateService,
            AccessControlService aclService) {
        this.pid = pid;
        this.repositoryObjectLoader = repositoryObjectLoader;
        this.sparqlUpdateService = sparqlUpdateService;
        this.aclService = aclService;
        this.agent = agent;
        this.message = message;
    }

    @Override
    public void run() {
        try (Timer.Context context = timer.time()) {
            aclService.assertHasAccess("Insufficient privileges to delete object " + pid.getUUID(),
                    pid, agent.getPrincipals(), markForDeletion);

            RepositoryObject repoObj = repositoryObjectLoader.getRepositoryObject(pid);

            if (repoObj instanceof AdminUnitImpl) {
                aclService.assertHasAccess("Insufficient privileges to delete admin unit " + pid.getUUID(),
                        pid, agent.getPrincipals(), markForDeletionUnit);
            }

            if (!(repoObj instanceof AbstractContentObject)) {
                throw new InvalidOperationForObjectType("Cannot mark object " + pid.getUUID()
                        + " for deletion, objects of type " + repoObj.getClass().getName() + " are not eligible.");
            }

            String updateString = createSparqlReplace(pid.getRepositoryPath(), markedForDeletion, true);

            sparqlUpdateService.executeUpdate(repoObj.getMetadataUri().toString(), updateString);

            repoObj.getPremisLog().buildEvent(Premis.Deaccession)
                    .addImplementorAgent(AgentPIDs.forPerson(agent))
                    .addEventDetail("Item marked for deletion and not available without permissions")
                    .addEventDetail(message)
                    .writeAndClose();
        }
    }
}
