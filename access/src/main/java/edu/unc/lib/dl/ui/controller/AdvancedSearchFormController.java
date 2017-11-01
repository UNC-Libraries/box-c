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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;

/**
 * Handles requests to the advanced search page, sending users to the form if there are no
 * query string parameters set, or constructing a search state and sending the user to
 * get results if they have populated the form.
 * @author bbpennel
 */
@Controller
@RequestMapping("/advancedSearch")
public class AdvancedSearchFormController extends AbstractSolrSearchController {
    LinkedHashMap<String, String> dropdownVals = new LinkedHashMap<String, String>();

    @PostConstruct
    public void init() throws IOException, JDOMException {
        SAXBuilder builder = new SAXBuilder();
        Document document = null;

        try (InputStream responseStream = this.getClass().getResourceAsStream("/mappings/dropdownMappings.xml")) {
            document = builder.build(responseStream);
            Element rootNode = document.getRootElement();
            Element element = rootNode.getChild("mapping");
            List<Element> list = element.getChildren("pair");

            for (int i = 0; i < list.size(); i++) {
                Element node = list.get(i);
                String dropdownKey = node.getAttribute("key").getValue();
                String dropdownVal = node.getTextTrim();

                dropdownVals.put(dropdownKey, dropdownVal);
            }
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public String handleRequest(Model model, HttpServletRequest request) {
        //If the user is coming to this servlet without any parameters set then send them to form.
        if (request.getQueryString() == null || request.getQueryString().length() == 0) {
            //Populate the list of collections for the advanced search page drop down
            AccessGroupSet accessGroups = GroupsThreadStore.getGroups();

            SearchResultResponse collectionResultResponse = queryLayer.getCollectionList(accessGroups);
            model.addAttribute("collectionList", collectionResultResponse.getResultList());

            SearchResultResponse departmentFieldObject = queryLayer.getDepartmentList(accessGroups, null);
            model.addAttribute("departmentList", departmentFieldObject.getFacetFields().get(0));

            model.addAttribute("pageSubtitle", "Advanced Search");

            model.addAttribute("formatMap", dropdownVals);
            return "advancedSearch";
        }

        // If the user has submitted the search form, then generate a search state
        // and forward them to the search servlet.
        SearchState searchState = searchStateFactory.createSearchStateAdvancedSearch(request.getParameterMap());
        String container = request.getParameter("container");

        request.getSession().setAttribute("searchState", searchState);

        model.addAllAttributes(SearchStateUtil.generateStateParameters(searchState));

        return "redirect:/search" + ((container != null && container.length() > 0) ? '/' + container : "");
    }
}
