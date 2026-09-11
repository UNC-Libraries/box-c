package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.sparql.vocabulary.FOAF;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
            asList(Premis.FilenameChange, Premis.Modification, Premis.Ingestion));
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
        List<Map<String, String>> publicEvents = new ArrayList<>();
        for (Resource eventType : PUBLIC_EVENTS) {
            var events = logModel.listResourcesWithProperty(RDF.type, eventType);
            while (events.hasNext()) {
                var formattedEvent = formatEvent(events.next(), logModel);
                publicEvents.add(formattedEvent);
            }
        }
        // sort in chronological order by timestamp
        publicEvents.sort(Comparator.comparing(m -> m.get("timestamp")));

        return new ResponseEntity<>(publicEvents, HttpStatus.OK);
    }

    private Map<String, String> formatEvent(Resource eventResource, Model model) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("username", getUsername(eventResource, model));
        metadata.put("timestamp", eventResource.getProperty(DCTerms.date).getLiteral().getValue().toString());
        metadata.put("note", eventResource.getProperty(Premis.note).getString());
        return metadata;
    }

    private String getUsername(Resource eventResource, Model model) {
        String unformattedUsername = null;
        String formattedUsername;
        var authorizer = eventResource.getProperty(Premis.hasEventRelatedAgentAuthorizor);
        if (authorizer != null) {
            unformattedUsername = authorizer.getObject().toString();
        } else {
            var implementor = eventResource.getProperty(Premis.hasEventRelatedAgentImplementor);
            if (implementor != null) {
                unformattedUsername = implementor.getObject().toString();
            }
        }

        if (unformattedUsername == null) {
            return null;
        }

        // if it contains a # it is a reference to a person resource
        // http://example.com/rest/content/path/event1635532842876104774973909727#authorizingAgent-bbpennel
        if (unformattedUsername.contains("#")) {
            var resource = model.getResource(unformattedUsername);
            formattedUsername = resource.getProperty(FOAF.name).getString();
        } else {
            // username looks like http://example.com/rest/agents/person/onyen/bbpennel
            var stringArray = unformattedUsername.split("/");
            formattedUsername = stringArray[stringArray.length - 1];
        }
        return formattedUsername;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }
}
