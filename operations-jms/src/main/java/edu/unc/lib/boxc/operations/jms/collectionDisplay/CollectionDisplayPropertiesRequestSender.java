package edu.unc.lib.boxc.operations.jms.collectionDisplay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Service for sending requests to update the public UI display properties of a CollectionObject
 */
public class CollectionDisplayPropertiesRequestSender extends MessageSender {
    private static final Logger log = LoggerFactory.getLogger(CollectionDisplayPropertiesRequestSender.class);
    private static final ObjectWriter MAPPER = new ObjectMapper().writerFor(CollectionDisplayPropertiesRequest.class);

    public void sendToQueue(CollectionDisplayPropertiesRequest request) throws IOException {
        String messageBody = MAPPER.writeValueAsString(request);
        sendMessage(messageBody);
        log.info("Job to update collection display properties has been queued for collection {} by {}",
                request.getId(), request.getAgent().getUsername());
    }
}