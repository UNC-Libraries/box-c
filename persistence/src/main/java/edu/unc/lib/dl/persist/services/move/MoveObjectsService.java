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
package edu.unc.lib.dl.persist.services.move;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.fcrepo.client.FcrepoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.destroy.DestroyProxyService;
import edu.unc.lib.dl.reporting.ActivityMetricsClient;
import edu.unc.lib.dl.search.solr.service.ObjectPathFactory;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.sparql.SparqlQueryService;

/**
 * Service which moves content objects between containers.
 *
 * @author bbpennel
 *
 */
public class MoveObjectsService {
    private static final Logger log = LoggerFactory.getLogger(MoveObjectsService.class);

    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private TransactionManager transactionManager;
    private SparqlQueryService sparqlQueryService;
    private FcrepoClient fcrepoClient;
    private OperationsMessageSender operationsMessageSender;
    private ObjectPathFactory objectPathFactory;
    private boolean asynchronous;
    private ExecutorService moveExecutor;
    private ActivityMetricsClient operationMetrics;
    private DestroyProxyService proxyService;

    /**
     * Move a list of objects to the destination container as the provided
     * agent.
     *
     * @param agent
     * @param destinationPid
     * @param pids
     */
    public String moveObjects(AgentPrincipals agent, PID destinationPid, List<PID> pids) {
        if (destinationPid == null || pids == null || pids.isEmpty()) {
            throw new IllegalArgumentException("Must provide a destination and at least one object to move.");
        }
        if (agent == null || agent.getUsername() == null || agent.getPrincipals() == null) {
            throw new IllegalArgumentException("Must provide agent identification information");
        }

        MoveObjectsJob job = new MoveObjectsJob(agent, destinationPid, pids, proxyService);
        job.setAclService(aclService);
        job.setFcrepoClient(fcrepoClient);
        job.setRepositoryObjectLoader(repositoryObjectLoader);
        job.setSparqlQueryService(sparqlQueryService);
        job.setTransactionManager(transactionManager);
        job.setOperationsMessageSender(operationsMessageSender);
        job.setObjectPathFactory(objectPathFactory);
        job.setOperationMetrics(operationMetrics);

        if (asynchronous) {
            log.info("User {} is queueing move operation {} of {} objects to destination {}",
                    new Object[] { agent.getUsername(), job.getMoveId(), pids.size(), destinationPid });
            moveExecutor.submit(job);
        } else {
            job.run();
        }

        return job.getMoveId();
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
     * @param transactionManager the transactionManager to set
     */
    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * @param sparqlQueryService the sparqlQueryService to set
     */
    public void setSparqlQueryService(SparqlQueryService sparqlQueryService) {
        this.sparqlQueryService = sparqlQueryService;
    }

    /**
     * @param fcrepoClient the fcrepoClient to set
     */
    public void setFcrepoClient(FcrepoClient fcrepoClient) {
        this.fcrepoClient = fcrepoClient;
    }

    /**
     * @param asynchronous the asynchronous to set
     */
    public void setAsynchronous(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }

    /**
     * @param operationsMessageSender the operationsMessageSender to set
     */
    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }

    /**
     * @param objectPathFactory the objectPathFactory to set
     */
    public void setObjectPathFactory(ObjectPathFactory objectPathFactory) {
        this.objectPathFactory = objectPathFactory;
    }

    /**
     * @param operationMetrics the operationMetrics to set
     */
    public void setOperationMetrics(ActivityMetricsClient operationMetrics) {
        this.operationMetrics = operationMetrics;
    }

    /**
     * @param moveExecutor the moveExecutor to set
     */
    public void setMoveExecutor(ExecutorService moveExecutor) {
        this.moveExecutor = moveExecutor;
    }

    /**
    * @param proxyService the proxyService to set
    */
   public void setProxyService(DestroyProxyService proxyService) {
       this.proxyService = proxyService;
   }
}
