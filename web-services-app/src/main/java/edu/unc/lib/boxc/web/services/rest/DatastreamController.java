package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.web.common.exceptions.ResourceNotFoundException;
import edu.unc.lib.boxc.web.common.services.AccessCopiesService;
import edu.unc.lib.boxc.web.common.services.DerivativeContentService;
import edu.unc.lib.boxc.web.common.services.FedoraContentService;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import edu.unc.lib.boxc.web.common.utils.AnalyticsTrackerUtil;
import edu.unc.lib.boxc.web.services.processing.DownloadImageService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static edu.unc.lib.boxc.auth.api.services.DatastreamPermissionUtil.getPermissionForDatastream;
import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.fcrepo.services.DerivativeService.isDerivative;
import static org.apache.http.HttpHeaders.RANGE;

/**
 *
 * Controller which provides access to datastreams of objects held in the repository.
 *
 * @author bbpennel
 *
 */
@Controller
public class DatastreamController {
    private static final Logger log = LoggerFactory.getLogger(DatastreamController.class);
    private static final String SMALL = "small";
    private static final String LARGE = "large";
    private static final Map<String, Integer> THUMB_SIZE_MAP = Map.of(SMALL, 64, LARGE, 128);

    @Autowired
    private FedoraContentService fedoraContentService;
    @Autowired
    private AnalyticsTrackerUtil analyticsTracker;
    @Autowired
    private DerivativeContentService derivativeContentService;
    @Autowired
    private SolrQueryLayerService solrQueryLayerService;
    @Autowired
    private AccessCopiesService accessCopiesService;
    @Autowired
    private AccessControlService accessControlService;
    @Autowired
    private DownloadImageService downloadImageService;

    private static final List<String> THUMB_QUERY_FIELDS = Arrays.asList(
            SearchFieldKey.ID.name(), SearchFieldKey.DATASTREAM.name(),
            SearchFieldKey.FILE_FORMAT_CATEGORY.name(), SearchFieldKey.FILE_FORMAT_TYPE.name(),
            SearchFieldKey.RESOURCE_TYPE.name(), SearchFieldKey.ANCESTOR_PATH.name());

    @CrossOrigin
    @RequestMapping("/file/{pid}")
    public void getDatastream(@PathVariable("pid") String pidString,
            @RequestParam(value = "dl", defaultValue = "false") boolean download,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        getDatastream(pidString, ORIGINAL_FILE.getId(), download, request, response);
    }

    @CrossOrigin
    @RequestMapping("/file/{pid}/{datastream}")
    public void getDatastream(@PathVariable("pid") String pidString,
            @PathVariable("datastream") String datastream,
            @RequestParam(value = "dl", defaultValue = "false") boolean download,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        PID pid = PIDs.get(pidString);
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();

        if (isDerivative(datastream)) {
            derivativeContentService.streamData(pid, datastream, principals, false, response);
        } else if (DatastreamType.MD_EVENTS.getId().equals(datastream)) {
            fedoraContentService.streamEventLog(pid, principals, download, response);
        } else {
            accessControlService.assertHasAccess("Insufficient permissions to access " + datastream + " for object " + pid,
                    pid, principals, getPermissionForDatastream(datastream));
            var range = request.getHeader(RANGE);
            fedoraContentService.streamData(pid, datastream, download, response, range);
            if (DatastreamType.ORIGINAL_FILE.getId().equals(datastream)) {
                recordDownloadEvent(pid, datastream, principals, request);
            }
        }
    }

    private void recordDownloadEvent(PID pid, String datastream, AccessGroupSet principals,
            HttpServletRequest request) {
        if (!(StringUtils.isBlank(datastream) || ORIGINAL_FILE.getId().equals(datastream))) {
            return;
        }
        analyticsTracker.trackEvent(request, "download", pid, principals);
    }

    @RequestMapping("/thumb/{pid}")
    public ResponseEntity<InputStreamResource> getThumbnailSmall(@PathVariable("pid") String pid,
            @RequestParam(value = "size", defaultValue = SMALL) String size) {

        return getThumbnail(pid, size);
    }

    @RequestMapping("/thumb/{pid}/{size}")
    public ResponseEntity<InputStreamResource> getThumbnail(@PathVariable("pid") String pidString,
                                                            @PathVariable("size") String size) {

        PID pid = PIDs.get(pidString);
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        accessControlService.assertHasAccess("Insufficient permissions to get thumbnail for " + pidString,
                pid, principals, Permission.viewAccessCopies);

        size = size.toLowerCase();
        if (!THUMB_SIZE_MAP.containsKey(size)) {
            throw new IllegalArgumentException("That is not a valid thumbnail size");
        }

        // For work objects, determine the source of the thumbnail
        var objRequest = new SimpleIdRequest(pid, THUMB_QUERY_FIELDS, principals);
        var objRecord = solrQueryLayerService.getObjectById(objRequest);
        if (objRecord == null) {
            throw new ResourceNotFoundException("The requested object either does not exist or is not accessible");
        }

        if (ResourceType.Folder.name().equals(objRecord.getResourceType()) ||
                ResourceType.Collection.name().equals(objRecord.getResourceType()) ||
                ResourceType.AdminUnit.name().equals(objRecord.getResourceType())) {
            String thumbName = "thumbnail_" + size.toLowerCase().trim();
            try {
                return derivativeContentService.streamThumbnail(pid, thumbName);
            } catch (IOException e) {
                log.error("Error streaming thumbnail for {}", pid);
            }
        } else if (ResourceType.Work.name().equals(objRecord.getResourceType())) {
            var thumbId = accessCopiesService.getThumbnailId(objRecord, principals, true);
            if (thumbId != null) {
                pid = PIDs.get(thumbId);
                // check permissions for thumbnail file
                accessControlService.assertHasAccess("Insufficient permissions to get thumbnail for " + pidString,
                        pid, principals, Permission.viewAccessCopies);
                log.debug("Got thumbnail id {} for work {}", thumbId, pidString);
            }
        }

        var thumbObjRequest = new SimpleIdRequest(pid, THUMB_QUERY_FIELDS, principals);
        var thumbObjRecord = solrQueryLayerService.getObjectById(thumbObjRequest);
        var pixelSize = THUMB_SIZE_MAP.get(size).toString();

        try {
            return downloadImageService.streamImage(thumbObjRecord, pixelSize, false);
        } catch (IOException e) {
            log.error("Error streaming thumbnail for {} at size {}", pidString, pixelSize, e);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler({ResourceNotFoundException.class, NotFoundException.class, FileNotFoundException.class})
    public void handleResourceNotFound() {
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessRestrictionException.class)
    public void handleInvalidRecordRequest() {
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler({ObjectTypeMismatchException.class, IllegalArgumentException.class})
    public void handleBadRequest() {
    }

    public void setFedoraContentService(FedoraContentService fedoraContentService) {
        this.fedoraContentService = fedoraContentService;
    }

    public void setAnalyticsTracker(AnalyticsTrackerUtil analyticsTracker) {
        this.analyticsTracker = analyticsTracker;
    }

    public void setDerivativeContentService(DerivativeContentService derivativeContentService) {
        this.derivativeContentService = derivativeContentService;
    }

    public void setSolrQueryLayerService(SolrQueryLayerService solrQueryLayerService) {
        this.solrQueryLayerService = solrQueryLayerService;
    }

    public void setAccessCopiesService(AccessCopiesService accessCopiesService) {
        this.accessCopiesService = accessCopiesService;
    }

    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    public void setDownloadImageService(DownloadImageService downloadImageService) {
        this.downloadImageService = downloadImageService;
    }
}