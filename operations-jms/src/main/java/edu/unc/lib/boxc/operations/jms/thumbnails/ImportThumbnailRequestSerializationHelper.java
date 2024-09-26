package edu.unc.lib.boxc.operations.jms.thumbnails;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

/**
 * Helper methods for serializing and deserializing import thumbnail requests
 *
 * @author snluong
 */
public class ImportThumbnailRequestSerializationHelper {
    private static final ObjectWriter REQUEST_WRITER;
    private static final ObjectReader REQUEST_READER;
    static {
        ObjectMapper mapper = new ObjectMapper();
        REQUEST_WRITER = mapper.writerFor(ImportThumbnailRequest.class);
        REQUEST_READER = mapper.readerFor(ImportThumbnailRequest.class);
    }

    private ImportThumbnailRequestSerializationHelper() {
    }

    /**
     * Transform request into a JSON string
     * @param request
     * @return
     * @throws IOException
     */
    public static String toJson(ImportThumbnailRequest request) throws IOException {
        return REQUEST_WRITER.writeValueAsString(request);
    }

    /**
     * Transform JSON string to an ImportThumbnailRequest
     * @param json
     * @return
     * @throws IOException
     */
    public static ImportThumbnailRequest toRequest(String json) throws IOException {
        return REQUEST_READER.readValue(json);
    }
}

