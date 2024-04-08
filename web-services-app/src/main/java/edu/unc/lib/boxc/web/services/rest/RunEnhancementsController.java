package edu.unc.lib.boxc.web.services.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.fcrepo.exceptions.AuthorizationException;
import edu.unc.lib.boxc.web.services.processing.RunEnhancementsService;

/**
 * Receives list of PIDs to run enhancements on
 * and runs service to create messages to kick off enhancement processing
 *
 * @author lfarrell
 */
@Controller
public class RunEnhancementsController {
    private static final Logger log = LoggerFactory.getLogger(RunEnhancementsController.class);

    @Autowired
    private RunEnhancementsService enhService;

    @PostMapping(value = "runEnhancements", produces = APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<Object> runEnhancements(@RequestBody RunEnhancementsRequest data) {
        Map<String, Object> result = new HashMap<>();

        try {
            enhService.run(GroupsThreadStore.getAgentPrincipals(), data.getPids(), data.isForce());
            result.put("message", "Enhancement of " + data.getPids().size()
                    + " object(s) and their children has begun");
            result.put("action", "runEnhancements");
        } catch (Exception e) {
            result.put("message", "Unable to run enhancements of " + data.getPids().size()
                    + " object(s) and their children");
            result.put("error", e.getMessage());

            log.error("Failed to run enhancements for {} {}", data.getPids().toString(), e.getMessage());

            if (e instanceof AuthorizationException || e instanceof AccessRestrictionException) {
                return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
            } else {
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    public static class RunEnhancementsRequest {
        private List<String> pids;
        private boolean force;

        public List<String> getPids() {
            return pids;
        }

        public boolean isForce() {
            return force;
        }
    }

    public void setEnhancementService(RunEnhancementsService enhService) {
        this.enhService = enhService;
    }
}
