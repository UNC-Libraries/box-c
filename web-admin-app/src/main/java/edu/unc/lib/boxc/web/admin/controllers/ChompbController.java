package edu.unc.lib.boxc.web.admin.controllers;

import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.web.admin.controllers.processing.ChompbPreIngestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * @author lfarrell
 */
@Controller
public class ChompbController {
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
//        var filename = extractFilename(request, jobName);
        var agentPrincipals = AgentPrincipalsImpl.createFromThread();
//        filename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        var nameSegment = Paths.get(filename).getFileName().toString();
        var stream = chompbPreIngestService.getProcessingResults(agentPrincipals, projectName, jobName, filename);
        InputStreamResource resource = new InputStreamResource(stream);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nameSegment)
                .contentType(getMediaType(filename))
                .body(resource);
    }

    private String extractFilename(HttpServletRequest request, String jobName) {
        var preceding = String.format("/processing_results/%s/", jobName);
        var queryPath = request.getRequestURI();
        return queryPath.substring(queryPath.indexOf(preceding) + preceding.length());
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
