package edu.unc.lib.boxc.operations.jms.streaming;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Service for sending requests to update streaming properties of a file object
 */
public class StreamingPropertiesRequestSender extends MessageSender {
    private static final Logger log = LoggerFactory.getLogger(StreamingPropertiesRequestSender.class);
    private static final ObjectWriter MAPPER = new ObjectMapper().writerFor(StreamingPropertiesRequest.class);

    public void sendToQueue(StreamingPropertiesRequest request) throws IOException {
        String messageBody = MAPPER.writeValueAsString(request);
        sendMessage(messageBody);
        log.info("Job to update streaming properties has been queued for file {} by {}",
                request.getId(), request.getAgent().getUsername());
    }
}
