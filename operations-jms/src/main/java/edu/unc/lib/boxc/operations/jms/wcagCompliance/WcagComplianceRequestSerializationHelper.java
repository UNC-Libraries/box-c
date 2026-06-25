package edu.unc.lib.boxc.operations.jms.wcagCompliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

/**
 * Helper methods for serializing and deserializing WCAG Compliance requests
 */
public class WcagComplianceRequestSerializationHelper {
    private static final ObjectWriter REQUEST_WRITER;
    private static final ObjectReader REQUEST_READER;

    static {
        ObjectMapper mapper = new ObjectMapper();
        REQUEST_WRITER = mapper.writerFor(WcagComplianceRequest.class);
        REQUEST_READER = mapper.readerFor(WcagComplianceRequest.class);
    }

    private WcagComplianceRequestSerializationHelper() {
    }

    /**
     * Transform request into a JSON string
     * @param request WCAG Compliance Request
     * @return json
     * @throws IOException
     */
    public static String toJson(WcagComplianceRequest request) throws IOException {
        return REQUEST_WRITER.writeValueAsString(request);
    }

    /**
     * Transform JSON string to a ThumbnailRequest
     * @param json
     * @return WCAG Compliance Request
     * @throws IOException
     */
    public static WcagComplianceRequest toRequest(String json) throws IOException {
        return REQUEST_READER.readValue(json);
    }
}
