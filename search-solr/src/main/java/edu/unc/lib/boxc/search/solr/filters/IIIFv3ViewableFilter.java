package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.filters.QueryFilter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter which restricts results to those with either JP2 access copy datastream or specified file types
 */
public class IIIFv3ViewableFilter implements QueryFilter {
    private SearchFieldKey fieldKey;
    private List<String> fileTypes;

    protected IIIFv3ViewableFilter(List<String> fileTypes) {
        this.fileTypes = fileTypes;
    }
    @Override
    public String toFilterString() {
        var fileTypeField = SearchFieldKey.FILE_FORMAT_TYPE.getSolrField();
        var fileTypeFilter = getFileTypes().stream().map( type -> fileTypeField + ":" + type)
                .collect(Collectors.joining(" OR ", "(", ")"));
        var datastreamFilter = SearchFieldKey.DATASTREAM.getSolrField() + ":" + DatastreamType.JP2_ACCESS_COPY.getId() + "|*";
        return "(" + fileTypeFilter + ") OR (" + datastreamFilter + ")";
    }

    @Override
    public SearchFieldKey getFieldKey() {
        return fieldKey;
    }

    public List<String> getFileTypes() {
        return fileTypes;
    }

}
