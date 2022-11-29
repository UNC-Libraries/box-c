package edu.unc.lib.boxc.web.services.rest.modify;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashMap;
import java.util.Map;

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

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsService;

/**
 * API controller for destroying repository objects and replacing them with tombstones
 *
 * @author harring
 *
 */
@Controller
public class DestroyObjectsController {
    private static final Logger log = LoggerFactory.getLogger(DestroyObjectsController.class);

    @Autowired
    private DestroyObjectsService service;

    @PostMapping(value = "/edit/destroy")
    @ResponseBody
    public ResponseEntity<Object> destroyBatch(@RequestParam("ids") String ids) {
        if (isEmpty(ids)) {
            throw new IllegalArgumentException("Must provide one or more ids");
        }

        return destroy(ids.split("\n"));
    }

    @PostMapping(value = "/edit/destroy/{id}")
    @ResponseBody
    public ResponseEntity<Object> destroyObject(@PathVariable("id") String id) {
        return destroy(id);
    }

    private ResponseEntity<Object> destroy(String... ids) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "destroy");
        if (ids.length == 1) {
            result.put("pid", ids[0]);
        } else {
            result.put("pids", ids);
        }

        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        String jobId = service.destroyObjects(agent, ids);
        result.put("job", jobId);
        log.info("{} initiated destruction of {} objects from the repository", agent.getUsername(), ids.length);

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
