package edu.unc.lib.boxc.web.services.rest.modify;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.fcrepo.exceptions.AuthorizationException;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.web.services.processing.AddContainerService;
import edu.unc.lib.boxc.web.services.processing.AddContainerService.AddContainerRequest;

/**
 * API controller for creating new containers
 *
 * @author harring
 *
 */
@Controller
public class AddContainerController {
    private static final Logger log = LoggerFactory.getLogger(AddContainerController.class);

    @Autowired
    private AddContainerService addContainerService;

    @RequestMapping(value = "edit/create/adminUnit/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Object> createAdminUnit(AddContainerRequest addRequest) {
        return createContainer(addRequest.withContainerType(ResourceType.AdminUnit));
    }

    @RequestMapping(value = "edit/create/collection/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Object> createCollection(AddContainerRequest addRequest) {
        return createContainer(addRequest.withContainerType(ResourceType.Collection));
    }

    @RequestMapping(value = "edit/create/folder/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Object> createFolder(AddContainerRequest addRequest) {
        return createContainer(addRequest.withContainerType(ResourceType.Folder));
    }

    @RequestMapping(value = "edit/create/work/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Object> createWork(AddContainerRequest addRequest) {
        return createContainer(addRequest.withContainerType(ResourceType.Work));
    }

    private ResponseEntity<Object> createContainer(AddContainerRequest addRequest) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "create");
        result.put("pid", addRequest.getParentPid().getId());

        try {
            addContainerService.addContainer(addRequest.withAgent(AgentPrincipalsImpl.createFromThread()));
        } catch (Exception e) {
            result.put("error", e.getMessage());
            Throwable t = e.getCause();
            if (t instanceof AuthorizationException || t instanceof AccessRestrictionException) {
                return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
            } else {
                log.error("Failed to create container for {}",  e);
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
