package edu.unc.lib.boxc.web.services.processing;

import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.operations.jms.RunEnhancementsMessageHelpers.makeEnhancementOperationBody;

import java.util.Arrays;
import java.util.List;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import io.dropwizard.metrics5.Timer;

/**
 * Queries solr and creates JMS message(s) to run enhancements on returned File objects
 *
 * @author lfarrell
 */
public class RunEnhancementsService {
    private static final Logger LOG = LoggerFactory.getLogger(RunEnhancementsService.class);
    private static final Timer timer = TimerFactory.createTimerForClass(RunEnhancementsService.class);

    private static final List<String> RESULTS_FIELD_LIST = Arrays.asList(
            SearchFieldKey.DATASTREAM.name(), SearchFieldKey.ID.name(),
            SearchFieldKey.RESOURCE_TYPE.name());

    private AccessControlService aclService;

    private MessageSender messageSender;

    private RepositoryObjectLoader repositoryObjectLoader;

    private SolrQueryLayerService queryLayer;

    /**
     * Service to take a list of pids searches for file objects which are in the list of pids
     * or children of those objects and run enhancements on.
     *
     * @param request Request to run enhancements
     */
    public void run(RunEnhancementsRequest request) {
        try (Timer.Context ignored = timer.time()) {
            var agent = request.getAgent();
            var objectPids = request.getPids();
            var force = request.isForce();
            var recursive = request.isRecursive();
            for (String objectPid : objectPids) {
                PID pid = PIDs.get(objectPid);

                aclService.assertHasAccess("User does not have permission to run enhancements",
                        pid, agent.getPrincipals(), Permission.runEnhancements);

                if (recursive && !(repositoryObjectLoader.getRepositoryObject(pid) instanceof FileObject)) {
                    LOG.debug("Queueing object and children for enhancements: {}", pid);
                    recursiveEnhancements(pid, agent, force);
                } else {
                    LOG.debug("Queueing object for enhancements: {}", pid);
                    shallowEnhancements(pid, agent, force);
                }
            }
        }
    }

    private void recursiveEnhancements(PID pid, AgentPrincipals agent, Boolean force) {
        SearchState searchState = new SearchState();
        searchState.addFacet(new GenericFacet(SearchFieldKey.RESOURCE_TYPE, ResourceType.File.name()));
        searchState.setResultFields(RESULTS_FIELD_LIST);
        searchState.setRowsPerPage(1000);
        searchState.setIgnoreMaxRows(true);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setAccessGroups(agent.getPrincipals());
        searchRequest.setSearchState(searchState);
        searchRequest.setRootPid(pid);
        searchRequest.setApplyCutoffs(false);

        // Page through results for requests to run enhancements of large folders
        long totalResults = -1;
        int count = 0;
        do {
            SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);
            if (totalResults == -1) {
                totalResults = resultResponse.getResultCount();
                LOG.debug("Found {} items to queue for enhancement run", totalResults);
                // Add the root container itself
                ContentObjectRecord rootContainer = resultResponse.getSelectedContainer();
                createMessage(rootContainer, agent.getUsername(), force);
            }
            for (ContentObjectRecord metadata : resultResponse.getResultList()) {
                createMessage(metadata, agent.getUsername(), force);
                count++;
            }
            LOG.debug("Queued {} out of {} items for enhancements", count, totalResults);
        } while(count < totalResults);
    }

    private void shallowEnhancements(PID pid, AgentPrincipals agent, Boolean force) {
        SimpleIdRequest searchRequest = new SimpleIdRequest(pid, agent.getPrincipals());
        ContentObjectRecord metadata = queryLayer.getObjectById(searchRequest);
        createMessage(metadata, agent.getUsername(), force);
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    private void createMessage(ContentObjectRecord metadata, String username, Boolean force) {
        PID pid = metadata.getPid();
        Datastream originalDs = metadata.getDatastreamObject(ORIGINAL_FILE.getId());
        String resourceType = metadata.getResourceType();
        PID originalPid = (ResourceType.File.equals(resourceType) && originalDs != null) ?
                DatastreamPids.getOriginalFilePid(pid) : pid;
        Document msg = makeEnhancementOperationBody(username, originalPid, force);
        messageSender.sendMessage(msg);
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setQueryLayer(SolrQueryLayerService queryLayer) {
        this.queryLayer = queryLayer;
    }

    public void setMessageSender(MessageSender messageSender) {
        this.messageSender = messageSender;
    }
}
