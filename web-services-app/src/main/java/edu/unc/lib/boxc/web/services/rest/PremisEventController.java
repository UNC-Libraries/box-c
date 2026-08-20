package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static java.util.Arrays.asList;

/**
 * Controller for handling requests about premis events
 * @author snluong
 */
@Controller
public class PremisEventController {
    private static final Logger log = LoggerFactory.getLogger(PremisEventController.class);
    private static final Set<Resource> PUBLIC_EVENTS = new HashSet<>(
            asList(Premis.FilenameChange, Premis.MetadataModification, Premis.Ingestion));
    @Autowired
    private AccessControlService aclService;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;

    @RequestMapping(value = "/premisEvents/{id}", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity<Object> getEvents(@PathVariable("id") String id) {
        PID pid = PIDs.get(id);

        // Check if the user is allowed to view this object's metadata
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        aclService.assertHasAccess("Insufficient permissions to get premis events for " + id,
                pid, principals, Permission.viewMetadata);

        var object = repositoryObjectLoader.getRepositoryObject(pid);
        if (!(object instanceof FileObject)) {
            log.error("Error object is not a file: {}", id);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        var logModel = object.getPremisLog().getEventsModel();
        var publicEvents = new ArrayList<>();
        for (Resource resource : PUBLIC_EVENTS) {
            var formattedEvents = formatEvents(logModel.getResource(resource.getURI()));
            publicEvents.add(formattedEvents);
        }
        return new ResponseEntity<>(publicEvents, HttpStatus.OK);
    }

    private Map<String, String> formatEvents(Resource eventResource) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("username", eventResource.getProperty(Premis.hasEventRelatedAgentAuthorizor).getString());
        metadata.put("timestamp", eventResource.getProperty(DC.date).getString());
        metadata.put("note", eventResource.getProperty(Premis.note).getString());
        return metadata;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }
}
