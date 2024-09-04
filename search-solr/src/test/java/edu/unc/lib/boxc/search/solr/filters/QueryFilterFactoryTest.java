package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.search.api.SearchFieldKey;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author lfarrell
 */
public class QueryFilterFactoryTest {
    @Test
    public void NamedDatastreamFilterTest() {
        var filter = QueryFilterFactory.createFilter(SearchFieldKey.DATASTREAM, DatastreamType.THUMBNAIL_LARGE);
        assertTrue(filter instanceof NamedDatastreamFilter);
    }

    @Test
    public void MultipleDirectlyOwnedDatastreamsFilterTest() {
        var datastreamTypes = new HashSet<DatastreamType>();
        datastreamTypes.add(DatastreamType.THUMBNAIL_LARGE);
        datastreamTypes.add(DatastreamType.THUMBNAIL_SMALL);
        var filter = QueryFilterFactory.createFilter(SearchFieldKey.DATASTREAM, datastreamTypes);
        assertTrue(filter instanceof MultipleDirectlyOwnedDatastreamsFilter);
    }

    @Test
    public void HasPopulatedFieldFilterTest() {
        var filter = QueryFilterFactory.createFilter(SearchFieldKey.STREAMING_TYPE);
        assertTrue(filter instanceof HasPopulatedFieldFilter);
    }

    @Test
    public void HasViewerTypeFilterTest() {
        var filter = QueryFilterFactory.createHasViewerTypeFilter(SearchFieldKey.FILE_FORMAT_TYPE, "application/pdf");
        assertTrue(filter instanceof HasViewerTypeFilter);
    }
}
