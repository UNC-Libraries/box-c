package edu.unc.lib.boxc.services.camel.patronAccess;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.operations.impl.acl.PatronAccessAssignmentService;
import edu.unc.lib.boxc.operations.impl.acl.PatronAccessAssignmentService.PatronAccessAssignmentRequest;

/**
 * Processor which performs a patron access assignment operation
 *
 * @author bbpennel
 */
public class PatronAccessAssignmentProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(PatronAccessAssignmentProcessor.class);
    private static final ObjectReader MAPPER = new ObjectMapper().readerFor(PatronAccessAssignmentRequest.class);

    private PatronAccessAssignmentService assignmentService;

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        PatronAccessAssignmentRequest request = MAPPER.readValue((String) in.getBody());
        try {
            assignmentService.updatePatronAccess(request);
        } catch (AccessRestrictionException e) {
            log.warn(e.getMessage());
        }
    }

    public void setPatronAccessAssignmentService(PatronAccessAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }
}
