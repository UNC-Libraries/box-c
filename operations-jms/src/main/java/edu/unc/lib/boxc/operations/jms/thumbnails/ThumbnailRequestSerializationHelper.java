package edu.unc.lib.boxc.operations.jms.thumbnails;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

/**
 * Helper methods for serializing and deserializing thumbnail requests
 *
 * @author snluong
 */
public class ThumbnailRequestSerializationHelper {
    private static final ObjectWriter REQUEST_WRITER;
    private static final ObjectReader REQUEST_READER;
    static {
        ObjectMapper mapper = new ObjectMapper();
        REQUEST_WRITER = mapper.writerFor(ThumbnailRequest.class);
        REQUEST_READER = mapper.readerFor(ThumbnailRequest.class);
    }

    private ThumbnailRequestSerializationHelper() {
    }

    /**
     * Transform request into a JSON string
     * @param request
     * @return
     * @throws IOException
     */
    public static String toJson(ThumbnailRequest request) throws IOException {
        return REQUEST_WRITER.writeValueAsString(request);
    }

    /**
     * Transform JSON string to a ThumbnailRequest
     * @param json
     * @return
     * @throws IOException
     */
    public static ThumbnailRequest toRequest(String json) throws IOException {
        return REQUEST_READER.readValue(json);
    }
}
