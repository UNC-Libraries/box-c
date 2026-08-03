package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.OriginalFileVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Controller that handles Fedora version requests
 * @author snluong
 */

@Controller
public class OriginalFileVersionsController {
    @Autowired
    private AccessControlService accessControlService;
    @Autowired
    private OriginalFileVersionService service;

    @RequestMapping(value = "/version/{id}", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<Object> getVersion(@PathVariable("id") String id) {
        PID pid = PIDs.get(id);
        
        // Check if the user is allowed to view this object's metadata
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        accessControlService.assertHasAccess("Insufficient permissions to get versions for " + id,
                pid, principals, Permission.viewMetadata);

        var metadata = service.getVersionMetadata(pid);
        return new ResponseEntity<>(metadata, HttpStatus.OK);
    }

    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    public void setService(OriginalFileVersionService service) {
        this.service = service;
    }
}
