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

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.ChildrenCountService;
import edu.unc.lib.boxc.web.common.controllers.AbstractSolrSearchController;

/**
 *
 * @author bbpennel
 *
 */
public class AbstractSearchController extends AbstractSolrSearchController {

    @Autowired
    protected ChildrenCountService childrenCountService;

    protected static List<String> resultsFieldList = Arrays.asList(SearchFieldKey.ID.name(),
            SearchFieldKey.TITLE.name(), SearchFieldKey.CREATOR.name(), SearchFieldKey.DATASTREAM.name(),
            SearchFieldKey.DATE_ADDED.name(), SearchFieldKey.DATE_UPDATED.name(),
            SearchFieldKey.RESOURCE_TYPE.name(),
            SearchFieldKey.STATUS.name(), SearchFieldKey.VERSION.name(),SearchFieldKey.ROLE_GROUP.name(),
            SearchFieldKey.CONTENT_TYPE.name(),
            SearchFieldKey.CONTENT_STATUS.name(), SearchFieldKey.LABEL.name(), SearchFieldKey.TIMESTAMP.name(),
            SearchFieldKey.ANCESTOR_PATH.name(), SearchFieldKey.ROLLUP_ID.name());

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
