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

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.httpclient.HttpClientUtil;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.ui.exception.InvalidRecordRequestException;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.VocabularyHelperManager;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.VocabularyHelper;

/**
 *
 * @author bbpennel
 *
 */
@Controller
public class MODSController extends AbstractSwordController {
    private static final Logger log = LoggerFactory.getLogger(MODSController.class);

    @Autowired
    private String swordUrl;
    @Autowired
    private String swordUsername;
    @Autowired
    private String swordPassword;

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

        AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
        // Retrieve the record for the object being edited
        SimpleIdRequest objectRequest = new SimpleIdRequest(pid, accessGroups);
        BriefObjectMetadataBean resultObject = queryLayer.getObjectById(objectRequest);
        if (resultObject == null) {
            throw new InvalidRecordRequestException();
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

        AccessGroupSet accessGroups = GroupsThreadStore.getGroups();

        // Retrieve the record for the object being edited
        SimpleIdRequest objectRequest = new SimpleIdRequest(pid, accessGroups);
        BriefObjectMetadataBean resultObject = queryLayer.getObjectById(objectRequest);
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
        Set<VocabularyHelper> helpers = vocabularies.getHelpers(new PID(pid));
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

    /**
     * Retrieves the MD_DESCRIPTIVE datastream, containing MODS, for this item if one is present. If it is not present,
     * then returns a blank MODS document.
     *
     * @param idPrefix
     * @param id
     * @return
     */
    @RequestMapping(value = "{pid}/mods", method = RequestMethod.GET,
            produces = {"application/json; text/*; charset=UTF-8"})
    public @ResponseBody
    String getMods(@PathVariable("pid") String pid) {
        String mods = "";
        String dataUrl = swordUrl + "em/" + pid + "/" + ContentModelHelper.Datastream.MD_DESCRIPTIVE;

        CloseableHttpClient client = HttpClientUtil.getAuthenticatedClient(null, swordUsername, swordPassword);
        HttpGet method = new HttpGet(dataUrl);

        // Pass the users groups along with the request
        AccessGroupSet groups = GroupsThreadStore.getGroups();
        method.addHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER, groups.joinAccessGroups(";"));

        try (CloseableHttpResponse httpResp = client.execute(method)) {
            int statusCode = httpResp.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                try {
                    mods = EntityUtils.toString(httpResp.getEntity(), "UTF-8");
                } catch (IOException e) {
                    log.info("Problem uploading MODS for " + pid + ": " + e.getMessage());
                }
            } else {
                if (statusCode == HttpStatus.SC_BAD_REQUEST || statusCode == HttpStatus.SC_NOT_FOUND) {
                    // Ensure that the object actually exists
                    PID existingPID = null;
                    if (existingPID == null) {
                        throw new Exception(
                                "Unable to retrieve MODS.  Object " + pid + " does not exist in the repository.");
                    }
                } else {
                    throw new Exception("Failure to retrieve fedora content due to response of: "
                            + httpResp.getStatusLine() + "\nPath was: " + method.getURI());
                }
            }
        } catch (Exception e) {
            log.error("Error while attempting to stream Fedora content for " + pid, e);
        }
        return mods;
    }

    /**
     * Pushes a MODS document to the target object
     *
     * @param idPrefix
     * @param id
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "describe/{pid}", method = RequestMethod.POST,
            produces = {"application/json; text/*; charset=UTF-8"})
    public @ResponseBody
    String updateDescription(@PathVariable("pid") String pid, Model model,
            HttpServletRequest request, HttpServletResponse response) {
        String datastream = Datastream.MD_DESCRIPTIVE.getName();
        return this.updateDatastream(pid, datastream, request, response);
    }

    public void setSwordUrl(String swordUrl) {
        this.swordUrl = swordUrl;
    }

    public void setSwordUsername(String swordUsername) {
        this.swordUsername = swordUsername;
    }

    public void setSwordPassword(String swordPassword) {
        this.swordPassword = swordPassword;
    }
}
