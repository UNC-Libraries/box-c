package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.impl.wcagCompliance.WcagComplianceService;
import edu.unc.lib.boxc.operations.jms.wcagCompliance.WcagComplianceRequest;
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

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
public class WcagComplianceController {
    @Autowired
    private WcagComplianceService service;

    @PostMapping(value = "/edit/wcagCompliance/{id}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> updateWcagCompliance(@PathVariable("id") String id, @RequestParam("level") String level) {
        var agent = AgentPrincipalsImpl.createFromThread();
        var request = new WcagComplianceRequest();
        request.setAgent(agent);
        request.setPidString(id);
        request.setLevel(level);

        service.updateWcagCompliance(request);

        Map<String, Object> result = new HashMap<>();
        result.put("action", "updateWcagCompliance");
        result.put("pid", id);
        result.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    public void setService(WcagComplianceService service) {
        this.service = service;
    }
}
