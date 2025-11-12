package edu.unc.lib.boxc.operations.jms.collectionDisplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

/**
 * Helper methods for serializing and deserializing CollectionDisplayPropertiesRequests
 */
public class CollectionDisplayPropertiesSerializationHelper {
    private static final ObjectWriter REQUEST_WRITER;
    private static final ObjectReader REQUEST_READER;
    static {
        ObjectMapper mapper = new ObjectMapper();
        REQUEST_WRITER = mapper.writerFor(CollectionDisplayPropertiesRequest.class);
        REQUEST_READER = mapper.readerFor(CollectionDisplayPropertiesRequest.class);
    }

    private CollectionDisplayPropertiesSerializationHelper() {
    }

    /**
     * Transform request into a JSON string
     * @param request
     * @return
     * @throws IOException
     */
    public static String toJson(CollectionDisplayPropertiesRequest  request) throws IOException {
        return REQUEST_WRITER.writeValueAsString(request);
    }

    /**
     * Transform JSON string to a CollectionDisplay Request
     * @param json
     * @return
     * @throws IOException
     */
    public static CollectionDisplayPropertiesRequest toRequest(String json) throws IOException {
        return REQUEST_READER.readValue(json);
    }
}