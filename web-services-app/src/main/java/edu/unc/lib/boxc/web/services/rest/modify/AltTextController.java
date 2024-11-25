package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.impl.altText.AltTextUpdateService;
import edu.unc.lib.boxc.operations.jms.altText.AltTextUpdateRequest;
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
 * @author bbpennel
 */
@Controller
public class AltTextController {
    @Autowired
    private AltTextUpdateService service;

    @PostMapping(value = "/edit/altText/{id}")
    @ResponseBody
    public ResponseEntity<Object> updateAltText(@PathVariable("id") String id, @RequestParam("altText") String altText) {
        var agent = AgentPrincipalsImpl.createFromThread();
        var altTextRequest = new AltTextUpdateRequest();
        altTextRequest.setPidString(id);
        altTextRequest.setAltText(altText);
        altTextRequest.setAgent(agent);

        var altTextBinary = service.updateAltText(altTextRequest);

        Map<String, Object> result = new HashMap<>();
        result.put("action", "updateAltText");
        result.put("pid", altTextBinary.getPid().getComponentId());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    public void setAltTextUpdateService(AltTextUpdateService service) {
        this.service = service;
    }
}
