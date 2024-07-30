package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.filters.QueryFilter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filter which restricts results to those with the specified file types
 */
public class FileTypeFilter implements QueryFilter {
    private SearchFieldKey fieldKey;
    private List<String> fileTypes;

    protected FileTypeFilter(List<String> fileTypes) {
        this.fileTypes = fileTypes;
    }
    @Override
    public String toFilterString() {
        var fileTypeField = SearchFieldKey.FILE_FORMAT_TYPE.getSolrField();
        return  getFileTypes().stream().map( type -> fileTypeField + ":" + type)
                .collect(Collectors.joining(" OR ", "(", ")"));
    }

    @Override
    public SearchFieldKey getFieldKey() {
        return fieldKey;
    }

    public List<String> getFileTypes() {
        return fileTypes;
    }
}
