package edu.unc.lib.boxc.web.services.rest.modify;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.jms.exportxml.ExportXMLRequest;
import edu.unc.lib.boxc.operations.jms.exportxml.ExportXMLRequestService;

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
    private ExportXMLRequestService service;

    /**
     * Exports an XML document containing metadata for all objects specified by the request
     *
     * @param exportRequest
     * @return
     */
    @PostMapping(value = "/edit/exportXML")
    public @ResponseBody
    Object exportFolder(@RequestBody ExportXMLRequest exportRequest) {
        return exportXML(exportRequest);
    }

    private ResponseEntity<Object> exportXML(ExportXMLRequest exportRequest) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "export xml");

        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();

        try {
            exportRequest.setAgent(agent);
            exportRequest.setRequestedTimestamp(Instant.now());
            service.sendRequest(exportRequest);
            result.put("message", "Metadata export for " + exportRequest.getPids().size()
                    + " objects has begun, you will receive the data via email soon");
        } catch (AccessRestrictionException e) {
            result.put("error", "User must have a username to export xml");
            return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            result.put("error", "Failed to begin export of xml: " + e.getMessage());
            log.error("Failed to begin export of xml",  e);
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
