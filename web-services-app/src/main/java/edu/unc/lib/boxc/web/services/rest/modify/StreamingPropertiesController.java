package edu.unc.lib.boxc.web.services.rest.modify;

import com.apicatalog.jsonld.StringUtils;
import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
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
    public ResponseEntity<Object> updateStreamingProperties(@RequestParam Map<String,String> allParams) {
        Map<String, Object> result = new HashMap<>();

        if (hasBadParams(allParams)) {
            result.put("error", "Streaming properties request must include action, file ID, folder, and filename");
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }

        var fileId = allParams.get("file");
        var pid = PIDs.get(fileId);
        var agent = getAgentPrincipals();
        AccessGroupSet principals = agent.getPrincipals();
        accessControlService.assertHasAccess("Insufficient permissions to update streaming properties for " +
                        fileId, pid, principals, Permission.ingest);

        var request = buildRequest(allParams);
        try {
            streamingPropertiesRequestSender.sendToQueue(request);
        } catch (IOException e) {
            log.error("Error updating streaming properties for {}", request.getFilePidString(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        result.put("status", "Submitted streaming properties updates for " + fileId);
        result.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private StreamingPropertiesRequest buildRequest(Map<String,String> params) {
        var request = new StreamingPropertiesRequest();
        request.setAction(params.get("action"));
        request.setFilePidString(params.get("file"));
        request.setFolder(params.get("folder"));
        request.setFilename(params.get("filename"));
        return request;
    }

    private boolean hasBadParams(Map<String,String> params) {
        return params.isEmpty() ||
                StringUtils.isBlank(params.get("action")) ||
                StringUtils.isBlank(params.get("filename")) ||
                StringUtils.isBlank(params.get("file")) ||
                StringUtils.isBlank(params.get("folder"));
    }
}
