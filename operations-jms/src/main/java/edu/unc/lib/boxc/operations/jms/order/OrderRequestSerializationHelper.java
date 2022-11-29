package edu.unc.lib.boxc.operations.jms.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

/**
 * Helper methods for serializing and deserializing order requests
 *
 * @author bbpennel
 */
public class OrderRequestSerializationHelper {
    private static final ObjectWriter MULTI_WRITER;
    private static final ObjectReader MULTI_READER;
    static {
        ObjectMapper mapper = new ObjectMapper();
        MULTI_WRITER = mapper.writerFor(MultiParentOrderRequest.class);
        MULTI_READER = mapper.readerFor(MultiParentOrderRequest.class);
    }

    private OrderRequestSerializationHelper() {
    }

    /**
     * Transform request into a JSON string
     * @param request
     * @return
     * @throws IOException
     */
    public static String toJson(MultiParentOrderRequest request) throws IOException {
        return MULTI_WRITER.writeValueAsString(request);
    }

    /**
     * Transform JSON string to a MultiParentOrderRequest
     * @param json
     * @return
     * @throws IOException
     */
    public static MultiParentOrderRequest toRequest(String json) throws IOException {
        return MULTI_READER.readValue(json);
    }
}
