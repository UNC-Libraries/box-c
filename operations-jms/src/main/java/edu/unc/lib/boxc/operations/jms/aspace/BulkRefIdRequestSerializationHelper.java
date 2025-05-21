package edu.unc.lib.boxc.operations.jms.aspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

public class BulkRefIdRequestSerializationHelper {
    private static final ObjectWriter REQUEST_WRITER;
    private static final ObjectReader REQUEST_READER;
    static {
        ObjectMapper mapper = new ObjectMapper();
        REQUEST_WRITER = mapper.writerFor(RefIdRequest.class);
        REQUEST_READER = mapper.readerFor(RefIdRequest.class);
    }

    private BulkRefIdRequestSerializationHelper() {
    }

    /**
     * Transform request into a JSON string
     * @param request
     * @return
     * @throws IOException
     */
    public static String toJson(RefIdRequest request) throws IOException {
        return REQUEST_WRITER.writeValueAsString(request);
    }

    /**
     * Transform JSON string to a ThumbnailRequest
     * @param json
     * @return
     * @throws IOException
     */
    public static RefIdRequest toRequest(String json) throws IOException {
        return REQUEST_READER.readValue(json);
    }
}
