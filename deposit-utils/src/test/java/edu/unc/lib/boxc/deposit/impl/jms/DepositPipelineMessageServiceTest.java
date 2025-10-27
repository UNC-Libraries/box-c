package edu.unc.lib.boxc.deposit.impl.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.unc.lib.boxc.deposit.api.PipelineAction;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
public class DepositPipelineMessageServiceTest {

    private DepositPipelineMessageService service;

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private Message jmsMessage;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    private final static String DESTINATION_NAME = "depositPipelineQueue";

    @BeforeEach
    public void setup() {
        service = new DepositPipelineMessageService();
        service.setJmsTemplate(jmsTemplate);
        service.setDestinationName(DESTINATION_NAME);
    }

    @Test
    public void testSendDepositPipelineMessage() throws JsonProcessingException {
        DepositPipelineMessage message = new DepositPipelineMessage();
        message.setAction(PipelineAction.QUIET);
        message.setUsername("testuser");

        service.sendDepositPipelineMessage(message);

        verify(jmsTemplate).convertAndSend(eq(DESTINATION_NAME), messageCaptor.capture());
        String capturedMessage = messageCaptor.getValue();
        assertTrue(capturedMessage.contains("QUIET"));
        assertTrue(capturedMessage.contains("testuser"));
    }

    @Test
    public void testFromJson() throws IOException, JMSException {
        String jsonMessage = "{\"action\":\"QUIET\"," +
                "\"username\":\"testuser\"}";
        when(jmsMessage.getBody(String.class)).thenReturn(jsonMessage);

        DepositPipelineMessage result = service.fromJson(jmsMessage);

        assertEquals(PipelineAction.QUIET, result.getAction());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    public void testFromJsonWithMinimalData() throws IOException, JMSException {
        String jsonMessage = "{}";
        when(jmsMessage.getBody(String.class)).thenReturn(jsonMessage);

        DepositPipelineMessage result = service.fromJson(jmsMessage);

        assertNull(result.getAction());
        assertNull(result.getUsername());
    }

    @Test
    public void testFromJsonWithMalformedJson() throws JMSException {
        String malformedJson = "{\"depositId\":\"test-deposit-id\",";
        when(jmsMessage.getBody(String.class)).thenReturn(malformedJson);

        assertThrows(IOException.class, () -> {
            service.fromJson(jmsMessage);
        });
    }

    @Test
    public void testFromJsonWithJMSException() throws JMSException {
        when(jmsMessage.getBody(String.class)).thenThrow(new JMSException("JMS error"));

        assertThrows(JMSException.class, () -> {
            service.fromJson(jmsMessage);
        });
    }
}