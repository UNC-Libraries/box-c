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

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.search.api.SearchFieldKeys;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.service.ChildrenCountService;
import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;

/**
 *
 * @author bbpennel
 *
 */
public class AbstractSearchController extends AbstractSolrSearchController {

    @Autowired
    protected ChildrenCountService childrenCountService;

    protected static List<String> resultsFieldList = Arrays.asList(SearchFieldKeys.ID.name(),
            SearchFieldKeys.TITLE.name(), SearchFieldKeys.CREATOR.name(), SearchFieldKeys.DATASTREAM.name(),
            SearchFieldKeys.DATE_ADDED.name(), SearchFieldKeys.DATE_UPDATED.name(),
            SearchFieldKeys.RESOURCE_TYPE.name(), SearchFieldKeys.CONTENT_MODEL.name(),
            SearchFieldKeys.STATUS.name(), SearchFieldKeys.VERSION.name(),SearchFieldKeys.ROLE_GROUP.name(),
            SearchFieldKeys.RELATIONS.name(), SearchFieldKeys.CONTENT_TYPE.name(),
            SearchFieldKeys.CONTENT_STATUS.name(), SearchFieldKeys.LABEL.name(), SearchFieldKeys.TIMESTAMP.name(),
            SearchFieldKeys.ANCESTOR_PATH.name(),
            SearchFieldKeys.IS_PART.name(), SearchFieldKeys.ROLLUP_ID.name());

    @Override
    protected SearchResultResponse getSearchResults(SearchRequest searchRequest) {
        return this.getSearchResults(searchRequest, resultsFieldList);
    }

    protected SearchResultResponse getSearchResults(SearchRequest searchRequest, List<String> resultsFieldList) {
        SearchState searchState = searchRequest.getSearchState();
        searchState.setResultFields(resultsFieldList);

        SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);

        List<ContentObjectRecord> objects = resultResponse.getResultList();
        childrenCountService.addChildrenCounts(objects, searchRequest.getAccessGroups());

        return resultResponse;
    }
}
