package edu.unc.lib.boxc.web.services.rest.modify;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.fcrepo.exceptions.AuthorizationException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.web.services.processing.SetAsPrimaryObjectService;

/**
 * API controller for setting the primary object on a work
 *
 * @author harring
 *
 */
@Controller
public class SetAsPrimaryObjectController {

    private static final Logger log = LoggerFactory.getLogger(SetAsPrimaryObjectController.class);

    @Autowired
    private SetAsPrimaryObjectService setAsPrimaryObjectService;

    @RequestMapping(value = "/edit/setAsPrimaryObject/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<Object> setAsPrimaryObject(@PathVariable("id") String id) {
        return setAsPrimary(id);
    }

    private ResponseEntity<Object> setAsPrimary(String id) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "setAsPrimaryObject");
        result.put("pid", id);

        PID fileObjPid = PIDs.get(id);

        try {
            setAsPrimaryObjectService.setAsPrimaryObject(AgentPrincipalsImpl.createFromThread(), fileObjPid);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            if (e instanceof AuthorizationException || e instanceof AccessRestrictionException) {
                return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
            } else {
                log.error("Failed to set primary object with pid " + fileObjPid, e);
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/edit/clearPrimaryObject/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<Object> clearPrimaryObject(@PathVariable("id") String id) {
        return clearPrimary(id);
    }

    private ResponseEntity<Object> clearPrimary(String id) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "clearPrimaryObject");
        result.put("pid", id);

        PID objPid = PIDs.get(id);

        try {
            setAsPrimaryObjectService.clearPrimaryObject(AgentPrincipalsImpl.createFromThread(), objPid);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            if (e instanceof AuthorizationException || e instanceof AccessRestrictionException) {
                return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
            } else {
                log.error("Failed to clear primary object on or with pid " + objPid, e);
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
