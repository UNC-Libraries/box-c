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

import java.util.ArrayList;
import java.util.Collection;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.sparql.SparqlUpdateService;

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

    /**
     * Mark a pid for deletion using the agent principals provided.
     *
     * @param agent security principals of the agent making request.
     * @param pids pid of object to mark for deletion
     */
    public void markForDeletion(AgentPrincipals agent, String... ids) {
        Collection<PID> pids = new ArrayList<>();
        for (String id : ids) {
            PID pid = PIDs.get(id);
            Runnable job = new MarkForDeletionJob(pid, agent, repositoryObjectLoader,
                    sparqlUpdateService, aclService);
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
                    repositoryObjectLoader, sparqlUpdateService, aclService);
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
}
