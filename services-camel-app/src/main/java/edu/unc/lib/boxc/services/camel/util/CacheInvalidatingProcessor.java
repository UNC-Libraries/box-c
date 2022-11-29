package edu.unc.lib.boxc.services.camel.util;

import edu.unc.lib.boxc.auth.fcrepo.services.ObjectAclFactory;
import edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants;
import edu.unc.lib.boxc.indexing.solr.utils.MemberOrderService;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDConstants;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.search.solr.services.TitleRetrievalService;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.fcrepo.camel.FcrepoHeaders;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Processor which invalidates cache entries for updated objects
 *
 * @author bbpennel
 */
public class CacheInvalidatingProcessor implements Processor {
    private static final Logger log = getLogger(CacheInvalidatingProcessor.class);
    private RepositoryObjectLoader repoObjLoader;
    private ObjectAclFactory objectAclFactory;
    private ContentPathFactory contentPathFactory;
    private TitleRetrievalService titleRetrievalService;
    private MemberOrderService memberOrderService;

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        var solrUpdateAction = (IndexingActionType) in.getHeader(CdrFcrepoHeaders.CdrSolrUpdateAction);
        // Skip invalidation for large actions
        if (IndexingActionUtil.LARGE_ACTIONS.contains(solrUpdateAction)) {
            log.debug("No invalidation for {}", solrUpdateAction);
            return;
        }

        String fcrepoUri = (String) in.getHeader(FcrepoHeaders.FCREPO_URI);
        if (fcrepoUri == null) {
            String fcrepoId = (String) in.getHeader(FcrepoJmsConstants.IDENTIFIER);
            String fcrepoBaseUrl = (String) in.getHeader(FcrepoJmsConstants.BASE_URL);
            fcrepoUri = fcrepoBaseUrl + fcrepoId;
        }

        PID pid;
        try {
            pid = PIDs.get(fcrepoUri);
        } catch (Exception e) {
            log.debug("Failed to parse fcrepo id {} as PID while filtering: {}", fcrepoUri, e.getMessage());
            return;
        }
        // Filter out non-content objects
        if (pid == null || !PIDConstants.CONTENT_QUALIFIER.equals(pid.getQualifier())) {
            return;
        }
        log.debug("Invalidating caches for {}", pid);
        repoObjLoader.invalidate(pid);
        objectAclFactory.invalidate(pid);
        contentPathFactory.invalidate(pid);
        memberOrderService.invalidate(pid);
        if (pid.getComponentPath() == null || pid.getComponentPath().contains(DatastreamType.MD_DESCRIPTIVE.getId())) {
            titleRetrievalService.invalidate(pid);
        }
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setObjectAclFactory(ObjectAclFactory objectAclFactory) {
        this.objectAclFactory = objectAclFactory;
    }

    public void setContentPathFactory(ContentPathFactory contentPathFactory) {
        this.contentPathFactory = contentPathFactory;
    }

    public void setTitleRetrievalService(TitleRetrievalService titleRetrievalService) {
        this.titleRetrievalService = titleRetrievalService;
    }

    public void setMemberOrderService(MemberOrderService memberOrderService) {
        this.memberOrderService = memberOrderService;
    }
}
