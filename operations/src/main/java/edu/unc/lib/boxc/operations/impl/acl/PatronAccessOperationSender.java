package edu.unc.lib.boxc.operations.impl.acl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import edu.unc.lib.boxc.operations.impl.acl.PatronAccessAssignmentService.PatronAccessAssignmentRequest;
import edu.unc.lib.boxc.operations.jms.MessageSender;

/**
 * Service which sends requests to update patron access
 *
 * @author bbpennel
 */
public class PatronAccessOperationSender extends MessageSender {
    private static final Logger log = LoggerFactory.getLogger(PatronAccessOperationSender.class);
    private static final ObjectWriter MAPPER = new ObjectMapper().writerFor(PatronAccessAssignmentRequest.class);

    /**
     * Push a patron access update job
     *
     * @param request
     * @throws IOException
     */
    public void sendUpdateRequest(PatronAccessAssignmentRequest request) throws IOException {
        String messageBody = MAPPER.writeValueAsString(request);
        sendMessage(messageBody);
        log.info("Job to update patron access for {} has been queued for {}",
                request.getTargetPid(), request.getAgent().getUsername());
    }
}
