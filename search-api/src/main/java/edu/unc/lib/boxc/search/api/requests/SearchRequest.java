package edu.unc.lib.boxc.search.api.requests;

import java.io.Serializable;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Request bean for a brief record search. Handles basic searches and advanced searches.
 *
 * @author bbpennel
 */
public class SearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    protected SearchState searchState;
    private boolean retrieveFacets;
    private boolean applyCutoffs;
    protected AccessGroupSet accessGroups;
    protected String rootId;
    protected PID rootPid;

    public SearchRequest() {
        this((PID) null, null, null, false);
    }

    public SearchRequest(SearchState searchState, AccessGroupSet accessGroups) {
        this((PID) null, searchState, accessGroups, false);
    }

    public SearchRequest(SearchState searchState, AccessGroupSet accessGroups, boolean retrieveFacets) {
        this((PID) null, searchState, accessGroups, retrieveFacets);
    }

    public SearchRequest(PID rootPid, SearchState searchState, AccessGroupSet accessGroups, boolean retrieveFacets) {
        setSearchState(searchState);
        setAccessGroups(accessGroups);
        applyCutoffs = true;
        this.retrieveFacets = retrieveFacets;
        this.rootPid = rootPid;
    }

    public SearchState getSearchState() {
        return searchState;
    }

    public void setSearchState(SearchState searchState) {
        this.searchState = searchState;
    }

    public AccessGroupSet getAccessGroups() {
        return accessGroups;
    }

    public void setAccessGroups(AccessGroupSet accessGroups) {
        this.accessGroups = accessGroups;
    }

    public boolean isApplyCutoffs() {
        return applyCutoffs;
    }

    public void setApplyCutoffs(boolean applyCutoffs) {
        this.applyCutoffs = applyCutoffs;
    }

    /**
     * @return the rootPid
     */
    public PID getRootPid() {
        return rootPid;
    }

    /**
     * @param rootPid the rootPid to set
     */
    public void setRootPid(PID rootPid) {
        this.rootPid = rootPid;
    }

    public boolean isRetrieveFacets() {
        return retrieveFacets;
    }

    public void setRetrieveFacets(boolean retrieveFacets) {
        this.retrieveFacets = retrieveFacets;
    }
}
