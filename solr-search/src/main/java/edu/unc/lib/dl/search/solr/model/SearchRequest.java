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
package edu.unc.lib.dl.search.solr.model;

import java.io.Serializable;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;

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

    public SearchRequest(String rootPid, SearchState searchState, AccessGroupSet accessGroups) {
        this(PIDs.get(rootPid), searchState, accessGroups, false);
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

    public void setRootPid(String rootId) {
        this.rootPid = PIDs.get(rootId);
    }

    public boolean isRetrieveFacets() {
        return retrieveFacets;
    }

    public void setRetrieveFacets(boolean retrieveFacets) {
        this.retrieveFacets = retrieveFacets;
    }
}
