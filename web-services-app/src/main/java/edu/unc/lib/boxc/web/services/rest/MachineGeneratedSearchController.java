package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.facets.CutoffFacetImpl;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService;
import edu.unc.lib.boxc.web.common.controllers.AbstractSolrSearchController;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

/**
 * Controller which takes a parent ID and returns alt text, full description, machine generated description,
 * transcript, review assessment, and safety assessment info for child file objects
 */
@Controller
public class MachineGeneratedSearchController extends AbstractSolrSearchController {
    private static final Logger log = LoggerFactory.getLogger(MachineGeneratedSearchController.class);
    public static final List<String> MG_RESULT_FIELDS = Arrays.asList(SearchFieldKey.ID.name(),
            SearchFieldKey.TITLE.name(), SearchFieldKey.ALT_TEXT.name(), SearchFieldKey.MG_CONTENT_TAGS.name(),
            SearchFieldKey.MG_DESCRIPTION.name(), SearchFieldKey.FULL_DESCRIPTION.name(),
            SearchFieldKey.TRANSCRIPT.name(), SearchFieldKey.MG_RISK_SCORE.name());
    public static final String NO_RESULTS = "No search results returned";
    @Autowired
    private AccessControlService accessControlService;
    @Autowired
    private MachineGeneratedContentService machineGeneratedContentService;

    @RequestMapping(value = "/machineGeneratedSearch/{parentId}")
    public @ResponseBody ResponseEntity<Object> search(@PathVariable("parentId") String pidString, HttpServletRequest request) {
        var pid = PIDs.get(pidString);
        accessControlService.assertHasAccess("Insufficient permissions to access machine generated metadata for object " + pid,
                pid, getAgentPrincipals().getPrincipals(), Permission.viewHidden);

        SearchRequest searchRequest = generateSearchRequest(request);
        searchRequest.setRootPid(pid); // Only children of this object will be returned
        searchRequest.setApplyCutoffs(false); // Filters to all children, not just immediate

        SearchState searchState = searchRequest.getSearchState();
        // filter to FileObjects
        searchState.setFacet(new GenericFacet(SearchFieldKey.RESOURCE_TYPE.name(), ResourceType.File.name()));
        searchState.setResultFields(MG_RESULT_FIELDS);
        Map<String, Object> response = new HashMap<>();

        SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);
        if (resultResponse == null) {
            response.put("results", List.of());
            response.put("errorMessage", NO_RESULTS);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        List<Map<String, Object>> results = new ArrayList<>(resultResponse.getResultList().size());
        for (ContentObjectRecord metadata : resultResponse.getResultList()) {
            Map<String, Object> data = new HashMap<>();
            data.put(SearchFieldKey.ID.getUrlParam(), metadata.getId());
            data.put(SearchFieldKey.TITLE.getUrlParam() , metadata.getTitle());
            data.put(SearchFieldKey.ALT_TEXT.getUrlParam() , metadata.getAltText());
            data.put(SearchFieldKey.MG_CONTENT_TAGS.getUrlParam() , metadata.getMgContentTags());

            var mgDescJson = machineGeneratedContentService.deserializeMachineGeneratedDescription(
                    metadata.getMgDescription());
            data.put(SearchFieldKey.MG_DESCRIPTION.getUrlParam() , mgDescJson);
            data.put("mgAltText", machineGeneratedContentService.extractAltText(mgDescJson));
            data.put("mgTranscript", machineGeneratedContentService.extractTranscript(mgDescJson));
            data.put("mgFullDescription", machineGeneratedContentService.extractFullDescription(mgDescJson));
            data.put("mgReviewAssessment", machineGeneratedContentService.extractReviewAssessment(mgDescJson));
            data.put("mgSafetyAssessment", machineGeneratedContentService.extractSafetyAssessment(mgDescJson));
            data.put("mgRiskScore", machineGeneratedContentService.extractRiskScore(mgDescJson));

            results.add(data);
        }
        response.put("results", results);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public void setMachineGeneratedContentService(MachineGeneratedContentService machineGeneratedContentService) {
        this.machineGeneratedContentService = machineGeneratedContentService;
    }

    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }
}
