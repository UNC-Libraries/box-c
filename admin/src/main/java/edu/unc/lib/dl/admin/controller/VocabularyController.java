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
package edu.unc.lib.dl.admin.controller;

import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.invalidTerm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.ui.util.SerializationUtil;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.VocabularyHelperManager;
import edu.unc.lib.dl.xml.VocabularyHelper;

/**
 * @author bbpennel
 * @date Sep 5, 2014
 */
@Controller
public class VocabularyController extends AbstractSearchController {

    @Autowired
    private VocabularyHelperManager vocabularies;

    @RequestMapping(value = { "invalidVocab", "invalidVocab/{pid}" }, method = RequestMethod.GET)
    public String invalidVocab() {
        return "report/invalidVocabulary";
    }

    @RequestMapping(value = { "getInvalidVocab", "getInvalidVocab/" }, method = RequestMethod.GET)
    public @ResponseBody
    Map<String, Object> getInvalidVocab(HttpServletRequest request, HttpServletResponse response) {
        SearchRequest searchRequest = generateSearchRequest(request);
        searchRequest.setRootPid(RepositoryPaths.getContentRootPid().toString());

        return getInvalidVocab(searchRequest);
    }

    @RequestMapping(value = "getInvalidVocab/{pid}", method = RequestMethod.GET)
    public @ResponseBody
    Map<String, Object> getInvalidVocab(@PathVariable("pid") String pid, HttpServletRequest request,
            HttpServletResponse response) {
        response.setContentType("application/json");

        SearchRequest searchRequest = generateSearchRequest(request);
        searchRequest.setRootPid(pid);

        return getInvalidVocab(searchRequest);
    }

    public Map<String, Object> getInvalidVocab(SearchRequest searchRequest) {
        AccessGroupSet groups = GroupsThreadStore.getGroups();

        BriefObjectMetadata selectedContainer =
                queryLayer.addSelectedContainer(searchRequest.getRootPid(), searchRequest.getSearchState(), false);

        Map<String, Object> results = new LinkedHashMap<String, Object>();

        Map<String, Object> vocabResults = new HashMap<>();

        Set<VocabularyHelper> helpers = vocabularies.getHelpers(RepositoryPaths.getContentRootPid());
        if (helpers != null) {
            for (VocabularyHelper helper : helpers) {
                String prefix = helper.getInvalidTermPrefix();
                String queryTerm = CDRProperty.invalidTerm.getPredicate() + "|" + prefix;
                SearchResultResponse resultResponse = queryLayer.getRelationSet(searchRequest, queryTerm);

                List<Map<String, Object>> vocabTypeResults = new ArrayList<>();
                String predicate = invalidTerm.getPredicate();
                for (BriefObjectMetadata record : resultResponse.getResultList()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", record.getId());
                    data.put("title", record.getTitle());
                    List<String> invalidTerms = record.getRelation(predicate);
                    List<String> resultTerms = new ArrayList<>();
                    for (String prefixedTerm : invalidTerms) {
                        String parts[] = prefixedTerm.split("\\|", 2);
                        if (parts[0].equals(prefix)) {
                            resultTerms.add(parts[1]);
                        }
                    }
                    data.put("invalidTerms", resultTerms);

                    vocabTypeResults.add(data);
                }

                vocabResults.put(helper.getInvalidTermPrefix(), vocabTypeResults);
            }
        }

        results.put("vocabTypes", vocabResults);
        results.put("container", SerializationUtil.metadataToMap(selectedContainer, groups));

        return results;
    }

}
