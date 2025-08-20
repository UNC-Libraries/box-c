package edu.unc.lib.boxc.deposit.impl.jms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.unc.lib.boxc.deposit.api.exceptions.DepositMessageException;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.springframework.jms.core.JmsTemplate;

import java.io.IOException;

/**
 * Service for sending and receiving deposit operation messages
 *
 * @author bbpennel
 */
public class DepositOperationMessageService {
    private static final ObjectWriter REQUEST_WRITER;
    private static final ObjectReader REQUEST_READER;
    static {
        ObjectMapper mapper = new ObjectMapper();
        REQUEST_WRITER = mapper.writerFor(DepositOperationMessage.class);
        REQUEST_READER = mapper.readerFor(DepositOperationMessage.class);
    }
    private JmsTemplate jmsTemplate;
    private String destinationName;

    public void sendDepositOperationMessage(DepositOperationMessage message) {
        try {
            String json = REQUEST_WRITER.writeValueAsString(message);
            jmsTemplate.convertAndSend(destinationName, json);
        } catch (JsonProcessingException e) {
            throw new DepositMessageException("Failed to serialize deposit message for " + message.getDepositId(), e);
        }
    }

    public DepositOperationMessage fromJson(Message message) throws IOException, JMSException {
        return REQUEST_READER.readValue(message.getBody(String.class));
    }

    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }
}
