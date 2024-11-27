package edu.unc.lib.boxc.web.admin.controllers;

import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.web.admin.controllers.processing.ChompbPreIngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * @author lfarrell
 */
@Controller
public class ChompbController {
    private static final Logger log = LoggerFactory.getLogger(ChompbController.class);
    @Autowired
    ChompbPreIngestService chompbPreIngestService;

    @RequestMapping(value = "chompb", method = RequestMethod.GET)
    public String chompb() {
        return "report/chompb";
    }

    @RequestMapping(value = "chompb/listProjects", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String chompbProjectsJSON() {
        var agentPrincipals = AgentPrincipalsImpl.createFromThread();
        return chompbPreIngestService.getProjectLists(agentPrincipals);
    }

    /**
     * Until the admin is a full SPA we'll need to set routes here and in the vue-admin-apps router
     * for each chompb action
     * @return
     */
    @RequestMapping(value = "chompb/project/**", method = RequestMethod.GET)
    public String chompbCroppingReport() {
        return "report/chompb";
    }

    /**
     * Get processing result files for a specific job
     * @param projectName
     * @param jobName
     * @param filePath
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "chompb/project/{projectName}/processing_results/{jobName}/files",
            method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> getProcessingResults(@PathVariable("projectName") String projectName,
                                                @PathVariable("jobName") String jobName,
                                                @RequestParam(value = "path", defaultValue = "false") String filename)
                                                throws IOException {
        var agentPrincipals = AgentPrincipalsImpl.createFromThread();
        var nameSegment = Paths.get(filename).getFileName().toString();
        var stream = chompbPreIngestService.getProcessingResults(agentPrincipals, projectName, jobName, filename);
        InputStreamResource resource = new InputStreamResource(stream);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nameSegment)
                .contentType(getMediaType(filename))
                .body(resource);
    }

    @RequestMapping(value = "chompb/project/{projectName}/action/velocicroptor", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Object> startCropping(@PathVariable("projectName") String projectName) {
        String userEmail = GroupsThreadStore.getEmail();
        Map<String, Object> result = new HashMap<>();
        result.put("action", "Start cropping for project " + projectName);
        try {
            var agentPrincipals = AgentPrincipalsImpl.createFromThread();
            chompbPreIngestService.startCropping(agentPrincipals, projectName, userEmail);
        } catch (Exception e){
            log.error("Failed to start cropping for project {}", projectName, e);
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private MediaType getMediaType(String filename) {
        if (filename.endsWith(".json")) {
            return MediaType.APPLICATION_JSON;
        } else if (filename.endsWith(".csv")) {
            return MediaType.TEXT_PLAIN;
        } else {
            return MediaType.IMAGE_JPEG;
        }
    }
}
