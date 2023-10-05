package edu.unc.lib.boxc.operations.jms.thumbnails;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Service for sending requests to update child object used as thumbnail
 *
 * @author snluong
 */
public class ThumbnailRequestSender extends MessageSender {
    private static final Logger log = LoggerFactory.getLogger(ThumbnailRequestSender.class);
    private static final ObjectWriter MAPPER = new ObjectMapper().writerFor(ThumbnailRequest.class);

    /**
     * Send a ThumbnailRequest to the configured JMS queue
     * @param request
     * @throws IOException
     */
    public void sendToQueue(ThumbnailRequest request) throws IOException {
        String messageBody = MAPPER.writeValueAsString(request);
        sendMessage(messageBody);
        log.info("Job to {} thumbnail has been queued for {} with file {}",
                request.getAction(), request.getAgent().getUsername(), request.getFilePidString());
    }
}
