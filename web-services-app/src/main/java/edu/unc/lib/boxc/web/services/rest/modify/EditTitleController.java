package edu.unc.lib.boxc.web.services.rest.modify;

import java.util.HashMap;
import java.util.Map;

import edu.unc.lib.boxc.operations.api.exceptions.StateUnmodifiedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.fcrepo.exceptions.AuthorizationException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.edit.EditTitleService;

/**
 * API controller that updates the dc:title of a given object
 *
 * @author lfarrell
 *
 */
@Controller
public class EditTitleController {
    private static final Logger log = LoggerFactory.getLogger(EditTitleController.class);

    @Autowired
    private EditTitleService service;

    @RequestMapping(value = "/edit/title/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<Object> editLabel(@PathVariable("id") String id, @RequestParam("title") String title) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "editTitle");
        result.put("pid", id);

        PID pid = PIDs.get(id);

        try {
            service.editTitle(AgentPrincipalsImpl.createFromThread(), pid, title);
        } catch (StateUnmodifiedException e) {
            log.info("No changes were made to {}", pid.getRepositoryPath());
            result.put("status", "unchanged");
        } catch (Exception e) {
            result.put("error", e.getMessage());
            if (e instanceof AuthorizationException || e instanceof AccessRestrictionException) {
                return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
            } else {
                log.error("Failed to edit title for {}", pid, e);
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
