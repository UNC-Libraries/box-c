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
package edu.unc.lib.dl.cdr.services.rest;

import static edu.unc.lib.dl.acl.util.GroupsThreadStore.getAgentPrincipals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.service.ChildrenCountService;
import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;
import edu.unc.lib.dl.ui.util.SerializationUtil;

/**
 *
 * @author bbpennel
 *
 */
@Controller
public class SearchRestController extends AbstractSolrSearchController {

    private static Pattern jsonpCleanupPattern = Pattern.compile("[^a-zA-Z0-9_$]+");

    @Autowired
    private ChildrenCountService childrenCountService;

    @RequestMapping(value = "/search")
    public @ResponseBody String search(HttpServletRequest request, HttpServletResponse response) {
        return doSearch(null, request, false);
    }

    @RequestMapping(value = "/search/{id}")
    public @ResponseBody String search(@PathVariable("id") String id, HttpServletRequest request) {
        return doSearch(id, request, false);
    }

    @RequestMapping(value = "/list")
    public @ResponseBody String list(HttpServletRequest request, HttpServletResponse response) {
        return doSearch(null, request, true);
    }

    @RequestMapping(value = "/list/{id}")
    public @ResponseBody String list(@PathVariable("id") String id, HttpServletRequest request) {
        return doSearch(id, request, true);
    }

    private List<String> getResultFields(HttpServletRequest request) {
        String fields = request.getParameter("fields");
        // Allow for retrieving of specific fields
        if (fields != null) {
            String[] fieldNames = fields.split(",");
            List<String> resultFields = new ArrayList<>();
            for (String fieldName: fieldNames) {
                String fieldKey = searchSettings.searchFieldKey(fieldName);
                if (fieldKey != null) {
                    resultFields.add(fieldKey);
                }
            }
            return resultFields;
        } else {
            // Retrieve a predefined set of fields
            String fieldSet = request.getParameter("fieldSet");
            List<String> resultFields = searchSettings.resultFields.get(fieldSet);
            if (resultFields == null) {
                resultFields = new ArrayList<>(searchSettings.resultFields.get("brief"));
            }
            return resultFields;
        }
    }

    private String doSearch(String pid, HttpServletRequest request, boolean applyCutoffs) {
        SearchRequest searchRequest = generateSearchRequest(request);
        searchRequest.setApplyCutoffs(applyCutoffs);
        searchRequest.setRootPid(pid);

        SearchState searchState = searchRequest.getSearchState();

        List<String> resultFields = this.getResultFields(request);
        searchState.setResultFields(resultFields);

        // Rollup
        String rollup = request.getParameter("rollup");
        searchState.setRollup(rollup != null && !"false".equalsIgnoreCase(rollup));
        if (searchState.getRollup() && !"true".equalsIgnoreCase(rollup)) {
            String fieldKey = searchSettings.searchFieldKey(rollup);
            if (fieldKey != null) {
                searchState.setRollupField(fieldKey);
            } else {
                searchState.setRollup(false);
            }
        }

        SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);
        if (resultResponse == null) {
            return null;
        }

        childrenCountService.addChildrenCounts(resultResponse.getResultList(),
                searchRequest.getAccessGroups());

        Map<String, Object> response = new HashMap<>();
        response.put("numFound", resultResponse.getResultCount());
        List<Map<String, Object>> results = new ArrayList<>(resultResponse.getResultList().size());
        for (BriefObjectMetadata metadata: resultResponse.getResultList()) {
            results.add(SerializationUtil.metadataToMap(metadata, GroupsThreadStore.getGroups()));
        }
        response.put("results", results);

        String callback = request.getParameter("callback");
        // If there's a jsonp callback, do some overzealous cleanup to remove any bad stuff
        if (callback != null) {
            callback = jsonpCleanupPattern.matcher(callback).replaceAll("");
            return callback + "(" + SerializationUtil.objectToJSON(response) + ")";
        }

        return SerializationUtil.objectToJSON(response);
    }

    @RequestMapping(value = "/record/{id}")
    public @ResponseBody String getSingleItem(@PathVariable("id") String id,
            HttpServletRequest request, HttpServletResponse response) {
        List<String> resultFields = this.getResultFields(request);

        SimpleIdRequest idRequest = new SimpleIdRequest(id, resultFields, getAgentPrincipals().getPrincipals());
        BriefObjectMetadataBean briefObject = queryLayer.getObjectById(idRequest);
        if (briefObject == null) {
            response.setStatus(404);
            return null;
        }

        String callback = request.getParameter("callback");
        // If there's a jsonp callback, do some overzealous cleanup to remove any bad stuff
        if (callback != null) {
            callback = jsonpCleanupPattern.matcher(callback).replaceAll("");
            return callback + "(" + SerializationUtil.metadataToJSON(briefObject, GroupsThreadStore.getGroups()) + ")";
        }

        return SerializationUtil.metadataToJSON(briefObject, GroupsThreadStore.getGroups());
    }
}
