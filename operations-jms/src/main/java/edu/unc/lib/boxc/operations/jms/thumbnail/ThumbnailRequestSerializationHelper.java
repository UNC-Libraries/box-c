package edu.unc.lib.boxc.operations.jms.thumbnail;

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
    private static final ObjectWriter MULTI_WRITER;
    private static final ObjectReader MULTI_READER;
    static {
        ObjectMapper mapper = new ObjectMapper();
        MULTI_WRITER = mapper.writerFor(ThumbnailRequest.class);
        MULTI_READER = mapper.readerFor(ThumbnailRequest.class);
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
        return MULTI_WRITER.writeValueAsString(request);
    }

    /**
     * Transform JSON string to a ThumbnailRequest
     * @param json
     * @return
     * @throws IOException
     */
    public static ThumbnailRequest toRequest(String json) throws IOException {
        return MULTI_READER.readValue(json);
    }
}
