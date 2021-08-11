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

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

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
        tierResultFieldsList = searchSettings.resultFields.get("structure");
    }

    protected HierarchicalBrowseResultResponse getStructureResult(String pid, boolean includeFiles,
            boolean collectionMode, boolean retrieveFacets, HttpServletRequest request) {
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
        browseRequest.setRetrieveFacets(retrieveFacets);
        if (retrieveFacets) {
            browseRequest.setSearchState(this.searchStateFactory.createHierarchicalBrowseSearchState(request
                    .getParameterMap()));

        } else {
            browseRequest.setSearchState(this.searchStateFactory.createStructureBrowseSearchState(request
                    .getParameterMap()));
        }
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

        if (retrieveFacets) {
            queryLayer.populateBreadcrumbs(browseRequest, resultResponse);
        }
        return resultResponse;
    }
}
