package edu.unc.lib.boxc.web.services.rest;

import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.unc.lib.boxc.web.common.controllers.AbstractSolrSearchController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.api.services.DatastreamPermissionUtil;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.web.common.services.AccessCopiesService;
import edu.unc.lib.boxc.web.services.processing.ImageServerV2Service;

/**
 * Controller for requests related to accessing jp2's through a v2 iiif server. Applies boxc access control
 *
 * @author bbpennel
 */
@Controller
public class ImageServerV2Controller extends AbstractSolrSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(ImageServerV2Controller.class);

    @Autowired
    private ImageServerV2Service imageServerV2Service;

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private AccessCopiesService accessCopiesService;

    private void assertHasAccess(PID pid) {
        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        accessControlService.assertHasAccess("Insufficient permissions for " + pid.getId(),
                pid, agent.getPrincipals(), Permission.viewAccessCopies);
    }

    /**
     * Handles requests for individual region tiles.
     * @param id
     * @param datastream
     * @param region
     * @param size
     * @param rotation
     * @param qualityFormat
     * @param response
     */
    @GetMapping("/iiif/v2/{id}/{datastream}/{region}/{size}/{rotation}/{qualityFormat:.+}")
    public void getRegion(@PathVariable("id") String id,
            @PathVariable("datastream") String datastream, @PathVariable("region") String region,
            @PathVariable("size") String size, @PathVariable("rotation") String rotation,
            @PathVariable("qualityFormat") String qualityFormat, HttpServletResponse response) {

        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object
        assertHasAccess(pid);
        try {
            String[] qualityFormatArray = qualityFormat.split("\\.");
            String quality = qualityFormatArray[0];
            String format = qualityFormatArray[1];
            response.addHeader("Access-Control-Allow-Origin", "*");
            imageServerV2Service.streamJP2(
                    id, region, size, rotation, quality, format,
                    response.getOutputStream(), response);
        } catch (IOException e) {
            LOG.error("Error retrieving streaming JP2 content for {}", id, e);
        }
    }

    /**
     * Handles requests for jp2 metadata
     *
     * @param id
     * @param datastream
     * @param response
     */
    @GetMapping("/iiif/v2/{id}/{datastream}/info.json")
    public void getMetadata(@PathVariable("id") String id,
            @PathVariable("datastream") String datastream, HttpServletResponse response) {
        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object
        assertHasAccess(pid);
        try {
            addAllowOriginHeader(response);
            imageServerV2Service.getMetadata(id, response.getOutputStream(), response);
        } catch (IOException e) {
            LOG.error("Error retrieving JP2 metadata content for {}", id, e);
        }
    }

    private void addAllowOriginHeader(HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
    }

    /**
     * Handles requests for IIIF canvases
     * @param id
     * @param datastream
     * @param response
     * @return
     */
    @GetMapping(value = "/iiif/v2/{id}/{datastream}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getCanvas(@PathVariable("id") String id, @PathVariable("datastream") String datastream,
                              HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException {
        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object's manifest
        assertHasAccess(pid);
        SimpleIdRequest idRequest = new SimpleIdRequest(pid, GroupsThreadStore
                .getAgentPrincipals().getPrincipals());
        ContentObjectRecord briefObj = queryLayer.getObjectById(idRequest);
        addAllowOriginHeader(response);
        return imageServerV2Service.getCanvas(id, datastream, briefObj);
    }

    /**
     * Handles requests for IIIF sequences
     * @param id
     * @param datastream
     * @param response
     * @return
     */
    @GetMapping(value = "/iiif/v2/{id}/{datastream}/sequence/normal", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getSequence(@PathVariable("id") String id, @PathVariable("datastream") String datastream,
                              HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException {
        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object's manifest
        assertHasAccess(pid);
        List<ContentObjectRecord> briefObjs = getDatastreams(pid);
        addAllowOriginHeader(response);
        return imageServerV2Service.getSequence(id, datastream, briefObjs);
    }

    /**
     * Handles requests for IIIF manifests
     * @param id
     * @param datastream
     * @param response
     * @return
     */
    @GetMapping(value = "/iiif/v2/{id}/{datastream}/manifest" , produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getManifest(@PathVariable("id") String id, @PathVariable("datastream") String datastream,
                            HttpServletRequest request, HttpServletResponse response) {
        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object's manifest
        assertHasAccess(pid);
        try {
            List<ContentObjectRecord> briefObjs = getDatastreams(pid);
            if (briefObjs.size() == 0) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
            } else {
                addAllowOriginHeader(response);
                return imageServerV2Service.getManifest(id, datastream, briefObjs);
            }
        } catch (IOException e) {
            LOG.error("Error retrieving manifest content for {}", id, e);
        }

        return "";
    }

    private List<ContentObjectRecord> getDatastreams(PID pid) {
        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        return accessCopiesService.listViewableFiles(pid, agent.getPrincipals());
    }

    public void setImageServerV2Service(ImageServerV2Service imageServerV2Service) {
        this.imageServerV2Service = imageServerV2Service;
    }

    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    public void setAccessCopiesService(AccessCopiesService accessCopiesService) {
        this.accessCopiesService = accessCopiesService;
    }
}