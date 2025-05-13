package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.filters.QueryFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter which restricts results to those with either JP2 access copy datastream or specified file types
 */
public class IIIFv3ViewableFilter implements QueryFilter {
    private static final List<String> FILE_TYPES = Arrays.asList("video/mp4",
            "video/mpeg",
            "video/quicktime",
            "video/mp4",
            "audio/mpeg");
    private static final String FILE_TYPE_FIELD = SearchFieldKey.FILE_FORMAT_TYPE.getSolrField();
    private static final String FILE_TYPE_FILTER = FILE_TYPES.stream().map( type -> FILE_TYPE_FIELD + ":" + type)
            .collect(Collectors.joining(" OR "));

    protected IIIFv3ViewableFilter() {
    }

    @Override
    public String toFilterString() {
        var datastreamFilter = SearchFieldKey.DATASTREAM.getSolrField() + ":" + DatastreamType.JP2_ACCESS_COPY.getId() + "|*";
        var datastreamFilterAudio = SearchFieldKey.DATASTREAM.getSolrField() + ":" + DatastreamType.AUDIO_ACCESS_COPY.getId() + "|*";
        return "((" + FILE_TYPE_FILTER + ") OR (" + datastreamFilter + ") OR (" + datastreamFilterAudio
                + ")) AND !" + FILE_TYPE_FIELD + ":\"application/pdf\"";
    }

    @Override
    public SearchFieldKey getFieldKey() {
        return null;
    }
}
