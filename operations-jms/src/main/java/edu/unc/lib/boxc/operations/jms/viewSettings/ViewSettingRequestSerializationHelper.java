package edu.unc.lib.boxc.operations.jms.viewSettings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

/**
 * Helper methods for serializing and deserializing view setting requests
 * @author snluong
 */
public class ViewSettingRequestSerializationHelper {
    private static final ObjectWriter REQUEST_WRITER;
    private static final ObjectReader REQUEST_READER;

    static {
        ObjectMapper mapper = new ObjectMapper();
        REQUEST_WRITER = mapper.writerFor(ViewSettingRequest.class);
        REQUEST_READER = mapper.readerFor(ViewSettingRequest.class);
    }

    private ViewSettingRequestSerializationHelper() {
    }

    public static String toJson(ViewSettingRequest request) throws JsonProcessingException {
        return REQUEST_WRITER.writeValueAsString(request);
    }

    public static ViewSettingRequest toRequest(String json) throws IOException {
        return REQUEST_READER.readValue(json);
    }
}
