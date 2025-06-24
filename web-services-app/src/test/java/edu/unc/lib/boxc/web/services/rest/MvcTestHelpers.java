package edu.unc.lib.boxc.web.services.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;

/**
 * @author bbpennel
 */
public class MvcTestHelpers {
    private MvcTestHelpers() {
    }

    public static Map<String, Object> getMapFromResponse(MvcResult result) throws Exception {
        MapType type = defaultInstance().constructMapType(HashMap.class, String.class, Object.class);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(result.getResponse().getContentAsString(), type);
    }

    public static JsonNode getResponseAsJson(MvcResult result) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    public static byte[] makeRequestBody(Object details) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsBytes(details);
    }

    public static SearchResultResponse createSearchResponse(List<ContentObjectRecord> records) {
        var response = new SearchResultResponse();
        response.setResultList(new ArrayList<>(records));
        return response;
    }

    public static List<CSVRecord> parseCsvResponse(MockHttpServletResponse response) throws Exception {
        List<CSVRecord> csvList = new ArrayList<>();

        var format = CSVFormat.DEFAULT.builder().setSkipHeaderRecord(true).get();
        var parser = CSVParser.parse(new StringReader(response.getContentAsString()), format);
        parser.forEach(csvList::add);

        return csvList;
    }
}
