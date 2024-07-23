package edu.unc.lib.boxc.web.services.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.HashMap;
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
}
