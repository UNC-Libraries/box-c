package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.search.api.SearchFieldKey;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * @author lfarrell
 */
public class QueryFilterFactoryTest {
    @Test
    public void namedDatastreamFilterTest() {
        var filter = QueryFilterFactory.createFilter(SearchFieldKey.DATASTREAM, DatastreamType.JP2_ACCESS_COPY);
        assertInstanceOf(NamedDatastreamFilter.class, filter);
    }

    @Test
    public void multipleDirectlyOwnedDatastreamsFilterTest() {
        var datastreamTypes = new HashSet<DatastreamType>();
        datastreamTypes.add(DatastreamType.JP2_ACCESS_COPY);
        datastreamTypes.add(DatastreamType.FULLTEXT_EXTRACTION);
        var filter = QueryFilterFactory.createDirectlyOwnedFilter(SearchFieldKey.DATASTREAM, datastreamTypes);
        assertInstanceOf(MultipleDirectlyOwnedDatastreamsFilter.class, filter);
        assertEquals("(datastream:jp2|*|| OR datastream:fulltext|*||)", filter.toFilterString());
    }

    @Test
    public void datastreamsFilterTest() {
        var datastreamTypes = new HashSet<DatastreamType>();
        datastreamTypes.add(DatastreamType.JP2_ACCESS_COPY);
        datastreamTypes.add(DatastreamType.FULLTEXT_EXTRACTION);
        var filter = QueryFilterFactory.createFilter(SearchFieldKey.DATASTREAM, datastreamTypes);
        assertInstanceOf(MultipleDirectlyOwnedDatastreamsFilter.class, filter);
        assertEquals("(datastream:jp2|* OR datastream:fulltext|*)", filter.toFilterString());
    }

    @Test
    public void hasPopulatedFieldFilterTest() {
        var filter = QueryFilterFactory.createFilter(SearchFieldKey.STREAMING_TYPE);
        assertInstanceOf(HasPopulatedFieldFilter.class, filter);
    }

    @Test
    public void hasValuesFilterTest() {
        var filter = QueryFilterFactory.createHasValuesFilter(SearchFieldKey.FILE_FORMAT_TYPE, List.of("application/pdf"));
        assertInstanceOf(HasValuesFilter.class, filter);
    }
}
