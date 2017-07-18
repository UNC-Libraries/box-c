/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.ui.controller;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;
import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;
import edu.unc.lib.dl.ui.util.SerializationUtil;

/**
 * Handles requests for the heirarchical structure browse view. The request may either be for an entire stand alone
 * view, or if the ajax option is true then a portion of the tree starting from the room node. The request can specify
 * the max depth in terms of nodes in the tree it will return, where the max depth is limited by the application wide
 * structured depth property.
 * 
 * @author bbpennel
 */
@Controller
public class StructureBrowseController extends AbstractStructureResultsController {
    protected List<String> tierResultFieldsList = Arrays.asList(SearchFieldKeys.ID.name(),
            SearchFieldKeys.RESOURCE_TYPE.name(), SearchFieldKeys.ANCESTOR_PATH.name(),
            SearchFieldKeys.CONTENT_MODEL.name(), SearchFieldKeys.ROLE_GROUP.name());

    /**
     * Retrieves the contents of the pid specified in a structural view
     */
    @RequestMapping("/structure")
    public String getStructure(@RequestParam(value = "files", required = false) String includeFiles,
            @RequestParam(value = "view", required = false) String view, Model model, HttpServletRequest request) {
        if (includeFiles == null) {
            includeFiles = "true";
        }
        return getStructureTree(null, "true".equals(includeFiles), view, false, model, request);
    }

    @RequestMapping("/structure/{pid}")
    public String getStructureJSON(@PathVariable("pid") String pid,
            @RequestParam(value = "files", required = false) String includeFiles,
            @RequestParam(value = "view", required = false) String view, Model model, HttpServletRequest request,
            HttpServletResponse response) {
        if (includeFiles == null) {
            includeFiles = "true";
        }
        return getStructureTree(pid, "true".equals(includeFiles), view, false, model, request);
    }

    /**
     * Retrieves the direct children of the pid specified. If no pid is specified, then the root is used
     */
    @RequestMapping("/structure/{pid}/tier")
    public String getSingleTier(@PathVariable("pid") String pid,
            @RequestParam(value = "files", required = false) String includeFiles,
            Model model, HttpServletRequest request, HttpServletResponse response) {
        BriefObjectMetadataBean selectedContainer =
                queryLayer.getObjectById(new SimpleIdRequest(pid, tierResultFieldsList));
        if (selectedContainer == null) {
            throw new ResourceNotFoundException("Object " + pid + " was not found.");
        }

        SearchRequest browseRequest = new SearchRequest();
        generateSearchRequest(request, null, browseRequest);
        SearchState searchState = browseRequest.getSearchState();
        searchState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), selectedContainer.getPath());
        if ("only".equals(includeFiles)) {
            searchState.setResourceTypes(Arrays.asList(searchSettings.resourceTypeFile));
        } else if ("true".equals(includeFiles)) {
            searchState.setResourceTypes(null);
        } else {
            searchState.setResourceTypes(Arrays.asList("!" + searchSettings.resourceTypeFile));
        }

        HierarchicalBrowseResultResponse resultResponse = queryLayer.getStructureTier(browseRequest);
        model.addAttribute("structureResults", resultResponse);

        String searchStateUrl = SearchStateUtil.generateStateParameterString(browseRequest.getSearchState());
        model.addAttribute("searchStateUrl", searchStateUrl);

        model.addAttribute("template", "ajax");
        return "/jsp/structure/structureTree";
    }

    private String getStructureTree(String pid, boolean includeFiles, String viewParam, boolean collectionMode,
            Model model, HttpServletRequest request) {
        HierarchicalBrowseResultResponse resultResponse = getStructureResult(pid, includeFiles, collectionMode,
                true, request);

        SearchState searchState = resultResponse.getSearchState();
        String searchParams = SearchStateUtil.generateSearchParameterString(searchState);
        model.addAttribute("searchParams", searchParams);

        model.addAttribute("resultType", "structure");
        model.addAttribute("pageSubtitle", "Browse Results");

        model.addAttribute("searchStateUrl",
                SearchStateUtil.generateStateParameterString(resultResponse.getSearchState()));
        model.addAttribute("searchQueryUrl",
                SearchStateUtil.generateSearchParameterString(resultResponse.getSearchState()));
        model.addAttribute("selectedContainer", resultResponse.getSelectedContainer());
        model.addAttribute("resultResponse", resultResponse);
        model.addAttribute("structureResults", resultResponse);

        model.addAttribute("resultJSON",
                SerializationUtil.structureToJSON(resultResponse, GroupsThreadStore.getGroups()));

        return "structureBrowse";
    }
}
