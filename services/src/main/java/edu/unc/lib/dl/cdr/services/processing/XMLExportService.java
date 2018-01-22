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
package edu.unc.lib.dl.cdr.services.processing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.cdr.services.rest.modify.ExportXMLController.XMLExportRequest;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;

/**
 * A service that starts a job for exporting the XML metadata of specified objects
 *
 * @author harring
 *
 */
public class XMLExportService {
    private SearchStateFactory searchStateFactory;
    private SolrQueryLayerService queryLayer;

    private final List<String> resultFields = Arrays.asList(SearchFieldKeys.ID.name());

    /**
     * Determines whether metadata for child objects should be exported, and then kicks off the job
     * @param username
     * @param group
     * @param request
     * @return
     */
    public Map<String,String> exportXml(String username, AccessGroupSet group, XMLExportRequest request) {
        if (request.exportChildren()) {
            addChildPIDsToRequest(request);
        }
        XMLExportJob job = new XMLExportJob(username, group, request);
        Thread thread = new Thread(job);
        thread.start();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Metadata export for " + request.getPids().size()
                + " objects has begun, you will receive the data via email soon");
        return response;
    }

    private void addChildPIDsToRequest(XMLExportRequest request) {
        List<String> pids = new ArrayList<>();
        for (String pid : request.getPids()) {
            SearchState searchState = searchStateFactory.createSearchState();
            searchState.setResultFields(resultFields);
            searchState.setSortType("export");
            searchState.setRowsPerPage(Integer.MAX_VALUE);

            SearchRequest searchRequest = new SearchRequest(searchState, GroupsThreadStore.getGroups());

            BriefObjectMetadata container = queryLayer.addSelectedContainer(pid, searchState, false);
            SearchResultResponse resultResponse = queryLayer.getSearchResults(searchRequest);

            List<BriefObjectMetadata> objects = resultResponse.getResultList();
            objects.add(0, container);

            for (BriefObjectMetadata object : objects) {
                pids.add(object.getPid().toString());
            }
        }
        // update the list of pids in the request with all of the child pids found
        request.setPids(pids);
    }

    public SearchStateFactory getSearchStateFactory() {
        return searchStateFactory;
    }

    public void setSearchStateFactory(SearchStateFactory searchStateFactory) {
        this.searchStateFactory = searchStateFactory;
    }

    public SolrQueryLayerService getQueryLayer() {
        return queryLayer;
    }

    public void setQueryLayer(SolrQueryLayerService queryLayer) {
        this.queryLayer = queryLayer;
    }

}
