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
package edu.unc.lib.dl.ui.controller;

import static edu.unc.lib.dl.model.DatastreamType.JP2_ACCESS_COPY;

import java.io.IOException;

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

import edu.unc.lib.dl.acl.fcrepo4.DatastreamPermissionUtil;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ui.service.LorisContentService;

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

        AgentPrincipals agent = AgentPrincipals.createFromThread();
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
     * Handles requests for IIIF manifests
     * @param id
     * @param datastream
     * @param response
     * @return
     */
    @GetMapping("/jp2Proxy/{id}/{datastream}/manifest")
    @ResponseBody
    public String getManifest(@PathVariable("id") String id, @PathVariable("datastream") String datastream,
                            HttpServletRequest request, HttpServletResponse response) {
        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object's manifest
        if (this.hasAccess(pid, datastream)) {
            try {
                return lorisContentService.getManifest(request);
            } catch (IOException e) {
                LOG.error("Error retrieving manifest content for {}", id, e);
            }
        } else {
            LOG.debug("Manifest access was forbidden to {} for user {}", id, GroupsThreadStore.getUsername());
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }

        return "";
    }
}
