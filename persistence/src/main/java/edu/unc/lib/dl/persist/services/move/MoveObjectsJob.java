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

import java.io.IOException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.fcrepo.client.FcrepoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FedoraTransaction;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.persist.services.destroy.DestroyProxyService;
import edu.unc.lib.dl.reporting.ActivityMetricsClient;
import edu.unc.lib.dl.search.solr.model.ObjectPath;
import edu.unc.lib.dl.search.solr.service.ObjectPathFactory;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.sparql.SparqlQueryService;
import edu.unc.lib.dl.util.DateTimeUtil;
import io.dropwizard.metrics5.Timer;

/**
 * Job which performs a single move operation to transfer a list of objects from
 * their source containers to a single destination content container
 *
 * @author bbpennel
 *
 */
public class MoveObjectsJob implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(MoveObjectsJob.class);
    private static final Logger moveLog = LoggerFactory.getLogger("move_logger");

    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private TransactionManager transactionManager;
    private SparqlQueryService sparqlQueryService;
    private FcrepoClient fcrepoClient;
    private OperationsMessageSender operationsMessageSender;
    private ObjectPathFactory objectPathFactory;
    private ActivityMetricsClient operationMetrics;
    private DestroyProxyService proxyService;

    private AgentPrincipals agent;
    private PID destinationPid;
    private List<PID> pids;

    private String moveId;

    private ContentContainerObject destContainer;

    private Map<String, Collection<PID>> sourceToPid;

    private static final Timer timer = TimerFactory.createTimerForClass(MoveObjectsJob.class);

    public MoveObjectsJob(AgentPrincipals agent, PID destination, List<PID> pids, DestroyProxyService proxyService) {
        this.agent = agent;
        this.destinationPid = destination;
        this.pids = pids;
        this.proxyService = proxyService;
        sourceToPid = new HashMap<>();
        moveId = Long.toString(new SecureRandom().nextLong());
    }

    @Override
    public void run() {
        log.debug("Performing move for agent {} of {} objects to destination {}",
                agent.getUsername(), pids.size(), destinationPid);
        try (Timer.Context context = timer.time()) {
            // Check that agent has permission to add items to destination
            aclService.assertHasAccess("Agent " + agent.getUsername() + " does not have permission"
                    + " to move objects into destination " + destinationPid,
                    destinationPid, agent.getPrincipals(), Permission.move);

            retrieveDestinationContainer();

            FedoraTransaction tx = transactionManager.startTransaction();
            try {
                for (PID movePid : pids) {
                    moveObject(movePid);
                }
            } catch (Exception e) {
                tx.cancel(e);
            } finally {
                tx.close();
            }

            reportCompleted();
        }
    }

    private void reportCompleted() {
        operationMetrics.incrMoves();

        List<PID> sourcePids = sourceToPid.keySet().stream().map(p -> PIDs.get(p)).collect(Collectors.toList());
        operationsMessageSender.sendMoveOperation(agent.getUsername(), sourcePids, destinationPid, pids, null);

        logMoveAction();
    }

    private void retrieveDestinationContainer() {
        // Verify that the destination is a content container
        RepositoryObject destObj = repositoryObjectLoader.getRepositoryObject(destinationPid);
        if (!(destObj instanceof ContentContainerObject)) {
            throw new IllegalArgumentException("Destination " + destinationPid + " was not a content container");
        }
        destContainer = (ContentContainerObject) destObj;
    }

    private void moveObject(PID objPid) {
        aclService.assertHasAccess("Agent " + agent.getUsername() + " does not have permission to move object "
                + objPid, objPid, agent.getPrincipals(), Permission.move);

        ContentObject moveContent = (ContentObject) repositoryObjectLoader.getRepositoryObject(objPid);

        proxyService.destroyProxy(objPid);

        destContainer.addMember(moveContent);
    }

    private void logMoveAction() {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("event", "moved");
        try {
            logEntry.put("timestamp", DateTimeUtil.formatDateToUTC(new Date()));
        } catch (ParseException e) {
            log.error("Failed to parse date", e);
        }
        logEntry.put("move_id", moveId);
        logEntry.put("user", agent.getUsername());

        logEntry.put("destination_id", destinationPid.getId());
        ObjectPath destPath = objectPathFactory.getPath(destinationPid);
        if (destPath != null) {
            logEntry.put("destination_path", destPath.toNamePath());
        }

        // Log moved objects grouped by source
        Map<String, Map<String, Object>> sourcesLog = new HashMap<>();
        for (String sourceId : sourceToPid.keySet()) {
            PID sourcePid = PIDs.get(sourceId);
            Map<String, Object> sourceLog = new HashMap<>();
            ObjectPath sourcePath = objectPathFactory.getPath(sourcePid);
            if (sourcePath != null) {
                sourceLog.put("path", sourcePath.toNamePath());
            }

            List<String> idList = sourceToPid.get(sourceId).stream()
                    .map(p -> p.getId()).collect(Collectors.toList());
            sourceLog.put("objects", idList);

            sourcesLog.put(sourceId, sourceLog);
        }

        logEntry.put("sources", sourcesLog);

        ObjectMapper mapper = new ObjectMapper();
        try {
            moveLog.info(mapper.writeValueAsString(logEntry));
        } catch (IOException e) {
            log.error("Failed to serialize log entry for move operation {}", moveId, e);
        }
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
     * @return the moveId
     */
    public String getMoveId() {
        return moveId;
    }
}
