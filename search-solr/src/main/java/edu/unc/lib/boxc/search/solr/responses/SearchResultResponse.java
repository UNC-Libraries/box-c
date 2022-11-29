package edu.unc.lib.boxc.search.solr.responses;

import edu.unc.lib.boxc.search.api.facets.FacetFieldList;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import org.apache.solr.client.solrj.SolrQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Response object for a search request.  Contains the list of results from the selected
 * page, the list of hierarchical and nonhierarchical facets, and the count of the total
 * number of results the query found.
 * @author bbpennel
 */
public class SearchResultResponse {
    private final Logger LOG = LoggerFactory.getLogger(SearchResultResponse.class);

    private ContentObjectRecord selectedContainer;
    private List<ContentObjectRecord> resultList;
    private FacetFieldList facetFields;
    private String minimumDateCreatedYear;
    private long resultCount;
    private SearchState searchState;
    private SolrQuery generatedQuery;

    public SearchResultResponse() {
    }

    public List<ContentObjectRecord> getResultList() {
        return resultList;
    }

    public void setResultList(List<ContentObjectRecord> resultList) {
        this.resultList = resultList;
    }

    public FacetFieldList getFacetFields() {
        return facetFields;
    }

    public void setFacetFields(FacetFieldList facetFields) {
        this.facetFields = facetFields;
    }

    public long getResultCount() {
        return resultCount;
    }

    public void setResultCount(long resultCount) {
        this.resultCount = resultCount;
    }

    public String getMinimumDateCreatedYear() {
        return minimumDateCreatedYear;
    }

    public void setMinimumDateCreatedYear(String year) {
        this.minimumDateCreatedYear = year;
    }

    public SearchState getSearchState() {
        return searchState;
    }

    public void setSearchState(SearchState searchState) {
        this.searchState = searchState;
    }

    public SolrQuery getGeneratedQuery() {
        return generatedQuery;
    }

    public void setGeneratedQuery(SolrQuery generatedQuery) {
        this.generatedQuery = generatedQuery;
    }

    public ContentObjectRecord getSelectedContainer() {
        return selectedContainer;
    }

    public void setSelectedContainer(ContentObjectRecord selectedContainer) {
        this.selectedContainer = selectedContainer;
    }

    public List<String> getIdList() {
        if (this.resultList == null) {
            return null;
        }
        List<String> ids = new ArrayList<String>();
        for (ContentObjectRecord brief: this.resultList) {
            ids.add(brief.getId());
        }
        return ids;
    }
}
