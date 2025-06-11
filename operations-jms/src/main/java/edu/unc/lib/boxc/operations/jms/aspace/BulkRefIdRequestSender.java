package edu.unc.lib.boxc.operations.jms.aspace;

import edu.unc.lib.boxc.operations.jms.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Service for sending requests to update bulk Ref IDs for work objects
 *
 * @author snluong
 */
public class BulkRefIdRequestSender extends MessageSender {
    private static final Logger log = LoggerFactory.getLogger(BulkRefIdRequestSender.class);

    /**
     * Send a RefIdRequest to the configured JMS queue
     * @param request
     * @throws IOException
     */
    public void sendToQueue(BulkRefIdRequest request) throws IOException {
        String messageBody = BulkRefIdRequestSerializationHelper.toJson(request);
        sendMessage(messageBody);
        log.info("Job to import Aspace Ref IDs has been queued for {}", request.getAgent().getUsername());
    }
}
