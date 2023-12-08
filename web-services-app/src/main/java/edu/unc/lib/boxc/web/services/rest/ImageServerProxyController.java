package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.api.services.DatastreamPermissionUtil;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.web.services.processing.ImageServerProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Controller that handles IIIF V3 requests to the image server
 * @author snluong
 */
@Controller
public class ImageServerProxyController {
    private static final Logger LOG = LoggerFactory.getLogger(ImageServerProxyController.class);

    @Autowired
    private ImageServerProxyService imageServerProxyService;

    @Autowired
    private AccessControlService accessControlService;

    /**
     * Determines if the user is allowed to access a JP2 datastream on the selected object.
     *
     * @param pid
     * @return
     */
    private boolean hasAccess(PID pid) {
        var datastream = JP2_ACCESS_COPY.getId();

        Permission permission = DatastreamPermissionUtil.getPermissionForDatastream(datastream);

        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        LOG.debug("Checking if user {} has access to {} belonging to object {}.",
                agent.getUsername(), datastream, pid);
        return accessControlService.hasAccess(pid, agent.getPrincipals(), permission);
    }

    /**
     * Handles requests for individual region tiles.
     * @param id
     * @param region
     * @param size
     * @param rotation
     * @param qualityFormat
     */
    @CrossOrigin
    @GetMapping("/iiif/v3/{id}/{region}/{size}/{rotation}/{qualityFormat:.+}")
    public ResponseEntity<InputStreamResource> getRegion(@PathVariable("id") String id,
                          @PathVariable("region") String region,
                          @PathVariable("size") String size, @PathVariable("rotation") String rotation,
                          @PathVariable("qualityFormat") String qualityFormat) {

        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object
        if (this.hasAccess(pid)) {
            try {
                String[] qualityFormatArray = qualityFormat.split("\\.");
                String quality = qualityFormatArray[0];
                String format = qualityFormatArray[1];
                return imageServerProxyService.streamJP2(
                        id, region, size, rotation, quality, format, 1);
            } catch (IOException e) {
                LOG.error("Error retrieving streaming JP2 content for {}", id, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            LOG.debug("Access was forbidden to {} for user {}", id, GroupsThreadStore.getUsername());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

//    @GetMapping("/iiif/v3/{id}/info.json")
//    public void getMetadata(@PathVariable("id") String id, HttpServletResponse response) {
//        PID pid = PIDs.get(id);
//        // Check if the user is allowed to view this object
//        if (this.hasAccess(pid)) {
//            try {
//                response.addHeader("Access-Control-Allow-Origin", "*");
//                imageServerProxyService.getMetadata(id, response.getOutputStream(), response, 1);
//            } catch (IOException e) {
//                LOG.error("Error retrieving JP2 metadata content for {}", id, e);
//                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
//            }
//        } else {
//            LOG.debug("Image access was forbidden to {} for user {}", id, GroupsThreadStore.getUsername());
//            response.setStatus(HttpStatus.FORBIDDEN.value());
//        }
//    }

    /**
     * Handles requests for jp2 metadata
     *
     * @param id
     * @param response
     */
//    @CrossOrigin
//    @GetMapping("/iiif/v3/{id}/info.json", produces = APPLICATION_JSON_VALUE)
//    public ResponseEntity<InputStreamResource> getMetadata(@PathVariable("id") String id) {
//        PID pid = PIDs.get(id);
//        // Check if the user is allowed to view this object
//        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
//        AccessGroupSet principals = agent.getPrincipals();
//        accessControlService.assertHasAccess("Insufficient permissions to download access copy for " + id,
//                pid, principals, Permission.viewAccessCopies);
//        try {
//            return imageServerProxyService.getMetadata(id, 1);
//        } catch (IOException e) {
//            LOG.error("Error retrieving JP2 metadata content for {}", id, e);
//            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//    }
}
