package edu.unc.lib.boxc.operations.jms.pdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

/**
 * Helper methods for serializing and deserializing aggregate PDF generation requests
 * @author krwong
 */
public class PdfRequestSerializationHelper {
    private static final ObjectWriter REQUEST_WRITER;
    private static final ObjectReader REQUEST_READER;
    static {
        ObjectMapper mapper = new ObjectMapper();
        REQUEST_WRITER = mapper.writerFor(PdfRequest.class);
        REQUEST_READER = mapper.readerFor(PdfRequest.class);
    }

    private PdfRequestSerializationHelper() {
    }

    /**
     * Transform request into a JSON string
     * @param request pdf request
     * @return json
     * @throws IOException
     */
    public static String toJson(PdfRequest request) throws IOException {
        return REQUEST_WRITER.writeValueAsString(request);
    }

    /**
     * Transform JSON string to a PdfRequest
     * @param json
     * @return pdf request
     * @throws IOException
     */
    public static PdfRequest toRequest(String json) throws IOException {
        return REQUEST_READER.readValue(json);
    }
}
