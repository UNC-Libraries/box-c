package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.fcrepo.exceptions.AuthorizationException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.operations.api.exceptions.StateUnmodifiedException;
import edu.unc.lib.boxc.operations.impl.mimetype.CorrectMimetypesService;
import edu.unc.lib.boxc.operations.impl.transcript.TranscriptUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API endpoint for updating mimetypes for file objects
 *
 * @author krwong
 */
@Controller
public class CorrectMimetypesController {
    private static final Logger log = LoggerFactory.getLogger(CorrectMimetypesController.class);

    @Autowired
    private CorrectMimetypesService correctMimetypesService;

    @RequestMapping(value = "/edit/correctMimetypes")
    @ResponseBody
    public ResponseEntity<Object> correctMimetype(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "correct mimetypes");

        if (file == null || file.isEmpty()) {
            result.put("error", "CSV file is required");
            result.put("timestamp", System.currentTimeMillis());
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }

        List<PID> pids = new ArrayList<>();
        try (InputStream correctMimetypeStream = file.getInputStream()) {
            pids = correctMimetypesService.correctMimetypes(correctMimetypeStream, AgentPrincipalsImpl.createFromThread());
            result.put("status", "Corrected mimetypes for " + pids);
        } catch (StateUnmodifiedException e) {
            log.info("No changes were made");
            result.put("status", "unchanged");
        } catch (Exception e) {
            result.put("error", e.getMessage());
            Throwable t = e.getCause();
            if (t instanceof AuthorizationException || t instanceof AccessRestrictionException) {
                return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
            } else {
                log.error("Failed to correct mimetypes for {}", pids, e);
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    public void setService(CorrectMimetypesService correctMimetypesService) {
        this.correctMimetypesService = correctMimetypesService;
    }
}
