package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.impl.transcript.TranscriptUpdateService;
import edu.unc.lib.boxc.operations.jms.transcript.TranscriptUpdateRequest;
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
 * Controller for updating transcript datastream
 */
@Controller
public class TranscriptController {
    @Autowired
    private TranscriptUpdateService service;

    @PostMapping(value = "/edit/transcript/{id}")
    @ResponseBody
    public ResponseEntity<Object> updateTranscript(@PathVariable("id") String id, @RequestParam("transcript") String transcriptText) {
        var agent = AgentPrincipalsImpl.createFromThread();
        var request = new TranscriptUpdateRequest();
        request.setPidString(id);
        request.setAgent(agent);
        request.setTranscriptText(transcriptText);

        var transcriptBinary = service.updateTranscript(request);

        Map<String, Object> result = new HashMap<>();
        result.put("action", "updateTranscript");
        result.put("pid", transcriptBinary.getPid().getComponentId());
        result.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    public void setService(TranscriptUpdateService service) {
        this.service = service;
    }
}
