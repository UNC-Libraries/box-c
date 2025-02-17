package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.fcrepo.exceptions.RangeNotSatisfiableException;
import edu.unc.lib.boxc.model.api.exceptions.InvalidPidException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.web.common.exceptions.ResourceNotFoundException;
import edu.unc.lib.boxc.web.common.services.FedoraContentService;
import edu.unc.lib.boxc.web.common.utils.AnalyticsTrackerUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static edu.unc.lib.boxc.auth.api.services.DatastreamPermissionUtil.getPermissionForDatastream;
import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static org.apache.http.HttpHeaders.RANGE;

/**
 * Controller which handles requests for specific content datastreams from Fedora and streams the results back as the
 * response.
 *
 * @author bbpennel
 */
@Controller
public class FedoraContentController {
    private static final Logger log = LoggerFactory.getLogger(FedoraContentController.class);

    @Autowired
    private FedoraContentService fedoraContentService;
    @Autowired
    private AnalyticsTrackerUtil analyticsTracker;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private AccessControlService accessControlService;

    @RequestMapping(value = {"/content/{pid}", "/indexablecontent/{pid}"})
    public void getDefaultDatastream(@PathVariable("pid") String pid,
                                     @RequestParam(value = "dl", defaultValue = "false") boolean download,
                                     HttpServletRequest request, HttpServletResponse response) {
        streamData(pid, ORIGINAL_FILE.getId(), download, request, response);
    }

    @RequestMapping(value = {"/content/{pid}/{datastream}", "/indexablecontent/{pid}/{datastream}"})
    public void getDatastream(@PathVariable("pid") String pid, @PathVariable("datastream") String datastream,
                              @RequestParam(value = "dl", defaultValue = "false") boolean download,
                              HttpServletRequest request, HttpServletResponse response) {
        streamData(pid, datastream, download, request, response);
    }

    @RequestMapping(value = {"/indexablecontent", "/content"})
    public void getDatastreamByParameters(@RequestParam("id") String id, @RequestParam("ds") String datastream,
                                          @RequestParam(value = "dl", defaultValue = "false") boolean download,
                                          HttpServletRequest request, HttpServletResponse response) {
        streamData(id, ORIGINAL_FILE.getId(), download, request, response);
    }

    private void streamData(String pidString, String datastream, boolean asAttachment, HttpServletRequest request,
                                              HttpServletResponse response) {
        PID pid = PIDs.get(pidString);
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();

        RepositoryObject repoObj = repoObjLoader.getRepositoryObject(pid);
        if (repoObj instanceof WorkObject) {
            FileObject primaryObj = ((WorkObject) repoObj).getPrimaryObject();
            if (primaryObj != null) {
                log.warn("Request to download work {}, redirecting to primary object {}",
                        pid.getId(), primaryObj.getPid().getId());
                pid = primaryObj.getPid();
            }
        }

        accessControlService.assertHasAccess("Insufficient permissions to access " + datastream + " for object " + pid,
                pid, principals, getPermissionForDatastream(datastream));

        var range = request.getHeader(RANGE);

        try {
            fedoraContentService.streamData(pid, datastream, asAttachment, response, range);
            recordDownloadEvent(pid, datastream, principals, request);
        } catch (IOException e) {
            handleIOException(pid, datastream, e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void handleIOException(PID pid, String datastream, IOException e) {
        if (connectionWasClosed(pid, datastream, e)) {
            return;
        }
        var cause = ExceptionUtils.getRootCause(e);
        if (connectionWasClosed(pid, datastream, cause)) {
            return;
        }
        log.error("Problem retrieving {}/{}", pid.getId(), datastream, e);
    }

    private boolean connectionWasClosed(PID pid, String datastream, Throwable e) {
        if (e instanceof EOFException
                || (e.getMessage() != null && e.getMessage().contains("Connection reset by peer"))) {
            log.debug("Client reset connection while downloading {}/{}", pid.getId(), datastream);
            return true;
        }
        if (e instanceof TimeoutException) {
            log.warn("Request to download {}/{} timed out: {}", pid.getId(), datastream, e.getMessage());
            return true;
        }
        return false;
    }

    private void recordDownloadEvent(PID pid, String datastream, AccessGroupSet principals,
                                     HttpServletRequest request) {
        if (!(StringUtils.isBlank(datastream) || ORIGINAL_FILE.getId().equals(datastream))) {
            return;
        }
        analyticsTracker.trackEvent(request, "download", pid, principals);
    }

    @ExceptionHandler({ResourceNotFoundException.class, NotFoundException.class, InvalidPidException.class})
    public ResponseEntity<Object> handleResourceNotFound() {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessRestrictionException.class)
    public ResponseEntity<Object> handleInvalidRecordRequest() {
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ObjectTypeMismatchException.class)
    public ResponseEntity<Object> handleObjectTypeMismatchException() {
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = { RuntimeException.class })
    public ResponseEntity<Object> handleUncaught(RuntimeException ex, WebRequest request) {
        var headers = new StringBuilder();
        request.getHeaderNames().forEachRemaining(header -> {
            headers.append("\n").append(header).append(" = ").append(request.getHeader(header));
        });
        var requestUri = ((ServletWebRequest) request).getRequest().getRequestURI();
        log.error("Uncaught exception while streaming content from {} headers {}", requestUri, headers, ex);
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = { MethodArgumentTypeMismatchException.class })
    public ResponseEntity<Object> handleArgumentTypeMismatch(RuntimeException ex) {
        log.debug("Argument type mismatch", ex);
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RangeNotSatisfiableException.class)
    public ResponseEntity<Object> handleRangeNotSatisfiable() {
        return new ResponseEntity<>(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    public void setFedoraContentService(FedoraContentService fedoraContentService) {
        this.fedoraContentService = fedoraContentService;
    }

    public void setAnalyticsTracker(AnalyticsTrackerUtil analyticsTracker) {
        this.analyticsTracker = analyticsTracker;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }
}