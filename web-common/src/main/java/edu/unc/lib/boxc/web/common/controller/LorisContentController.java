/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.boxc.web.common.controller;

import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import edu.unc.lib.boxc.web.common.services.LorisContentService;

/**
 * Controller for requests related to accessing jp2's through loris. Applies cdr access control as a prerequisite to
 * connecting with loris.
 *
 * @author bbpennel
 */
@Controller
public class LorisContentController extends AbstractSolrSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(LorisContentController.class);

    @Autowired
    private LorisContentService lorisContentService;

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private AccessCopiesService accessCopiesService;

    /**
     * Determines if the user is allowed to access a specific datastream on the selected object.
     *
     * @param pid
     * @param datastream
     * @return
     */
    private boolean hasAccess(PID pid, String datastream) {
        // Defaults to jp2 surrogate
        if (datastream == null) {
            datastream = JP2_ACCESS_COPY.getId();
        }

        Permission permission = DatastreamPermissionUtil.getPermissionForDatastream(datastream);

        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        LOG.debug("Checking if user {} has access to {} belonging to object {}.",
                agent.getUsername(), datastream, pid);
        return accessControlService.hasAccess(pid, agent.getPrincipals(), permission);
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
    @GetMapping("/jp2Proxy/{id}/{datastream}/{region}/{size}/{rotation}/{qualityFormat:.+}")
    public void getRegion(@PathVariable("id") String id,
            @PathVariable("datastream") String datastream, @PathVariable("region") String region,
            @PathVariable("size") String size, @PathVariable("rotation") String rotation,
            @PathVariable("qualityFormat") String qualityFormat, HttpServletResponse response) {

        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object
        if (this.hasAccess(pid, datastream)) {
            try {
                String[] qualityFormatArray = qualityFormat.split("\\.");
                String quality = qualityFormatArray[0];
                String format = qualityFormatArray[1];
                response.addHeader("Access-Control-Allow-Origin", "*");
                lorisContentService.streamJP2(
                        id, region, size, rotation, quality, format, datastream,
                        response.getOutputStream(), response);
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
     * @param datastream
     * @param response
     */
    @GetMapping("/jp2Proxy/{id}/{datastream}/info.json")
    public void getMetadata(@PathVariable("id") String id,
            @PathVariable("datastream") String datastream, HttpServletResponse response) {
        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object
        if (this.hasAccess(pid, datastream)) {
            try {
                response.addHeader("Access-Control-Allow-Origin", "*");
                lorisContentService.getMetadata(id, datastream, response.getOutputStream(), response);
            } catch (IOException e) {
                LOG.error("Error retrieving JP2 metadata content for {}", id, e);
            }
        } else {
            LOG.debug("Image access was forbidden to {} for user {}", id, GroupsThreadStore.getUsername());
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }
    }

    /**
     * Handles requests for IIIF canvases
     * @param id
     * @param datastream
     * @param response
     * @return
     */
    @GetMapping(value = "/jp2Proxy/{id}/{datastream}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getCanvas(@PathVariable("id") String id, @PathVariable("datastream") String datastream,
                              HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException {
        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object's manifest
        if (this.hasAccess(pid, datastream)) {
            SimpleIdRequest idRequest = new SimpleIdRequest(pid, GroupsThreadStore
                    .getAgentPrincipals().getPrincipals());
            ContentObjectRecord briefObj = queryLayer.getObjectById(idRequest);
            response.addHeader("Access-Control-Allow-Origin", "*");
            return lorisContentService.getCanvas(request, briefObj);
        } else {
            LOG.debug("Manifest access was forbidden to {} for user {}", id, GroupsThreadStore.getUsername());
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }

        return "";
    }

    /**
     * Handles requests for IIIF sequences
     * @param id
     * @param datastream
     * @param response
     * @return
     */
    @GetMapping(value = "/jp2Proxy/{id}/{datastream}/sequence/normal", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getSequence(@PathVariable("id") String id, @PathVariable("datastream") String datastream,
                              HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException {
        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object's manifest
        if (this.hasAccess(pid, datastream)) {
            List<ContentObjectRecord> briefObjs = getDatastreams(pid);
            response.addHeader("Access-Control-Allow-Origin", "*");
            return lorisContentService.getSequence(request, briefObjs);
        } else {
            LOG.debug("Manifest access was forbidden to {} for user {}", id, GroupsThreadStore.getUsername());
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }

        return "";
    }

    /**
     * Handles requests for IIIF manifests
     * @param id
     * @param datastream
     * @param response
     * @return
     */
    @GetMapping(value = "/jp2Proxy/{id}/{datastream}/manifest", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getManifest(@PathVariable("id") String id, @PathVariable("datastream") String datastream,
                            HttpServletRequest request, HttpServletResponse response) {
        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object's manifest
        if (this.hasAccess(pid, datastream)) {
            try {
                List<ContentObjectRecord> briefObjs = getDatastreams(pid);
                if (briefObjs.size() == 0) {
                    response.setStatus(HttpStatus.NOT_FOUND.value());
                } else {
                    response.addHeader("Access-Control-Allow-Origin", "*");
                    return lorisContentService.getManifest(request, briefObjs);
                }
            } catch (IOException e) {
                LOG.error("Error retrieving manifest content for {}", id, e);
            }
        } else {
            LOG.debug("Manifest access was forbidden to {} for user {}", id, GroupsThreadStore.getUsername());
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }

        return "";
    }

    private List<ContentObjectRecord> getDatastreams(PID pid) {
        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        return accessCopiesService.listViewableFiles(pid, agent.getPrincipals());
    }
}
