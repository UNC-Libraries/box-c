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
            SearchFieldKey.FILE_FORMAT_CATEGORY.name(), SearchFieldKey.FILE_FORMAT_TYPE.name(),
            SearchFieldKey.FILE_FORMAT_DESCRIPTION.name(),
            SearchFieldKey.CONTENT_STATUS.name(), SearchFieldKey.TIMESTAMP.name(),
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
