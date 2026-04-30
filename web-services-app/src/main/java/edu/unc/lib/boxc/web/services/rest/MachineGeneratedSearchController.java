package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService;
import edu.unc.lib.boxc.search.solr.services.SearchResultResponseDecoratorService;
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

import java.util.Arrays;
import java.util.Collections;
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
    @Autowired
    private SearchResultResponseDecoratorService searchResultResponseDecoratorService;

    @SuppressWarnings("unchecked")
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

        SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);
        if (resultResponse == null) {
            return new ResponseEntity<>(Collections.emptyMap(), HttpStatus.OK);
        }

        searchResultResponseDecoratorService.populateThumbnailUrls(searchRequest.getAccessGroups(), resultResponse);
        searchResultResponseDecoratorService.retrieveFacets(searchRequest, resultResponse);
        var searchResults = getResults(resultResponse, "search", request);
        var resultList = (List<Map<String, Object>>) searchResults.get("metadata");

        // add extracted machine generated fields to the search results
        for (var result : resultList) {
            var mgDescJson = machineGeneratedContentService.deserializeMachineGeneratedDescription(
                    (String) result.get(SearchFieldKey.MG_DESCRIPTION.getUrlParam()));
            result.put("mgAltText", machineGeneratedContentService.extractAltText(mgDescJson));
            result.put("mgTranscript", machineGeneratedContentService.extractTranscript(mgDescJson));
            result.put("mgFullDescription", machineGeneratedContentService.extractFullDescription(mgDescJson));
            result.put("mgReviewAssessment", machineGeneratedContentService.extractReviewAssessment(mgDescJson));
            result.put("mgSafetyAssessment", machineGeneratedContentService.extractSafetyAssessment(mgDescJson));
        }

        return new ResponseEntity<>(searchResults, HttpStatus.OK);
    }

    public void setMachineGeneratedContentService(MachineGeneratedContentService machineGeneratedContentService) {
        this.machineGeneratedContentService = machineGeneratedContentService;
    }

    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    public void setSearchResultResponseDecoratorService(SearchResultResponseDecoratorService searchResultResponseDecoratorService) {
        this.searchResultResponseDecoratorService = searchResultResponseDecoratorService;
    }
}
