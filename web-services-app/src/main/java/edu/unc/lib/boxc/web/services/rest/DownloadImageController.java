
package edu.unc.lib.boxc.web.services.rest;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.images.ImageServerUtil;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import edu.unc.lib.boxc.web.common.utils.AnalyticsTrackerUtil;
import edu.unc.lib.boxc.web.services.processing.DownloadImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Objects;

/**
 * Controller for handling requests to download access copy image
 * @author snluong
 */
@Controller
public class DownloadImageController {
    private static final Logger log = LoggerFactory.getLogger(DownloadImageController.class);
    @Autowired
    private AccessControlService aclService;
    @Autowired
    private DownloadImageService downloadImageService;
    @Autowired
    private SolrQueryLayerService solrSearchService;
    @Autowired
    private AnalyticsTrackerUtil analyticsTracker;

    @RequestMapping("/downloadImage/{pid}/{size}")
    public ResponseEntity<Resource> getImage(@PathVariable("pid") String pidString,
                                             @PathVariable("size") String size,
                                             HttpServletRequest request) {
        PID pid = PIDs.get(pidString);

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        aclService.assertHasAccess("Insufficient permissions to download access copy for " + pidString,
                pid, principals, Permission.viewReducedResImages);

        var contentObjectRecord = solrSearchService.getObjectById(new SimpleIdRequest(pid, principals));
        if (contentObjectRecord == null) {
            log.error("No content object found for {}", pidString);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (downloadImageService.isInvalidJP2Datastream(contentObjectRecord)) {
            log.error("No valid JP2 datastream for {}", pidString);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        String validatedSize = downloadImageService.getSize(contentObjectRecord, size);
        if (validatedSize == null) {
            log.error("No validated size found for {}", pidString);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (Objects.equals(validatedSize, ImageServerUtil.FULL_SIZE)) {
            aclService.assertHasAccess("Insufficient permissions to download full size copy for " + pidString,
                    pid, principals, Permission.viewOriginal);
        }

        try {
            analyticsTracker.trackEvent(request, "download access copy", pid, principals);
            return downloadImageService.streamDownload(contentObjectRecord, validatedSize);
        } catch (IOException e) {
            log.error("Error streaming access copy image for {} at size {}", pidString, validatedSize, e);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    public void setDownloadImageService(DownloadImageService downloadImageService) {
        this.downloadImageService = downloadImageService;
    }
}
