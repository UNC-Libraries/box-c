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
package edu.unc.lib.boxc.operations.impl.acl;

import static edu.unc.lib.boxc.common.util.DateTimeUtil.formatDateToUTC;
import static edu.unc.lib.boxc.common.util.DateTimeUtil.parseUTCToDate;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.embargoUntil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.common.util.DateTimeUtil;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.SoftwareAgentConstants.SoftwareAgent;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.api.sparql.SparqlQueryService;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.jms.JMSMessageUtil;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import io.dropwizard.metrics5.Timer;

/**
 * Service that manages embargo expiration
 *
 * @author smithjp
 *
 */
@Component
@EnableScheduling
public class ExpireEmbargoService {

    private RepositoryObjectFactory repoObjFactory;
    private RepositoryObjectLoader repoObjLoader;
    private OperationsMessageSender operationsMessageSender;
    private TransactionManager txManager;
    private SparqlQueryService sparqlQueryService;
    private PremisLoggerFactory premisLoggerFactory;

    private static final Timer timer = TimerFactory.createTimerForClass(ExpireEmbargoService.class);

    private static final Logger log = LoggerFactory.getLogger(ExpireEmbargoService.class);

    public ExpireEmbargoService() {
    }

    // run service every day 1 minute after midnight
    @Scheduled(cron = "0 1 0 * * *")
    public void expireEmbargoes() {
        log.info("running ExpireEmbargoService");
        // get list of expired embargoes
        List<String> resourceList = getEmbargoInfo();
        Collection<PID> pids = new ArrayList<>();
        PID currentPid = null;

        if (resourceList.size() > 0) {
            // remove all expired embargoes
            for (String rescUri: resourceList) {
                FedoraTransaction tx = txManager.startTransaction();

                try (Timer.Context context = timer.time()) {
                    PID pid = PIDs.get(rescUri);
                    currentPid = pid;
                    RepositoryObject repoObj = repoObjLoader.getRepositoryObject(pid);
                    Resource resc = repoObj.getResource(true);

                    // remove embargo
                    String embargoDate = resc.getProperty(embargoUntil).getString();
                    repoObjFactory.deleteProperty(repoObj, embargoUntil);
                    pids.add(pid);
                    log.info("Expired embargo for {}", pid);
                    String eventText = "Expired an embargo which ended " +
                            formatDateToUTC(parseUTCToDate(embargoDate));
                    // Produce the premis event for this embargo
                    premisLoggerFactory.createPremisLogger(repoObj)
                            .buildEvent(Premis.Dissemination)
                            .addSoftwareAgent(AgentPids.forSoftware(SoftwareAgent.embargoExpirationService))
                            .addEventDetail(eventText)
                            .writeAndClose();
                } catch (Exception e) {
                    tx.cancelAndIgnore();
                    log.error("Failed to expire embargo for {} with error:", currentPid, e);
                } finally {
                    tx.close();
                }
            }
        } else {
            log.info("No embargoes to expire");
        }

        if (!pids.isEmpty()) {
            // send a message for expired embargoes
            operationsMessageSender.sendOperationMessage(SoftwareAgent.embargoExpirationService.getFullname(),
                    JMSMessageUtil.CDRActions.EDIT_ACCESS_CONTROL,
                    pids);
        }
    }

    private final static String EMBARGO_QUERY =
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                    "select ?resource\n" +
                    "where {\n" +
                    "  ?resource <http://cdr.unc.edu/definitions/acl#embargoUntil> ?date .\n" +
                    "  FILTER (?date < \"%s\"^^xsd:dateTime)\n" +
                    "}";

    private List<String> getEmbargoInfo() {
        String today = DateTimeUtil.formatDateToUTC(new Date());
        String query = String.format(EMBARGO_QUERY, today);

        try (QueryExecution exec = sparqlQueryService.executeQuery(query)) {
            ResultSet resultSet = exec.execSelect();
            List<String> embargoedRescList = new ArrayList<>();

            for (; resultSet.hasNext() ;) {
                QuerySolution soln = resultSet.nextSolution();
                Resource resc = soln.getResource("resource");
                embargoedRescList.add(resc.getURI());
            }
            return embargoedRescList;
        }
    }

    /**
     * @param sparqlQueryService the sparqlQueryService to set
     */
    public void setSparqlQueryService(SparqlQueryService sparqlQueryService) {
        this.sparqlQueryService = sparqlQueryService;
    }

    /**
     * @param repoObjLoader the object loader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    /**
     * @param repoObjFactory the factory to set
     */
    public void setRepositoryObjectFactory(RepositoryObjectFactory repoObjFactory) {
        this.repoObjFactory = repoObjFactory;
    }

    /**
     * @param operationsMessageSender the operations message sender to set
     */
    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }

    /**
     * @param txManager the transaction manager to set
     */
    public void setTransactionManager(TransactionManager txManager) {
        this.txManager = txManager;
    }

    public void setPremisLoggerFactory(PremisLoggerFactory premisLoggerFactory) {
        this.premisLoggerFactory = premisLoggerFactory;
    }
}
