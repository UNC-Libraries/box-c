package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.web.common.services.FedoraContentService;
import edu.unc.lib.boxc.web.common.utils.AnalyticsTrackerUtil;
import edu.unc.lib.boxc.web.services.processing.SingleUseKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.web.common.services.FedoraContentService.RANGE_HEADER;

/**
 * Controller for generating and utilizing single use links for a specific UUID
 *
 * @author snluong
 */
@Controller
public class SingleUseKeyController {
    private static final Logger log = LoggerFactory.getLogger(SingleUseKeyController.class);
    @Autowired
    private AccessControlService aclService;
    @Autowired
    private SingleUseKeyService singleUseKeyService;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    private FedoraContentService fedoraContentService;
    @Autowired
    private AnalyticsTrackerUtil analyticsTracker;

    @RequestMapping(value = "/single_use_link/create/{id}", method = RequestMethod.POST)
    public ResponseEntity<Object> generate(@PathVariable("id") String id) {
        var pid = PIDs.get(id);
        // requester must have the right permission
        var agent= getAgentPrincipals();
        aclService.assertHasAccess("Insufficient permissions to generate single use link for " + id,
                pid, agent.getPrincipals(), Permission.viewHidden);

        // check if object is a FileObject
        ContentObject obj = (ContentObject) repositoryObjectLoader.getRepositoryObject(pid);
        if (!(obj instanceof FileObject)) {
            throw new InvalidOperationForObjectType("Single use link cannot be generated for " +
                    obj.getClass().getName() + " objects.");
        }

        var keyInfo = singleUseKeyService.generate(id);
        log.info("Single use link created for UUID {} by user {}", id, agent.getUsername());
        return new ResponseEntity<>(keyInfo, HttpStatus.OK);
    }

    @RequestMapping(value = "/single_use_link/{key}", method = RequestMethod.GET)
    public void download(@PathVariable("key") String accessKey, HttpServletRequest request,
                                                HttpServletResponse response) throws IOException {
        if (singleUseKeyService.keyIsValid(accessKey)) {
            var id = singleUseKeyService.getId(accessKey);
            var pid = PIDs.get(id);
            var datastream = ORIGINAL_FILE.getId();
            var principals = getAgentPrincipals().getPrincipals();
            var range = request.getHeader(RANGE_HEADER);

            singleUseKeyService.invalidate(accessKey);
            fedoraContentService.streamData(pid, datastream, true, response, range);
            log.info("Single use link used. Access Key: {}, UUID: {}", accessKey, id);
            analyticsTracker.trackEvent(request, "download", pid, principals);
        } else {
            throw new NotFoundException("Single use key is not valid: " + accessKey);
        }
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setSingleUseKeyService(SingleUseKeyService singleUseKeyService) {
        this.singleUseKeyService = singleUseKeyService;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setFedoraContentService(FedoraContentService fedoraContentService) {
        this.fedoraContentService = fedoraContentService;
    }

    public void setAnalyticsTracker(AnalyticsTrackerUtil analyticsTracker) {
        this.analyticsTracker = analyticsTracker;
    }
}
