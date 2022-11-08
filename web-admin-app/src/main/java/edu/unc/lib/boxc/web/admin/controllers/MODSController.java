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
package edu.unc.lib.boxc.web.admin.controllers;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
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
import edu.unc.lib.boxc.operations.api.vocab.VocabularyHelper;
import edu.unc.lib.boxc.operations.impl.vocab.VocabularyHelperManager;
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

    @Autowired
    private VocabularyHelperManager vocabularies;

    @PostConstruct
    public void init() {
        namespaces = new HashMap<>();
        namespaces.put(JDOMNamespaceUtil.MODS_V3_NS.getPrefix(), JDOMNamespaceUtil.MODS_V3_NS.getURI());
    }

    /**
     * Forwards user to the MODS editor page with the
     *
     * @param idPrefix
     * @param id
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

        // Structure vocabulary info into a map usable by the editor
        Map<String, Object> vocabConfigs = new HashMap<>();
        // xpath selectors for each vocabulary
        Map<String, String> selectors = new HashMap<>();
        // map of vocabulary names to their term values
        Map<String, Object> perVocabConfigs = new HashMap<>();
        Set<VocabularyHelper> helpers = vocabularies.getHelpers(PIDs.get(pid));
        if (helpers != null) {
            for (VocabularyHelper helper : helpers) {
                Map<String, Object> vocabConfig = new HashMap<>();
                vocabConfig.put("values", helper.getVocabularyTerms());
                perVocabConfigs.put(helper.getVocabularyURI(), vocabConfig);
                if (helper.getSelector() != null) {
                    selectors.put(helper.getSelector(), helper.getVocabularyURI());
                }
            }
        }
        vocabConfigs.put("xpathSelectors", selectors);
        vocabConfigs.put("vocabularies", perVocabConfigs);
        vocabConfigs.put("xpathNamespaces", namespaces);

        results.put("vocabularyConfigs", vocabConfigs);

        return results;
    }
}
