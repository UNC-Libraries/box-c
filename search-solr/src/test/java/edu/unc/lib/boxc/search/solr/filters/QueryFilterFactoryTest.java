package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.search.api.SearchFieldKey;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * @author lfarrell
 */
public class QueryFilterFactoryTest {
    @Test
    public void NamedDatastreamFilterTest() {
        var filter = QueryFilterFactory.createFilter(SearchFieldKey.DATASTREAM, DatastreamType.JP2_ACCESS_COPY);
        assertInstanceOf(NamedDatastreamFilter.class, filter);
    }

    @Test
    public void MultipleDirectlyOwnedDatastreamsFilterTest() {
        var datastreamTypes = new HashSet<DatastreamType>();
        datastreamTypes.add(DatastreamType.JP2_ACCESS_COPY);
        datastreamTypes.add(DatastreamType.FULLTEXT_EXTRACTION);
        var filter = QueryFilterFactory.createFilter(SearchFieldKey.DATASTREAM, datastreamTypes);
        assertInstanceOf(MultipleDirectlyOwnedDatastreamsFilter.class, filter);
    }

    @Test
    public void HasPopulatedFieldFilterTest() {
        var filter = QueryFilterFactory.createFilter(SearchFieldKey.STREAMING_TYPE);
        assertInstanceOf(HasPopulatedFieldFilter.class, filter);
    }

    @Test
    public void HasViewerTypeFilterTest() {
        var filter = QueryFilterFactory.createHasViewerTypeFilter(SearchFieldKey.FILE_FORMAT_TYPE, "application/pdf");
        assertInstanceOf(HasViewerTypeFilter.class, filter);
    }
}
