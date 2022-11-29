package edu.unc.lib.boxc.operations.jms.exportxml;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import edu.unc.lib.boxc.operations.jms.MessageSender;

/**
 * Service for sending object xml export requests
 *
 * @author bbpennel
 */
public class ExportXMLRequestService extends MessageSender {
    private static final Logger log = getLogger(ExportXMLRequestService.class);
    private ObjectWriter requestWriter;
    private ObjectReader requestReader;

    public ExportXMLRequestService() {
        JavaTimeModule module = new JavaTimeModule();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        requestWriter = mapper.writerFor(ExportXMLRequest.class);
        requestReader = mapper.readerFor(ExportXMLRequest.class);
    }

    /**
     * Send a XML export request to the route configured for this service
     * @param request
     * @throws IOException
     */
    public void sendRequest(ExportXMLRequest request) throws IOException {
        sendMessage(serializeRequest(request));
        log.info("Bulk export job has been queued for {}", request.getAgent().getUsername());
    }

    /**
     * @param request
     * @return Request serialized as a json string
     * @throws IOException
     */
    public String serializeRequest(ExportXMLRequest request) throws IOException {
        return requestWriter.writeValueAsString(request);
    }

    /**
     * @param body
     * @return The given body, deserialized as a request object
     * @throws IOException
     */
    public ExportXMLRequest deserializeRequest(String body) throws IOException {
        return requestReader.readValue(body);
    }
}
