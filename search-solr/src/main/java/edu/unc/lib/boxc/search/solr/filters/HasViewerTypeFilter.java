package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.filters.QueryFilter;

/**
 * @author lfarrell
 */
public class HasViewerTypeFilter  implements QueryFilter {
    private final SearchFieldKey fieldKey;
    private final String fileType;

    protected HasViewerTypeFilter(SearchFieldKey fieldKey, String fileType) {
        this.fieldKey = fieldKey;
        this.fileType= fileType;
    }

    @Override
    public String toFilterString() {
        return getFieldKey().getSolrField() + ":" + fileType;
    }

    @Override
    public SearchFieldKey getFieldKey() {
        return fieldKey;
    }

    public String getFileType() {
        return fileType;
    }
}
