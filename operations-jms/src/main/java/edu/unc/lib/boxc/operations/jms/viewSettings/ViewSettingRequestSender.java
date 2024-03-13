package edu.unc.lib.boxc.operations.jms.viewSettings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Service for sending requests to update view settings of a work object
 */
public class ViewSettingRequestSender extends MessageSender {
    private static final Logger log = LoggerFactory.getLogger(ViewSettingRequestSender.class);
    private static final ObjectWriter MAPPER = new ObjectMapper().writerFor(ViewSettingRequest.class);

    public void sendToQueue(ViewSettingRequest request) throws IOException {
        String messageBody = MAPPER.writeValueAsString(request);
        sendMessage(messageBody);
        log.info("Job to update view setting has been queued for work {} by {}",
                request.getObjectPidString(), request.getAgent().getUsername());
    }

}
