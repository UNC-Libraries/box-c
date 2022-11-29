package edu.unc.lib.boxc.search.api.requests;

import edu.unc.lib.boxc.search.api.SearchFieldKey;

/**
 * Request for listing values of a facet
 *
 * @author bbpennel
 */
public class FacetValuesRequest {
    private SearchRequest baseSearchRequest;
    private SearchFieldKey facetFieldKey;
    private String sort;
    private Integer start;
    private Integer rows;

    public FacetValuesRequest(SearchFieldKey facetFieldKey) {
        this.facetFieldKey = facetFieldKey;
    }

    public SearchRequest getBaseSearchRequest() {
        return baseSearchRequest;
    }

    public void setBaseSearchRequest(SearchRequest baseSearchRequest) {
        this.baseSearchRequest = baseSearchRequest;
    }

    public SearchFieldKey getFacetFieldKey() {
        return facetFieldKey;
    }

    public void setFacetFieldKey(SearchFieldKey facetFieldKey) {
        this.facetFieldKey = facetFieldKey;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getRows() {
        return rows;
    }

    public void setRows(Integer rows) {
        this.rows = rows;
    }
}
