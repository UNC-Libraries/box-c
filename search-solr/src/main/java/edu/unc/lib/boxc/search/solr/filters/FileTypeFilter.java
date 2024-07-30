package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.filters.QueryFilter;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filter which restricts results to those with the specified file types
 */
public class FileTypeFilter implements QueryFilter {
    private SearchFieldKey fieldKey;
    private Set<String> fileTypes;

    protected FileTypeFilter(SearchFieldKey fieldKey, Set<String> fileTypes) {
        this.fieldKey = fieldKey;
        this.fileTypes = fileTypes;
    }
    @Override
    public String toFilterString() {
        var solrField = getFieldKey().getSolrField();
        return  getFileTypes().stream().map( type -> solrField + ":" + type)
                .collect(Collectors.joining(" OR ", "(", ")"));
    }

    @Override
    public SearchFieldKey getFieldKey() {
        return fieldKey;
    }

    public Set<String> getFileTypes() {
        return fileTypes;
    }
}
