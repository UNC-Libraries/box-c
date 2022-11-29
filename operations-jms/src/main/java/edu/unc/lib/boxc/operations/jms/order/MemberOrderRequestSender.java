package edu.unc.lib.boxc.operations.jms.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Service for sending requests to update member order
 *
 * @author bbpennel
 */
public class MemberOrderRequestSender extends MessageSender {
    private static final Logger log = LoggerFactory.getLogger(MemberOrderRequestSender.class);
    private static final ObjectWriter MAPPER = new ObjectMapper().writerFor(MultiParentOrderRequest.class);

    /**
     * Send a MultiParentOrderRequest to the configured JMS queue
     * @param request
     * @throws IOException
     */
    public void sendToQueue(MultiParentOrderRequest request) throws IOException {
        String messageBody = MAPPER.writeValueAsString(request);
        sendMessage(messageBody);
        log.info("Job to update member order has been queued for {}", request.getAgent().getUsername());
    }
}
