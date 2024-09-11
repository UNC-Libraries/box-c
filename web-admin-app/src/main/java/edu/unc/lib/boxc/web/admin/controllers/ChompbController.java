package edu.unc.lib.boxc.web.admin.controllers;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.web.admin.controllers.processing.ChompbPreIngestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    @RequestMapping(value = "chompb/project", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String chompbProjectsJSON() {
        AccessGroupSet accessGroups = GroupsThreadStore.getPrincipals();
        return chompbPreIngestService.getProjectLists(accessGroups);
    }
}
