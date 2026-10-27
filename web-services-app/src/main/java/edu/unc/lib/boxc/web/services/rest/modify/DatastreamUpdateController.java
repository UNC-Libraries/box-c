package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.operations.impl.datastreams.DatastreamUpdateService;
import edu.unc.lib.boxc.operations.jms.datastreams.DatastreamUpdateRequest;
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

@Controller
public class DatastreamUpdateController {
    @Autowired
    DatastreamUpdateService datastreamUpdateService;

    @PostMapping(value = "/edit/datastreamText/{id}/{datastreamType}")
    @ResponseBody
    public ResponseEntity<Object> updateDatastreamText(@PathVariable("id") String id,
                                                       @PathVariable("datastreamType") String datastreamType,
                                                       @RequestParam("datastreamText") String datastreamText) {
        var agent = AgentPrincipalsImpl.createFromThread();
        var request = new DatastreamUpdateRequest();
        request.setPidString(id);
        request.setText(datastreamText);
        request.setAgent(agent);
        request.setDatastreamType(DatastreamType.getByIdentifier(datastreamType));

        var datastreamBinary = datastreamUpdateService.updateDatastreamText(request);

        Map<String, Object> result = new HashMap<>();
        result.put("action", "update_" + datastreamType);
        result.put("pid", datastreamBinary.getPid().getComponentId());
        result.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    public void setDatastreamUpdateService(DatastreamUpdateService datastreamUpdateService) {
        this.datastreamUpdateService = datastreamUpdateService;
    }
}
