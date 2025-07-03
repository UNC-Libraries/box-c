package edu.unc.lib.boxc.web.admin.controllers;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.web.common.controllers.AbstractSolrSearchController;
import edu.unc.lib.boxc.web.common.exceptions.InvalidRecordRequestException;

/**
 *
 * @author bbpennel
 *
 */
@Controller
public class MODSController extends AbstractSolrSearchController {
    private Map<String, String> namespaces;

    @PostConstruct
    public void init() {
        namespaces = new HashMap<>();
        namespaces.put(JDOMNamespaceUtil.MODS_V3_NS.getPrefix(), JDOMNamespaceUtil.MODS_V3_NS.getURI());
    }

    /**
     * Forwards user to the MODS editor page with the
     *
     * @param pid
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "describe/{pid}", method = RequestMethod.GET)
    public String editDescription(@PathVariable("pid") String pid, Model model,
            HttpServletRequest request) {

        AccessGroupSet accessGroups = GroupsThreadStore.getPrincipals();
        // Retrieve the record for the object being edited
        SimpleIdRequest objectRequest = new SimpleIdRequest(PIDs.get(pid), accessGroups);
        ContentObjectRecord resultObject = queryLayer.getObjectById(objectRequest);
        if (resultObject == null) {
            throw new NotFoundException("No record found for " + pid);
        }

        model.addAttribute("resultObject", resultObject);
        return "edit/description";
    }

    @RequestMapping(value = "describeInfo/{pid}", method = RequestMethod.GET,
            produces = {"application/json; text/*; charset=UTF-8"})
    public @ResponseBody
    Map<String, Object> editDescription(@PathVariable("pid") String pid, HttpServletResponse response) {
        response.setContentType("application/json");

        Map<String, Object> results = new LinkedHashMap<>();

        AccessGroupSet accessGroups = GroupsThreadStore.getPrincipals();

        // Retrieve the record for the object being edited
        SimpleIdRequest objectRequest = new SimpleIdRequest(PIDs.get(pid), accessGroups);
        ContentObjectRecord resultObject = queryLayer.getObjectById(objectRequest);
        if (resultObject == null) {
            throw new InvalidRecordRequestException();
        }

        results.put("resultObject", resultObject);

        return results;
    }
}
