package edu.unc.lib.boxc.deposit.impl.jms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.jms.JMSException;
import org.springframework.jms.core.JmsTemplate;
import jakarta.jms.Message;

import java.io.IOException;

/**
 * Service for sending and receiving deposit job messages
 *
 * @author bbpennel
 */
public class DepositJobMessageService {
    private static final ObjectWriter REQUEST_WRITER;
    private static final ObjectReader REQUEST_READER;
    static {
        ObjectMapper mapper = new ObjectMapper();
        REQUEST_WRITER = mapper.writerFor(DepositJobMessage.class);
        REQUEST_READER = mapper.readerFor(DepositJobMessage.class);
    }
    private JmsTemplate jmsTemplate;
    private String destinationName;

    public void sendDepositJobMessage(DepositJobMessage message) throws JsonProcessingException {
        String json = REQUEST_WRITER.writeValueAsString(message);
        jmsTemplate.convertAndSend(destinationName, json);
    }

    public DepositJobMessage fromJson(Message message) throws IOException, JMSException {
        return REQUEST_READER.readValue(message.getBody(String.class));
    }

    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }
}
