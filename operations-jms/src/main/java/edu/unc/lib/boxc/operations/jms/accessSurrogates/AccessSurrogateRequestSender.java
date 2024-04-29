package edu.unc.lib.boxc.operations.jms.accessSurrogates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AccessSurrogateRequestSender extends MessageSender {
    private static final Logger log = LoggerFactory.getLogger(AccessSurrogateRequestSender.class);
    private static final ObjectWriter MAPPER = new ObjectMapper().writerFor(AccessSurrogateRequest.class);

    public void sendToQueue(AccessSurrogateRequest request) throws IOException {
        String messageBody = MAPPER.writeValueAsString(request);
        sendMessage(messageBody);
        log.info("Job to {} access surrogates has been queued for file {} by {}",
                request.getAction(), request.getPidString(), request.getAgent().getUsername());
    }
}
