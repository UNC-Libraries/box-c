package edu.unc.lib.boxc.operations.jms.aspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BulkRefIdRequestSender extends MessageSender {
    private static final Logger log = LoggerFactory.getLogger(BulkRefIdRequestSender.class);
    private static final ObjectWriter MAPPER = new ObjectMapper().writerFor(RefIdRequest.class);

    /**
     * Send a RefIdRequest to the configured JMS queue
     * @param request
     * @throws IOException
     */
    public void sendToQueue(RefIdRequest request) throws IOException {
        String messageBody = MAPPER.writeValueAsString(request);
        sendMessage(messageBody);
        log.info("Job to update Aspace Ref ID has been queued for {} with Work {}",
                request.getAgent().getUsername(), request.getPidString());
    }
}
