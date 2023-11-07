package edu.unc.lib.boxc.web.common.controllers;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.api.services.DatastreamPermissionUtil;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.web.common.services.ImageServerProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;

/**
 * @author snluong
 */
@Controller
public class ImageServerProxyController extends AbstractSolrSearchController {
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
     * @param response
     */
    @GetMapping("/iiif/v3/{id}/{region}/{size}/{rotation}/{qualityFormat:.+}")
    public void getRegion(@PathVariable("id") String id,
                          @PathVariable("region") String region,
                          @PathVariable("size") String size, @PathVariable("rotation") String rotation,
                          @PathVariable("qualityFormat") String qualityFormat, HttpServletResponse response) {

        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object
        if (this.hasAccess(pid)) {
            try {
                String[] qualityFormatArray = qualityFormat.split("\\.");
                String quality = qualityFormatArray[0];
                String format = qualityFormatArray[1];
                response.addHeader("Access-Control-Allow-Origin", "*");
                imageServerProxyService.streamJP2(
                        id, region, size, rotation, quality, format,
                        response.getOutputStream(), response, 1);
            } catch (IOException e) {
                LOG.error("Error retrieving streaming JP2 content for {}", id, e);
            }
        } else {
            LOG.debug("Access was forbidden to {} for user {}", id, GroupsThreadStore.getUsername());
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }
    }

    /**
     * Handles requests for jp2 metadata
     *
     * @param id
     * @param response
     */
    @GetMapping("/iiif/v3/{id}/info.json")
    public void getMetadata(@PathVariable("id") String id, HttpServletResponse response) {
        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object
        if (this.hasAccess(pid)) {
            try {
                response.addHeader("Access-Control-Allow-Origin", "*");
                imageServerProxyService.getMetadata(id, response.getOutputStream(), response, 1);
            } catch (IOException e) {
                LOG.error("Error retrieving JP2 metadata content for {}", id, e);
            }
        } else {
            LOG.debug("Image access was forbidden to {} for user {}", id, GroupsThreadStore.getUsername());
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }
    }
}
