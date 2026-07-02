package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.operations.impl.wcagCompliance.WcagComplianceService;
import edu.unc.lib.boxc.operations.jms.wcagCompliance.WcagComplianceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Controller for setting WCAG compliance level on a FileObject
 */
@Controller
public class WcagComplianceController {
    @Autowired
    private WcagComplianceService service;

    @PutMapping(value = "/edit/wcagCompliance/{pidString}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> updateWcagCompliance(WcagComplianceRequest request) {
        request.setAgent(getAgentPrincipals());
        service.updateWcagCompliance(request);

        Map<String, Object> result = new HashMap<>();
        result.put("action", "updateWcagCompliance");
        result.put("pid", request.getPidString());
        result.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    public void setService(WcagComplianceService service) {
        this.service = service;
    }
}
