package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
public class ViewSettingController {
    private static final Logger LOG = LoggerFactory.getLogger(ViewSettingController.class);
    @Autowired
    private AccessControlService accessControlService;

    @CrossOrigin
    @GetMapping(value = "/edit/viewSettings/{id}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getViewSetting(@PathVariable("id") String id) {
        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        accessControlService.assertHasAccess("Insufficient permissions to get a region for " + id,
                pid, principals, Permission.viewHidden);
    }

}
