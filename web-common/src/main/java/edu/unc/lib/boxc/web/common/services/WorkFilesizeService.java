package edu.unc.lib.boxc.web.common.services;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service to retrieve the total filesize of all files in a work
 *
 * @author lfarrell
 */
public class WorkFilesizeService {
    private static final Logger log = LoggerFactory.getLogger(AccessCopiesService.class);

    private GlobalPermissionEvaluator globalPermissionEvaluator;
    private SolrSearchService solrSearchService;

    public Long getTotalFilesize(ContentObjectRecord contentObjectRecord, AccessGroupSet principals) {
        var request = buildChildrenQuery(contentObjectRecord, principals);
        var resp = solrSearchService.getSearchResults(request);

        if (resp.getResultCount() > 0) {
            return resp.getResultList().stream()
                    .mapToLong(ContentObjectRecord::getFilesizeSort)
                    .reduce(0, Long::sum);
        } else {
            log.debug("No child objects for work {}", contentObjectRecord.getId());
            return null;
        }
    }

    private SearchRequest buildChildrenQuery(ContentObjectRecord briefObj, AccessGroupSet principals) {
        SearchState searchState = new SearchState();
        if (!globalPermissionEvaluator.hasGlobalPrincipal(principals)) {
            searchState.setPermissionLimits(List.of(Permission.viewOriginal));
        }
        searchState.setFacetsToRetrieve(null);
        searchState.setIgnoreMaxRows(true);
        searchState.setRowsPerPage(3000);
        searchState.setSortType("default");
        searchState.setResultFields(List.of(SearchFieldKey.FILESIZE.name()));
        CutoffFacet selectedPath = briefObj.getPath();
        searchState.addFacet(selectedPath);
        SearchRequest searchRequest = new SearchRequest(searchState, principals);
        searchRequest.setApplyCutoffs(true);

        return searchRequest;
    }

    public void setGlobalPermissionEvaluator(GlobalPermissionEvaluator globalPermissionEvaluator) {
        this.globalPermissionEvaluator = globalPermissionEvaluator;
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }
}
