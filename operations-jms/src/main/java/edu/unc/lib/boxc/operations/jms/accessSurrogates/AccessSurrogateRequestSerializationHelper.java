package edu.unc.lib.boxc.operations.jms.accessSurrogates;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequest;

import java.io.IOException;

public class AccessSurrogateRequestSerializationHelper {
    private static final ObjectWriter REQUEST_WRITER;
    private static final ObjectReader REQUEST_READER;

    static {
        ObjectMapper mapper = new ObjectMapper();
        REQUEST_WRITER = mapper.writerFor(AccessSurrogateRequest.class);
        REQUEST_READER = mapper.readerFor(AccessSurrogateRequest.class);
    }

    private AccessSurrogateRequestSerializationHelper() {
    }

    public static String toJson(AccessSurrogateRequest request) throws JsonProcessingException {
        return REQUEST_WRITER.writeValueAsString(request);
    }

    public static AccessSurrogateRequest toRequest(String json) throws IOException {
        return REQUEST_READER.readValue(json);
    }
}
