package edu.unc.lib.boxc.operations.jms.pdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Service for sending requests for aggregate PDF generation
 * @author krwong
 */
public class PdfRequestSender extends MessageSender {
    private static final Logger log = LoggerFactory.getLogger(PdfRequestSender.class);
    private static final ObjectWriter MAPPER = new ObjectMapper().writerFor(PdfRequest.class);

    /**
     * Send a PdfRequest to the configured JMS queue
     * @param request
     * @throws IOException
     */
    public void sendToQueue(PdfRequest request) throws IOException {
        String messageBody = MAPPER.writeValueAsString(request);
        sendMessage(messageBody);
        log.info("Job to generate aggregate PDF with OCR has been queued for {}", request.getAgent().getUsername());
    }
}
