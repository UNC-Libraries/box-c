package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.CdrView;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
public class ViewSettingController {
    private static final Logger LOG = LoggerFactory.getLogger(ViewSettingController.class);
    @Autowired
    private AccessControlService accessControlService;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;

    @CrossOrigin
    @GetMapping(value = "/edit/viewSettings/{id}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getViewSetting(@PathVariable("id") String id) {
        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        accessControlService.assertHasAccess("Insufficient permissions to get view settings for " + id,
                pid, principals, Permission.viewHidden);

        // check if object is a WorkObject
        var repositoryObject = repositoryObjectLoader.getRepositoryObject(pid);
        if (!(repositoryObject instanceof WorkObject)) {
            throw new InvalidOperationForObjectType("Cannot get View Settings of type " +
                    repositoryObject.getClass().getName() + ", as only WorkObjects have View Settings");
        }

        var behavior = getValue(repositoryObject.getResource(), CdrView.viewBehavior);
        Map<String, String> result = new HashMap<>();
        result.put("id", id);
        result.put("viewBehavior", behavior);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private String getValue(Resource resource, Property property) {
        var propValue = resource.getProperty(property);
        return propValue == null ? null : propValue.getString();
    }

}
