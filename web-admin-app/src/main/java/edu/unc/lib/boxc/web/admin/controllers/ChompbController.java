package edu.unc.lib.boxc.web.admin.controllers;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author lfarrell
 */
@Controller
public class ChompbController {
    @RequestMapping(value = "chompb", method = RequestMethod.GET)
    public String chompb() {
        return "report/chompb";
    }

    @RequestMapping(value = "chompb/project", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody
    Map<String, Object> chompbProjectsJSON(HttpServletRequest request, HttpServletResponse response) {
        AccessGroupSet accessGroups = GroupsThreadStore.getPrincipals();

        Map<String, Object> results = new LinkedHashMap<>();

        return results;
    }
}
