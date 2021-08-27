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
package edu.unc.lib.boxc.operations.impl.delete;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Collection;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.sparql.SparqlUpdateService;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;

/**
 * Service which manages setting of the marked for deletion flag.
 *
 * @author bbpennel
 *
 */
public class MarkForDeletionService {

    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private SparqlUpdateService sparqlUpdateService;
    private OperationsMessageSender operationsMessageSender;
    private PremisLoggerFactory premisLoggerFactory;

    /**
     * Mark a pid for deletion using the agent principals provided.
     *
     * @param agent security principals of the agent making request.
     * @param message message containing the reason for this action
     * @param pids pid of object to mark for deletion
     */
    public void markForDeletion(AgentPrincipals agent, String message, String... ids) {
        if (isBlank(message)) {
            throw new IllegalArgumentException("A message describing the reason for this deletion must be provided");
        }

        Collection<PID> pids = new ArrayList<>();
        for (String id : ids) {
            PID pid = PIDs.get(id);
            Runnable job = new MarkForDeletionJob(pid, message, agent, repositoryObjectLoader,
                    sparqlUpdateService, aclService, premisLoggerFactory);
            job.run();
            pids.add(pid);
        }
        operationsMessageSender.sendMarkForDeletionOperation(agent.getUsername(), pids);
    }

    /**
     * Restore each object such that it is no longer marked for deletion using
     * the agent principals provided.
     *
     * @param agent security principals of the agent making request.
     * @param pids pids of objects to restore
     */
    public void restoreMarked(AgentPrincipals agent, String... ids) {
        Collection<PID> pids = new ArrayList<>();
        for (String id : ids) {
            PID pid = PIDs.get(id);
            Runnable job = new RestoreDeletedJob(pid, agent,
                    repositoryObjectLoader, sparqlUpdateService, aclService, premisLoggerFactory);
            job.run();
            pids.add(pid);
        }
        operationsMessageSender.sendRestoreFromDeletionOperation(agent.getUsername(), pids);
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
     * @param sparqlUpdateService the sparqlUpdateService to set
     */
    public void setSparqlUpdateService(SparqlUpdateService sparqlUpdateService) {
        this.sparqlUpdateService = sparqlUpdateService;
    }

    /**
    *
    * @param operationsMessageSender
    */
   public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
       this.operationsMessageSender = operationsMessageSender;
   }

    public void setPremisLoggerFactory(PremisLoggerFactory premisLoggerFactory) {
        this.premisLoggerFactory = premisLoggerFactory;
    }
}
