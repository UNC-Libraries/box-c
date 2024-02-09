package edu.unc.lib.boxc.operations.jms.views;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

/**
 * Helper methods for serializing and deserializing view behavior requests
 * @author snluong
 */
public class ViewBehaviorRequestSerializationHelper {
    private static final ObjectWriter REQUEST_WRITER;
    private static final ObjectReader REQUEST_READER;

    static {
        ObjectMapper mapper = new ObjectMapper();
        REQUEST_WRITER = mapper.writerFor(ViewBehaviorRequest.class);
        REQUEST_READER = mapper.readerFor(ViewBehaviorRequest.class);
    }

    private ViewBehaviorRequestSerializationHelper() {
    }

    public static String toJson(ViewBehaviorRequest request) throws JsonProcessingException {
        return REQUEST_WRITER.writeValueAsString(request);
    }

    public static ViewBehaviorRequest toRequest(String json) throws IOException {
        return REQUEST_READER.readValue(json);
    }
}
