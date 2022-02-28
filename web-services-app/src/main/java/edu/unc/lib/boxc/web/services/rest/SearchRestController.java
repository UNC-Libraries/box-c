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
package edu.unc.lib.boxc.web.services.rest;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.web.common.services.AccessCopiesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.ChildrenCountService;
import edu.unc.lib.boxc.web.common.controllers.AbstractSolrSearchController;
import edu.unc.lib.boxc.web.common.utils.SerializationUtil;

/**
 *
 * @author bbpennel
 *
 */
@Controller
public class SearchRestController extends AbstractSolrSearchController {

    private static Pattern jsonpCleanupPattern = Pattern.compile("[^a-zA-Z0-9_$]+");

    public static final List<String> RESULT_FIELDS_ID = Arrays.asList(SearchFieldKey.ID.name());
    public static final List<String> RESULT_FIELDS_IDENTIFIER = Arrays.asList(SearchFieldKey.ID.name(),
            SearchFieldKey.IDENTIFIER.name());
    public static final List<String> RESULT_FIELDS_BRIEF = Arrays.asList(SearchFieldKey.ID.name(),
            SearchFieldKey.TITLE.name(), SearchFieldKey.CREATOR.name(), SearchFieldKey.ABSTRACT.name(),
            SearchFieldKey.RESOURCE_TYPE.name(), SearchFieldKey.PARENT_COLLECTION.name());
    public static final List<String> RESULT_FIELDS_FULL = Arrays.asList(SearchFieldKey.ID.name(),
            SearchFieldKey.TITLE.name(), SearchFieldKey.CREATOR.name(), SearchFieldKey.ABSTRACT.name(),
            SearchFieldKey.RESOURCE_TYPE.name(), SearchFieldKey.PARENT_COLLECTION.name(),
            SearchFieldKey.DATASTREAM.name(), SearchFieldKey.DATE_ADDED.name(), SearchFieldKey.DATE_CREATED.name(),
            SearchFieldKey.DATE_UPDATED.name(), SearchFieldKey.IDENTIFIER.name(), SearchFieldKey.LANGUAGE.name(),
            SearchFieldKey.SUBJECT.name(), SearchFieldKey.FILESIZE.name());
    public static final Map<String, List<String>> RESULT_FIELD_SETS = Map.of("id", RESULT_FIELDS_ID,
            "identifier", RESULT_FIELDS_IDENTIFIER,
            "brief", RESULT_FIELDS_BRIEF,
            "full", RESULT_FIELDS_FULL,
            "structure", SearchSettings.RESULT_FIELDS_STRUCTURE);

    @Autowired
    private ChildrenCountService childrenCountService;
    @Autowired
    private AccessCopiesService accessCopiesService;

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
        String fields = request.getParameter(SearchSettings.URL_PARAM_FIELDS);
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
            String fieldSet = request.getParameter(SearchSettings.URL_PARAM_FIELDSET);
            if (fieldSet == null) {
                fieldSet = "brief";
            }
            List<String> resultFields = RESULT_FIELD_SETS.get(fieldSet);
            if (resultFields == null) {
                resultFields = new ArrayList<>(RESULT_FIELDS_BRIEF);
            }
            return resultFields;
        }
    }

    private String doSearch(String pid, HttpServletRequest request, boolean applyCutoffs) {
        SearchRequest searchRequest = generateSearchRequest(request);
        searchRequest.setApplyCutoffs(applyCutoffs);
        searchRequest.setRootPid(PIDs.get(pid));

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

        AccessGroupSet principals = GroupsThreadStore.getPrincipals();
        Map<String, Object> response = new HashMap<>();
        response.put("numFound", resultResponse.getResultCount());
        List<Map<String, Object>> results = new ArrayList<>(resultResponse.getResultList().size());
        accessCopiesService.populateThumbnailIds(resultResponse.getResultList(), principals, false);
        for (ContentObjectRecord metadata: resultResponse.getResultList()) {
            results.add(SerializationUtil.metadataToMap(metadata, principals));
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

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        SimpleIdRequest idRequest = new SimpleIdRequest(PIDs.get(id), resultFields, principals);
        ContentObjectRecord briefObject = queryLayer.getObjectById(idRequest);
        if (briefObject == null) {
            response.setStatus(404);
            return null;
        }

        String callback = request.getParameter("callback");
        // If there's a jsonp callback, do some overzealous cleanup to remove any bad stuff
        if (callback != null) {
            callback = jsonpCleanupPattern.matcher(callback).replaceAll("");
            return callback + "(" + SerializationUtil.metadataToJSON(briefObject, principals) + ")";
        }

        return SerializationUtil.metadataToJSON(briefObject, principals);
    }
}
