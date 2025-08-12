package edu.unc.lib.boxc.deposit.impl.jms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.springframework.jms.core.JmsTemplate;

import java.io.IOException;

/**
 * Service for sending and receiving deposit pipeline messages
 *
 * @author bbpennel
 */
public class DepositPipelineMessageService {
    private static final ObjectWriter REQUEST_WRITER;
    private static final ObjectReader REQUEST_READER;
    static {
        ObjectMapper mapper = new ObjectMapper();
        REQUEST_WRITER = mapper.writerFor(DepositPipelineMessage.class);
        REQUEST_READER = mapper.readerFor(DepositPipelineMessage.class);
    }
    private JmsTemplate jmsTemplate;
    private String destinationName;

    public void sendDepositPipelineMessage(DepositPipelineMessage message) throws JsonProcessingException {
        String json = REQUEST_WRITER.writeValueAsString(message);
        jmsTemplate.convertAndSend(destinationName, json);
    }

    public DepositPipelineMessage fromJson(Message message) throws IOException, JMSException {
        return REQUEST_READER.readValue(message.getBody(String.class));
    }

    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }
}
