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

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;

/**
 * Populates a list of department values and forwards to the dept browse.
 * @author bbpennel
 */
@Controller
@RequestMapping("/browse/dept")
public class BrowseDepartmentsController extends AbstractSolrSearchController {

    @RequestMapping(value = "/{pid}", method = RequestMethod.GET)
    public String handleRequest(@PathVariable("pid") String pid, Model model) {
        SearchResultResponse result;

        if (pid != null) {
            result = queryLayer.getDepartmentList(GroupsThreadStore.getGroups(), pid);
        } else {
            result = queryLayer.getDepartmentList(GroupsThreadStore.getGroups(), null);
        }

        if (result != null) {
            model.addAttribute("departmentFacets", result.getFacetFields().get(0));
            model.addAttribute("container", result.getSelectedContainer());
        }

        model.addAttribute("resultType", "departmentBrowse");
        model.addAttribute("menuId", "browse");
        return "browseDepartments";
    }

    @RequestMapping(method = RequestMethod.GET)
    public String handleRequest(Model model) {
        return handleRequest(null, model);
    }
}
