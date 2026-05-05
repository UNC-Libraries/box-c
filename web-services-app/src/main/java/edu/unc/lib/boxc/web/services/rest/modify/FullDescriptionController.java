package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.impl.fullDescription.FullDescriptionUpdateService;
import edu.unc.lib.boxc.operations.jms.fullDescription.FullDescriptionUpdateRequest;
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
 * Controller for handling full description update requests
 */
@Controller
public class FullDescriptionController {
    @Autowired
    private FullDescriptionUpdateService fullDescriptionUpdateService;

    @PostMapping(value = "/edit/fullDescription/{id}")
    @ResponseBody
    public ResponseEntity<Object> updateFullDescription(@PathVariable("id") String id, @RequestParam("fullDescription") String fullDescription) {
        var agent = AgentPrincipalsImpl.createFromThread();
        var request = new FullDescriptionUpdateRequest();
        request.setPidString(id);
        request.setFullDescriptionText(fullDescription);
        request.setAgent(agent);

        var fullDescBinary = fullDescriptionUpdateService.updateFullDescription(request);

        Map<String, Object> result = new HashMap<>();
        result.put("action", "updateFullDescription");
        result.put("pid", fullDescBinary.getPid().getComponentId());
        result.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    public void setFullDescriptionUpdateService(FullDescriptionUpdateService fullDescriptionUpdateService) {
        this.fullDescriptionUpdateService = fullDescriptionUpdateService;
    }
}
