package edu.unc.lib.boxc.indexing.solr.utils;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.facets.CutoffFacetImpl;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;

public class SearchGeneratorUtil {
    private static final int MAX_FILES_PER_WORK = 10000;
    private SearchGeneratorUtil(){
    }
    public static SearchState getSearchState(IndexDocumentBean doc, ContentPathFactory contentPathFactory) {
        var searchState = new SearchState();
        var objectPath = getObjectPath(doc, contentPathFactory);
        searchState.setFacet(objectPath);
        searchState.setFacet(new GenericFacet(SearchFieldKey.RESOURCE_TYPE.name(), ResourceType.File.name()));
        searchState.setRowsPerPage(MAX_FILES_PER_WORK);
        return searchState;
    }
    private static CutoffFacet getObjectPath(IndexDocumentBean doc, ContentPathFactory contentPathFactory) {
        CutoffFacetImpl ancestorPath;
        if (doc.getAncestorPath() != null && !doc.getAncestorPath().isEmpty()) {
            ancestorPath = new CutoffFacetImpl(SearchFieldKey.ANCESTOR_PATH.name(), doc.getAncestorPath(), -1);
        } else {
            var ancestorPids = contentPathFactory.getAncestorPids(doc.getPid());
            ancestorPath = new CutoffFacetImpl(SearchFieldKey.ANCESTOR_PATH.name(), ancestorPids);
        }
        ancestorPath.addNode(doc.getId());
        return ancestorPath;
    }

    public static SearchResultResponse getSearchResults(SearchState searchState, SolrSearchService solrSearchService) {
        var searchRequest = new SearchRequest();
        searchRequest.setSearchState(searchState);
        return solrSearchService.getSearchResults(searchRequest);
    }
}
