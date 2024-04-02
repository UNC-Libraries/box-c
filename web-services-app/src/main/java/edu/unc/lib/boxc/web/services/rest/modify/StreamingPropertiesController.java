package edu.unc.lib.boxc.web.services.rest.modify;

import com.apicatalog.jsonld.StringUtils;
import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest;
import edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequestSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest.ADD;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Controller for setting streaming properties on a FileObject
 */
@Controller
public class StreamingPropertiesController {
    private static final Logger log = LoggerFactory.getLogger(StreamingPropertiesController.class);
    @Autowired
    private AccessControlService accessControlService;
    @Autowired
    private StreamingPropertiesRequestSender streamingPropertiesRequestSender;

    @PutMapping(value = "/edit/streamingProperties", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> updateStreamingProperties(StreamingPropertiesRequest streamingRequest) {
        Map<String, Object> result = new HashMap<>();

        if (hasBadParams(streamingRequest)) {
            result.put("error", "The streaming properties request is missing a required parameter");
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }

        var fileId = streamingRequest.getId();
        var pid = PIDs.get(fileId);
        var agent = getAgentPrincipals();
        AccessGroupSet principals = agent.getPrincipals();
        accessControlService.assertHasAccess("Insufficient permissions to update streaming properties for " +
                        fileId, pid, principals, Permission.ingest);

        streamingRequest.setAgent(agent);
        try {
            streamingPropertiesRequestSender.sendToQueue(streamingRequest);
        } catch (IOException e) {
            log.error("Error updating streaming properties for {}", streamingRequest.getId(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        result.put("status", "Submitted streaming properties updates for " + fileId);
        result.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private boolean hasBadParams(StreamingPropertiesRequest request) {
        var action = request.getAction();
        if (StringUtils.isBlank(action) || StringUtils.isBlank(request.getId())) {
            return true;
        }
        if (Objects.equals(ADD, action)) {
            return StringUtils.isBlank(request.getFilename()) || StringUtils.isBlank(request.getFolder());
        }

        return false;
    }
}
