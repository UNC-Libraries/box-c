package edu.unc.lib.boxc.web.services.rest.modify;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.thumbnail.ThumbnailRequest;
import edu.unc.lib.boxc.operations.jms.thumbnail.ThumbnailRequestSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.web.services.processing.ImportThumbnailService;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;

/**
 * Controller for handling thumbnail requests, including:
 * upload submission for collection display thumbnails,
 * assigning a child object to use as a thumbnail,
 * and deleting an assigned child object thumbnail
 *
 * @author lfarrell
 *
 */
@Controller
public class ThumbnailController {
    private static final Logger log = LoggerFactory.getLogger(ThumbnailController.class);

    @Autowired
    private ImportThumbnailService service;
    @Autowired
    private ThumbnailRequestSender thumbnailRequestSender;
    @Autowired
    private AccessControlService aclService;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;

    /**
     * This endpoint uploads a file to use as the thumbnail
     * @param pid PID of object getting a thumbnail
     * @param thumbnailFile file to use as thumbnail
     * @return
     */
    @PostMapping(value = "/edit/displayThumbnail/{pid}")
    public @ResponseBody
    ResponseEntity<Object> importThumbnail(@PathVariable("pid") String pid,
                                                     @RequestParam("file") MultipartFile thumbnailFile) {

        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        String mimeType = thumbnailFile.getContentType();

        Map<String, Object> result = new HashMap<>();
        result.put("action", "editThumbnail");
        result.put("username", agent.getUsername());

        try (InputStream importStream = thumbnailFile.getInputStream()) {
            service.run(importStream, agent, pid, mimeType);
        } catch (IOException e) {
            log.error("Failed to get submitted file", e);
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalArgumentException e) {
            log.error("Error queueing the job", e);
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        result.put("destination", pid);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * This endpoint assigns a child file to use as the thumbnail for the parent work
     * @param pidString ID of the file
     * @return HTTP status
     */
    @PutMapping(value = "/edit/assignThumbnail/{pidString}")
    @ResponseBody
    public ResponseEntity<Object> assignThumbnail(@PathVariable("pidString") String pidString) {
        PID pid = PIDs.get(pidString);

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        aclService.assertHasAccess("Insufficient permissions to assign thumbnail for " + pidString,
                pid, principals, Permission.editDescription);

        var object = repositoryObjectLoader.getRepositoryObject(pid);
        if (!(object instanceof FileObject)) {
            log.error("Error object is not a file: {}", pidString);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        var agent = AgentPrincipalsImpl.createFromThread();
        var request = new ThumbnailRequest();
        request.setAgent(agent);
        request.setFilePidString(pidString);
        request.setAction(ThumbnailRequest.ASSIGN);

        try {
            thumbnailRequestSender.sendToQueue(request);
        } catch (IOException e) {
            log.error("Error assigning file {} as thumbnail", request.getFilePidString(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping(value = "/edit/deleteThumbnail/{pidString}")
    @ResponseBody
    public ResponseEntity<Object> deleteThumbnail(@PathVariable("pidString") String pidString) {
        PID pid = PIDs.get(pidString);

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        aclService.assertHasAccess("Insufficient permissions to assign thumbnail for " + pidString,
                pid, principals, Permission.editDescription);

        var object = repositoryObjectLoader.getRepositoryObject(pid);
        if (!(object instanceof FileObject)) {
            log.error("Error object is not a file: {}", pidString);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        var agent = AgentPrincipalsImpl.createFromThread();
        var request = new ThumbnailRequest();
        request.setAgent(agent);
        request.setFilePidString(pidString);
        request.setAction(ThumbnailRequest.DELETE);
        try {
            thumbnailRequestSender.sendToQueue(request);
        } catch (IOException e) {
            log.error("Error deleting assigned thumbnail for {}", request.getFilePidString(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
