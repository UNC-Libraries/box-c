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
package edu.unc.lib.dl.cdr.services.rest.modify;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.cdr.services.processing.XMLExportService;
import edu.unc.lib.dl.fedora.FedoraException;

/**
 * Responds to requests to generate an XML document containing metadata for objects in the selected set of objects,
 * and sends the document to the provided email address.
 *
 * @author sreenug
 * @author bbpennel
 * @author harring
 */
@Controller
public class ExportXMLController {
    private static final Logger log = LoggerFactory.getLogger(ExportXMLController.class);

    @Autowired
    private XMLExportService service;

    /**
     * Exports an XML document containing metadata for all objects specified by the request
     *
     * @param exportRequest
     * @return
     */
    @RequestMapping(value = "/edit/exportXML", method = RequestMethod.POST)
    public @ResponseBody
    Object exportFolder(@RequestBody XMLExportRequest exportRequest) throws IOException, FedoraException {

        return exportXML(exportRequest);
    }

    private ResponseEntity<Object> exportXML(XMLExportRequest exportRequest) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "export xml");

        AgentPrincipals agent = AgentPrincipals.createFromThread();

        try {
            service.exportXml(agent.getUsername(), agent.getPrincipals(), new XMLExportRequest(
                    exportRequest.getPids(), exportRequest.getExportChildren(), exportRequest.getEmail()));
            result.put("message", "Metadata export for " + exportRequest.getPids().size()
                    + " objects has begun, you will receive the data via email soon");
        } catch (Exception e) {
            result.put("error", e.getMessage());
            if (e instanceof AccessRestrictionException) {
                result.put("error", "User must have a username to export xml");
                return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
            } else {
                log.error("Failed to begin export of xml due to ",  e);
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    public static class XMLExportRequest {
        private List<String> pids;
        private boolean exportChildren;
        private String email;

        public XMLExportRequest() {
        }

        public XMLExportRequest(List<String> pids, boolean exportChildren, String email) {
            this.pids = pids;
            this.exportChildren = exportChildren;
            this.email = email;
        }

        public List<String> getPids() {
            return pids;
        }

        public void setPids(List<String> pids) {
            this.pids = pids;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public boolean getExportChildren() {
            return exportChildren;
        }

        public void setExportChildren(boolean exportChildren) {
            this.exportChildren = exportChildren;
        }
    }

}
