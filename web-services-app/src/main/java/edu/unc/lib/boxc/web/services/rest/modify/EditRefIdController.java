package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.exceptions.InvalidPidException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.operations.impl.aspace.RefIdRequest;
import edu.unc.lib.boxc.operations.impl.aspace.RefIdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * API controller for editing the ArchivesSpace Ref ID associated with a WorkObject.
 */
@Controller
public class EditRefIdController {
    private static final Logger log = LoggerFactory.getLogger(EditRefIdController.class);

    @Autowired
    RefIdService service;

    @PostMapping(value = "/edit/aspace/updateRefId/{pid}")
    @ResponseBody
    public ResponseEntity<Object> updateAspaceRefId(@PathVariable("pid") String pid, @RequestParam("aspaceRefId") String refId) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "editAspaceRefID");
        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        try {
            var request = buildRequest(refId, pid, agent);
            service.updateRefId(request);

            result.put("status", "Updated object with PID" + pid + " with Aspace Ref ID: " + refId);
            result.put("timestamp", System.currentTimeMillis());
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (InvalidPidException | AccessRestrictionException | InvalidOperationForObjectType e) {
            throw e;
        } catch (RepositoryException e) {
            log.error("Error editing Aspace Ref ID for {}", agent.getUsername(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private RefIdRequest buildRequest(String refId, String pidString, AgentPrincipals agent) {
        var request = new RefIdRequest();
        request.setRefId(refId);
        request.setPidString(pidString);
        request.setAgent(agent);
        return request;
    }

    public void setService(RefIdService service) {
        this.service = service;
    }
}
