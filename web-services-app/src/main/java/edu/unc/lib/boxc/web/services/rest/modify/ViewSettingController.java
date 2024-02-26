package edu.unc.lib.boxc.web.services.rest.modify;

import com.apicatalog.jsonld.StringUtils;
import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.CdrView;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.viewSettings.ViewSettingRequest;
import edu.unc.lib.boxc.operations.jms.viewSettings.ViewSettingRequestSender;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.operations.jms.viewSettings.ViewSettingRequest.ViewBehavior.caseInsensitiveValueOf;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Controller for handling view setting requests. This may include things like setting a viewBehavior.
 */
@Controller
public class ViewSettingController {
    private static final Logger log = LoggerFactory.getLogger(ViewSettingController.class);
    @Autowired
    private AccessControlService accessControlService;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    private ViewSettingRequestSender viewSettingRequestSender;

    /**
     * This endpoint gets the view settings of the object
     * @param id UUID of the object
     * @return json with the UUID and key value pairs that are the view setting : value and response status
     */
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

    @PutMapping(value = "/edit/viewSettings", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> updateViewSetting(@RequestParam Map<String,String> allParams) {
        Map<String, Object> result = new HashMap<>();

        if (hasBadParams(allParams)) {
            result.put("error", "Request must include ids and view settings");
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }

        // see if view behavior is valid
        var viewBehavior = caseInsensitiveValueOf(allParams.get("behavior"));

        var ids = allParams.get("targets").split(",");
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        for (String id : ids) {
            var pid = PIDs.get(id);
            // check permissions for object first
            accessControlService.assertHasAccess("Insufficient permissions to edit view settings for " + id,
                    pid, principals, Permission.editViewSettings);

            //check if object is a work
            var repositoryObject = repositoryObjectLoader.getRepositoryObject(pid);
            if (!(repositoryObject instanceof WorkObject)) {
                throw new InvalidOperationForObjectType("Cannot update View Settings of object " +
                        id + ", as only WorkObjects have View Settings");
            }
        }

        // now build and send requests
        for (String id : ids) {
            var request = buildRequest(id, viewBehavior);
            try {
                viewSettingRequestSender.sendToQueue(request);
            } catch (IOException e) {
                log.error("Error updating view setting for {}", request.getObjectPidString(), e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }

        result.put("ids", ids);
        result.put("status", "Submitted view setting updates for " + ids.length + " object(s)");
        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private String getValue(Resource resource, Property property) {
        var propValue = resource.getProperty(property);
        return propValue == null ? null : propValue.getString();
    }

    private ViewSettingRequest buildRequest(String id, ViewSettingRequest.ViewBehavior viewBehavior) {
        var request = new ViewSettingRequest();
        request.setObjectPidString(id);
        request.setViewBehavior(viewBehavior);
        return request;
    }

    private boolean hasBadParams(Map<String,String> params) {
        return params.isEmpty() || StringUtils.isBlank(params.get("targets"));
    }
}
