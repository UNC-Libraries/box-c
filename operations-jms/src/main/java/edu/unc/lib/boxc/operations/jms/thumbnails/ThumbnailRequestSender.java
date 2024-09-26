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
    private static final ObjectWriter IMPORT_MAPPER = new ObjectMapper().writerFor(ImportThumbnailRequest.class);
    private String importDestinationName;

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

    /**
     * Send an ImportThumbnailRequest to the appropriate JMS queue
     * @param request
     * @throws IOException
     */
    public void sendToImportQueue(ImportThumbnailRequest request) throws IOException {
        String messageBody = IMPORT_MAPPER.writeValueAsString(request);
        sendImportMessage(messageBody);
        log.info("Job to import thumbnail has been queued for {} with object {}",
                request.getAgent().getUsername(), request.getPidString());
    }

    public void sendImportMessage(String msgStr) {
        jmsTemplate.send(importDestinationName, (session -> {
            // Committing the session to flush changes in long-running threads
            if (session.getTransacted()) {
                session.commit();
            }
            return session.createTextMessage(msgStr);
        }));
    }

    public void setImportDestinationName(String importDestinationName) {
        this.importDestinationName = importDestinationName;
    }
}
