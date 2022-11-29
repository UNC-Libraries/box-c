package edu.unc.lib.boxc.web.admin.controllers.structure;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.responses.HierarchicalBrowseResultResponse;
import edu.unc.lib.boxc.web.common.controllers.AbstractStructureResultsController;
import edu.unc.lib.boxc.web.common.exceptions.ResourceNotFoundException;
import edu.unc.lib.boxc.web.common.utils.SerializationUtil;

/**
 * Handles requests for JSON representations of structural results.
 *
 * @author bbpennel
 */
@Controller
public class StructureResultsController extends AbstractStructureResultsController {
    @RequestMapping("/structure/json")
    public @ResponseBody
    String getStructureJSON(@RequestParam(value = "files", required = false) String includeFiles, Model model,
            HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("application/json");
        HierarchicalBrowseResultResponse result = getStructureResult(getContentRootPid().getId(),
                "true".equals(includeFiles), false, request);
        return SerializationUtil.structureToJSON(result, getAgentPrincipals().getPrincipals());
    }

    @RequestMapping("/structure/{pid}/json")
    public @ResponseBody
    String getStructureJSON(@PathVariable("pid") String pid,
            @RequestParam(value = "files", required = false) String includeFiles,
            Model model, HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("application/json");
        HierarchicalBrowseResultResponse result = getStructureResult(pid, "true".equals(includeFiles), false,
                request);
        return SerializationUtil.structureToJSON(result, getAgentPrincipals().getPrincipals());
    }

    @RequestMapping("/structure/path")
    public @ResponseBody String getPathStructure(
            @RequestParam(value = "files", required = false) String includeFiles,
            Model model, HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("application/json");
        HierarchicalBrowseResultResponse result = getStructureResult(getContentRootPid().getId(),
                "true".equals(includeFiles), true, request);
        return SerializationUtil.structureToJSON(result, getAgentPrincipals().getPrincipals());
    }

    /**
     * Retrieves the structure path leading up to the specified pid, returning expanded containers starting at the
     * collections object up to the selected container
     */
    @RequestMapping("/structure/{pid}/path")
    public @ResponseBody String getPathStructure(@PathVariable("pid") String pid,
            @RequestParam(value = "files", required = false) String includeFiles,
            Model model, HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("application/json");
        HierarchicalBrowseResultResponse result = getStructureResult(pid, "true".equals(includeFiles), true,
                request);
        return SerializationUtil.structureToJSON(result, getAgentPrincipals().getPrincipals());
    }

    /**
     * Retrieves the structure of the contents of the parent of the specified pid.
     */
    @RequestMapping("/structure/{pid}/parent")
    public @ResponseBody String getParentChildren(@PathVariable("pid") String pid,
            @RequestParam(value = "files", required = false) String includeFiles,
            Model model, HttpServletRequest request, HttpServletResponse response) {

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        // Get the parent pid for the selected object and get its structure view
        ContentObjectRecord selectedContainer = queryLayer.getObjectById(
                new SimpleIdRequest(PIDs.get(pid), tierResultFieldsList, principals));
        if (selectedContainer == null) {
            throw new ResourceNotFoundException("Object " + pid + " was not found.");
        }

        response.setContentType("application/json");
        HierarchicalBrowseResultResponse result = getStructureResult(
                selectedContainer.getAncestorPathFacet().getSearchKey(),
                "true".equals(includeFiles), false, request);

        return SerializationUtil.structureToJSON(result, GroupsThreadStore.getPrincipals());
    }
}