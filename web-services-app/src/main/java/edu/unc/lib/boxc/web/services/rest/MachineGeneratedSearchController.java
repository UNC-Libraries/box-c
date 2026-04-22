package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService;
import edu.unc.lib.boxc.web.common.controllers.AbstractSolrSearchController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;

@Controller
public class MachineGeneratedSearchController extends AbstractSolrSearchController {
    public static final List<String> MG_RESULT_FIELDS = Arrays.asList(SearchFieldKey.ID.name(),
            SearchFieldKey.TITLE.name(), SearchFieldKey.ALT_TEXT.name(), SearchFieldKey.MG_CONTENT_TAGS.name(),
            SearchFieldKey.MG_DESCRIPTION.name(), SearchFieldKey.FULL_DESCRIPTION.name(),
            SearchFieldKey.TRANSCRIPT.name(), SearchFieldKey.MG_RISK_SCORE.name());
    @Autowired
    private AccessControlService accessControlService;
    @Autowired
    private MachineGeneratedContentService machineGeneratedContentService;

    @RequestMapping(value = "/machineGeneratedSearch/{parentId}")
    public @ResponseBody Map<String, Object> search(@PathVariable("parentId") String pidString, HttpServletRequest request) {
        var pid = PIDs.get(pidString);
        accessControlService.assertHasAccess("Insufficient permissions to access machine generated metadata for object " + pid,
                pid, getAgentPrincipals().getPrincipals(), Permission.viewHidden);

        SearchRequest searchRequest = generateSearchRequest(request);
        SearchState searchState = searchRequest.getSearchState();
        searchState.setResultFields(MG_RESULT_FIELDS);

        SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);
        if (resultResponse == null) {
            return null;
        }
        Map<String, Object> response = new HashMap<>();

        List<Map<String, Object>> results = new ArrayList<>(resultResponse.getResultList().size());
        for (ContentObjectRecord metadata : resultResponse.getResultList()) {

            Map<String, Object> data = new HashMap<>();
            data.put("id", metadata.getId());
            data.put("title", metadata.getTitle());
            data.put("altText", metadata.getAltText());
            data.put("mgContentTags", metadata.getMgContentTags());

            var mgDescJson = machineGeneratedContentService.deserializeMachineGeneratedDescription(
                    metadata.getMgDescription());
            data.put("mgDescription", mgDescJson);
            data.put("mgAltText", machineGeneratedContentService.extractAltText(mgDescJson));
            data.put("mgTranscript", machineGeneratedContentService.extractTranscript(mgDescJson));
            data.put("mgFullDescription", machineGeneratedContentService.extractFullDescription(mgDescJson));
            data.put("mgReviewAssessment", machineGeneratedContentService.extractReviewAssessment(mgDescJson));
            data.put("mgSafetyAssessment", machineGeneratedContentService.extractSafetyAssessment(mgDescJson));
            data.put("mgRiskScore", machineGeneratedContentService.extractRiskScore(mgDescJson));

            results.add(data);
        }
        response.put("results", results);
        return response;
    }


    public void setMachineGeneratedContentService(MachineGeneratedContentService machineGeneratedContentService) {
        this.machineGeneratedContentService = machineGeneratedContentService;
    }

    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }
}
