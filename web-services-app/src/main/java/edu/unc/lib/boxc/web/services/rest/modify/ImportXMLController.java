package edu.unc.lib.boxc.web.services.rest.modify;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.operations.impl.importxml.ImportXMLService;

/**
 * API endpoint that accepts an XML document containing objects' metadata, kicks off update operations for
 * individual objects, and sends an email with a report of objects that were/were not able to be updated.
 *
 * @author bbpennel
 * @author harring
 */
@Controller
public class ImportXMLController {
    private static final Logger log = LoggerFactory.getLogger(ImportXMLController.class);

    @Autowired
    private ImportXMLService service;

    /**
     * Imports an XML document containing metadata for all objects specified by the request
     *
     * @param xmlFile
     * @return response entity with result and response code
     */
    @PostMapping(value = "/edit/importXML")
    public @ResponseBody ResponseEntity<Object> importXML(@RequestParam("file") MultipartFile xmlFile) {

        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        String userEmail = GroupsThreadStore.getEmail();
        log.info("User with email {} has submitted a bulk metadata update package", userEmail);

        Map<String, Object> result = new HashMap<>();
        result.put("action", "import xml");
        result.put("username", agent.getUsername());
        result.put("user email", userEmail);

        try (InputStream importStream = xmlFile.getInputStream()) {
            service.pushJobToQueue(importStream, agent, userEmail);
        } catch (IOException e) {
            log.error("Error creating or writing to import file: {}", e);
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IllegalArgumentException e) {
            log.error("Error queueing the job: {}", e);
            result.put("error", e.getMessage());
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        result.put("message", "Import of metadata has begun. " + userEmail
                + " will be emailed when the update completes");
        result.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
