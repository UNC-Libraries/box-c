package edu.unc.lib.boxc.search.api.requests;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;

/**
 *
 * @author bbpennel
 *
 */
public class HierarchicalBrowseRequest extends SearchRequest {
    private static final long serialVersionUID = 1L;
    private int retrievalDepth;
    private boolean includeFiles;

    public HierarchicalBrowseRequest(int retrievalDepth, AccessGroupSet accessGroups) {
        this(null, retrievalDepth, accessGroups);
    }

    public HierarchicalBrowseRequest(SearchState searchState, int retrievalDepth, AccessGroupSet accessGroups) {
        super(searchState, accessGroups);
        this.retrievalDepth = retrievalDepth;
    }

    public int getRetrievalDepth() {
        return retrievalDepth;
    }

    public void setRetrievalDepth(int retrievalDepth) {
        this.retrievalDepth = retrievalDepth;
    }

    public boolean isIncludeFiles() {
        return includeFiles;
    }

    public void setIncludeFiles(boolean includeFiles) {
        this.includeFiles = includeFiles;
    }
}
