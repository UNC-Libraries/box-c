package edu.unc.lib.boxc.search.solr.filters;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.search.api.SearchFieldKey;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        var filterString = filter.toFilterString();
        filterString = filterString.substring(1, filterString.length() - 1); // Trim off parens
        var parts = Arrays.asList(filterString.split(" OR ")); // Match regardless of order
        assertTrue(parts.contains("datastream:jp2|*||"));
        assertTrue(parts.contains("datastream:fulltext|*||"));
    }

    @Test
    public void datastreamsFilterTest() {
        var datastreamTypes = new HashSet<DatastreamType>();
        datastreamTypes.add(DatastreamType.JP2_ACCESS_COPY);
        datastreamTypes.add(DatastreamType.FULLTEXT_EXTRACTION);
        var filter = QueryFilterFactory.createFilter(SearchFieldKey.DATASTREAM, datastreamTypes);
        assertInstanceOf(MultipleDirectlyOwnedDatastreamsFilter.class, filter);
        var filterString = filter.toFilterString();
        filterString = filterString.substring(1, filterString.length() - 1); // Trim off parens
        var parts = Arrays.asList(filterString.split(" OR ")); // Match regardless of order
        assertTrue(parts.contains("datastream:jp2|*"));
        assertTrue(parts.contains("datastream:fulltext|*"));
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
