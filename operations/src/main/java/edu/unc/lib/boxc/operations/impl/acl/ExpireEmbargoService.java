package edu.unc.lib.boxc.operations.impl.acl;

import static edu.unc.lib.boxc.common.util.DateTimeUtil.formatDateToUTC;
import static edu.unc.lib.boxc.common.util.DateTimeUtil.parseUTCToDate;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.embargoUntil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.SoftwareAgentConstants.SoftwareAgent;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.jms.JMSMessageUtil;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.search.api.FacetConstants;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.filters.QueryFilterFactory;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
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
    private SolrSearchService searchService;
    private AgentPrincipals agentPrincipals;
    private PremisLoggerFactory premisLoggerFactory;

    private static final List<String> SEARCH_FIELDS = Arrays.asList(SearchFieldKey.ID.name(),
            SearchFieldKey.STATUS.name());
    private static final int DEFAULT_PAGE_SIZE = 10000;
    public static final String AGENT_NAME = "EmbargoExpirationAgent";

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

        if (!resourceList.isEmpty()) {
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

    private List<String> getEmbargoInfo() {
        SearchState searchState = new SearchState();
        searchState.setResultFields(SEARCH_FIELDS);
        searchState.setRowsPerPage(DEFAULT_PAGE_SIZE);
        searchState.setIgnoreMaxRows(true);
        searchState.addFilter(QueryFilterFactory.createHasValuesFilter(SearchFieldKey.STATUS, List.of(FacetConstants.EMBARGOED)));
        SearchRequest request = new SearchRequest(searchState, agentPrincipals.getPrincipals());
        request.setApplyCutoffs(false);

        var response = searchService.getSearchResults(request);
        List<ContentObjectRecord> embargoes = response.getResultList();

        var today = new Date();
        List<String> embargoedRescList = new ArrayList<>();

        for (ContentObjectRecord contentObj : embargoes) {
            var repoObj = repoObjLoader.getRepositoryObject(contentObj.getPid());
            var embargoProperty = repoObj.getResource().getProperty(embargoUntil);
            var embargoDate = LocalDateTime.parse(embargoProperty.getLiteral().getString(),
                    DateTimeFormatter.ISO_DATE_TIME);

            ZonedDateTime zonedDateTime = embargoDate.atZone(ZoneId.systemDefault());
            Instant embargoDateInstant = zonedDateTime.toInstant();
            Date date = Date.from(embargoDateInstant);

            if (today.after(date)) {
                embargoedRescList.add(contentObj.getId());
            }
        }

        return embargoedRescList;
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
     *
     * @param searchService the search service to set
     */
    public void setSearchService(SolrSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     *
     * @param accessGroups groups for the embargo user
     */
    public void setAccessGroups(AccessGroupSet accessGroups) {
        this.agentPrincipals = new AgentPrincipalsImpl(AGENT_NAME, accessGroups);
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
