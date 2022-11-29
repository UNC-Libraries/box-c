package edu.unc.lib.boxc.web.common.controllers;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.exceptions.InvalidHierarchicalFacetException;
import edu.unc.lib.boxc.search.api.requests.HierarchicalBrowseRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.responses.HierarchicalBrowseResultResponse;
import edu.unc.lib.boxc.search.solr.services.StructureQueryService;

/**
 * Base structure browse controller.
 * This is separate from StructureResultsController to allow for other controllers to use the same base functionality,
 * since controllers cannot inherit from each other.
 * @author bbpennel
 *
 */
public class AbstractStructureResultsController extends AbstractSolrSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractStructureResultsController.class);

    @Autowired
    protected StructureQueryService structureService;

    protected List<String> tierResultFieldsList;

    @PostConstruct
    public void init() {
        tierResultFieldsList = SearchSettings.RESULT_FIELDS_STRUCTURE;
    }

    protected HierarchicalBrowseResultResponse getStructureResult(String pid, boolean includeFiles,
            boolean collectionMode, HttpServletRequest request) {
        int depth;
        try {
            depth = Integer.parseInt(request.getParameter("depth"));
            if (depth > searchSettings.structuredDepthMax) {
                depth = searchSettings.structuredDepthMax;
            }
        } catch (Exception e) {
            depth = searchSettings.structuredDepthDefault;
        }

        // Request object for the search
        HierarchicalBrowseRequest browseRequest = new HierarchicalBrowseRequest(depth,
                getAgentPrincipals().getPrincipals());
        browseRequest.setSearchState(this.searchStateFactory.createStructureBrowseSearchState(request
                .getParameterMap()));
        if (pid != null) {
            browseRequest.setRootPid(PIDs.get(pid));
        }
        browseRequest.setIncludeFiles(includeFiles);

        SearchState searchState = browseRequest.getSearchState();

        try {
            searchActionService.executeActions(searchState, request.getParameterMap());
        } catch (InvalidHierarchicalFacetException e) {
            LOG.debug("An invalid facet was provided: " + request.getQueryString(), e);
        }

        if (pid == null && !searchState.getFacets().containsKey(SearchFieldKey.ANCESTOR_PATH.name())) {
            browseRequest.setRetrievalDepth(1);
        }

        HierarchicalBrowseResultResponse resultResponse = null;
        if (collectionMode) {
            resultResponse = structureService.getExpandedStructurePath(browseRequest);
        } else {
            resultResponse = structureService.getHierarchicalBrowseResults(browseRequest);
        }

        resultResponse.setSearchState(searchState);

        return resultResponse;
    }
}
