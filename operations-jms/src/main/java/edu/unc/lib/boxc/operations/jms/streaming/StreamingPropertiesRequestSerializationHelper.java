package edu.unc.lib.boxc.operations.jms.streaming;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

/**
 * Helper methods for serializing and deserializing StreamingPropertiesRequests
 */
public class StreamingPropertiesRequestSerializationHelper {
    private static final ObjectWriter REQUEST_WRITER;
    private static final ObjectReader REQUEST_READER;
    static {
        ObjectMapper mapper = new ObjectMapper();
        REQUEST_WRITER = mapper.writerFor(StreamingPropertiesRequest.class);
        REQUEST_READER = mapper.readerFor(StreamingPropertiesRequest.class);
    }

    private StreamingPropertiesRequestSerializationHelper() {
    }

    /**
     * Transform request into a JSON string
     * @param request
     * @return
     * @throws IOException
     */
    public static String toJson(StreamingPropertiesRequest request) throws IOException {
        return REQUEST_WRITER.writeValueAsString(request);
    }

    /**
     * Transform JSON string to a StreamingRequest
     * @param json
     * @return
     * @throws IOException
     */
    public static StreamingPropertiesRequest toRequest(String json) throws IOException {
        return REQUEST_READER.readValue(json);
    }
}
