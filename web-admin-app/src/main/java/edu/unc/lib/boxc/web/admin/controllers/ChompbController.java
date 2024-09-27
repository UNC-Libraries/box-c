package edu.unc.lib.boxc.web.admin.controllers;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.web.admin.controllers.processing.ChompbPreIngestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

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

    @RequestMapping(value = "chompb/project/{projectName}/processing_results/{jobName}/data.json",
            method = RequestMethod.GET,
            produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String getProcessingResults(@PathVariable("projectName") String projectName,
                                                     @PathVariable("jobName") String jobName) {
        var agentPrincipals = AgentPrincipalsImpl.createFromThread();
        try {
            return chompbPreIngestService.getProcessingResults(agentPrincipals, projectName, jobName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
