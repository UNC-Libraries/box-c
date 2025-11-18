package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.collectionDisplay.CollectionDisplayPropertiesRequest;
import edu.unc.lib.boxc.operations.jms.collectionDisplay.CollectionDisplayPropertiesRequestSender;
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
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Controller for setting default display properties on a CollectionObject
 */
@Controller
public class EditCollectionDisplayController {
    private static final Logger log = LoggerFactory.getLogger(EditCollectionDisplayController.class);
    @Autowired
    private AccessControlService accessControlService;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    private CollectionDisplayPropertiesRequestSender collectionDisplayPropertiesRequestSender;

    @GetMapping(value = "/edit/collectionDisplay/{id}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getCollectionDisplayProperties(@PathVariable("id") String id) {
        PID pid = PIDs.get(id);
        // Check if the user is allowed to view this object
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        accessControlService.assertHasAccess("Insufficient permissions to get collection display properties for " + id,
                pid, principals, Permission.viewHidden);

        // check if object is a CollectionObject
        var repositoryObject = repositoryObjectLoader.getRepositoryObject(pid);
        if (!(repositoryObject instanceof CollectionObject)) {
            throw new InvalidOperationForObjectType("Cannot get collection display properties of type " +
                    repositoryObject.getClass().getName() + ", as only CollectionObjects have collection display properties");
        }

        var resource = repositoryObject.getResource();
        var properties = getProperties(id, resource);

        return new ResponseEntity<>(properties, HttpStatus.OK);
    }

    @PutMapping(value = "/edit/collectionDisplay", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> editCollectionDisplay(CollectionDisplayPropertiesRequest collectionDisplayRequest) {
        Map<String, Object> result = new HashMap<>();

        var collectionId = collectionDisplayRequest.getId();
        var pid = PIDs.get(collectionId);
        var agent = getAgentPrincipals();
        AccessGroupSet principals = agent.getPrincipals();
        accessControlService.assertHasAccess("Insufficient permissions to update collection display properties for " +
                collectionId, pid, principals, Permission.ingest);

        collectionDisplayRequest.setAgent(agent);

        try {
            collectionDisplayPropertiesRequestSender.sendToQueue(collectionDisplayRequest);
        } catch (IOException e) {
            log.error("Error updating collection display properties for {}", collectionDisplayRequest.getId(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        result.put("status", "Submitted collection display properties updates for " + collectionId);
        result.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private String getValue(Resource resource, Property property) {
        var propValue = resource.getProperty(property);
        return propValue == null ? null : propValue.getString();
    }

    private Map<String, String> getProperties(String id, Resource resource) {
        Map<String, String> result = new HashMap<>();
        var collectionDisplaySettings = getValue(resource, Cdr.collectionDefaultDisplaySettings);

        result.put("id", id);
        result.put("collectionDefaultDisplaySettings", collectionDisplaySettings);
        return result;
    }
}