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

import edu.unc.lib.dl.fcrepo4.FedoraTransaction;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.sparql.SparqlQueryService;
import edu.unc.lib.dl.util.DateTimeUtil;
import edu.unc.lib.dl.util.JMSMessageUtil;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;
import io.dropwizard.metrics5.Timer;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static edu.unc.lib.dl.rdf.CdrAcl.embargoUntil;
import static edu.unc.lib.dl.util.DateTimeUtil.formatDateToUTC;
import static edu.unc.lib.dl.util.DateTimeUtil.parseUTCToDate;

/**
 * Service that manages embargo expiration
 *
 * @author smithjp
 *
 */
@EnableScheduling
public class ExpireEmbargoService {

    private RepositoryObjectFactory repoObjFactory;
    private RepositoryObjectLoader repoObjLoader;
    private OperationsMessageSender operationsMessageSender;
    private TransactionManager txManager;
    private SparqlQueryService sparqlQueryService;

    private static final Timer timer = TimerFactory.createTimerForClass(ExpireEmbargoService.class);

    public ExpireEmbargoService() {
    }

    // run service every day 1 minute after midnight
    @Scheduled(cron = "0 1 0 * * *")
    public void expireEmbargoes() {
        // get list of expired embargoes
        List<String> resourceList = getEmbargoInfo();
        Collection<PID> pids = new ArrayList<>();

        // remove all expired embargoes
        for (String rescUri: resourceList) {
            FedoraTransaction tx = txManager.startTransaction();

            try (Timer.Context context = timer.time()) {
                PID pid = PIDs.get(rescUri);
                RepositoryObject repoObj = repoObjLoader.getRepositoryObject(pid);
                Resource resc = repoObj.getResource();

                String eventText = null;
                boolean expiredEmbargo = false;

                // remove embargo
                String embargoDate = resc.getProperty(embargoUntil).getString();
                repoObjFactory.deleteProperty(repoObj, embargoUntil);
                pids.add(pid);
                expiredEmbargo = true;

                if (expiredEmbargo) {
                    eventText = "Expired an embargo which ended " +
                            formatDateToUTC(parseUTCToDate(embargoDate));
                    // Produce the premis event for this embargo
                    repoObj.getPremisLog().buildEvent(Premis.Dissemination)
                            .addSoftwareAgent(SoftwareAgent.embargoExpirationService.getFullname())
                            .addEventDetail(eventText)
                            .writeAndClose();
                }
            } catch (Exception e) {
                tx.cancelAndIgnore();
                throw e;
            } finally {
                tx.close();
            }
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
}
