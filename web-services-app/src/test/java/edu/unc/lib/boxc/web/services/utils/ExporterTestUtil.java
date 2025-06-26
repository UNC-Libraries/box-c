package edu.unc.lib.boxc.web.services.utils;

import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Util class for Exporter tests
 * assertion methods and mocking search results
 *
 * @author snluong
 */
public class ExporterTestUtil {
    public static void assertNumberOfEntries(int expected, List<CSVRecord> csvParser) throws IOException {
        assertEquals(expected, csvParser.size());
    }

    public static SearchResultResponse makeResultResponse(ContentObjectRecord... results) {
        var resp = new SearchResultResponse();
        resp.setResultList(Arrays.asList(results));
        resp.setResultCount(results.length);
        return resp;
    }

    public static SearchResultResponse makeEmptyResponse() {
        var resp = new SearchResultResponse();
        resp.setResultList(List.of());
        resp.setResultCount(0);
        return resp;
    }

    public static void mockSingleRecordResults(SolrSearchService solrSearchService, ContentObjectRecord parentRec, ContentObjectRecord... parentRecs) {
        when(solrSearchService.getObjectById(any())).thenReturn(parentRec, parentRecs);
    }

    public static void mockSearchResults(SolrSearchService solrSearchService, ContentObjectRecord... results) {
        when(solrSearchService.getSearchResults(any())).thenReturn(makeResultResponse(results));
    }
}
