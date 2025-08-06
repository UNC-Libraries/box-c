package edu.unc.lib.boxc.deposit.impl.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.unc.lib.boxc.deposit.api.DepositOperation;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants;
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
public class DepositOperationMessageServiceTest {

    private DepositOperationMessageService service;

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private Message jmsMessage;
    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @BeforeEach
    public void setup() {
        service = new DepositOperationMessageService();
        service.setJmsTemplate(jmsTemplate);
        service.setDestinationName("depositOperationsQueue");
    }

    @Test
    public void testSendDepositOperationMessage() throws JsonProcessingException {
        DepositOperationMessage message = new DepositOperationMessage();
        message.setAction(DepositOperation.REGISTER);
        message.setUsername("test-username");
        message.setDepositId("test-deposit-id");
        message.setJobId("test-job-id");
        message.setBody("Test body content");

        service.sendDepositOperationMessage(message);

        // Verify the JmsTemplate was called with the right parameters
        verify(jmsTemplate).convertAndSend(eq("depositOperationsQueue"), messageCaptor.capture());
        String capturedMessage = messageCaptor.getValue();
        assertTrue(capturedMessage.contains("test-username"));
        assertTrue(capturedMessage.contains("test-deposit-id"));
        assertTrue(capturedMessage.contains("REGISTER"));
        assertTrue(capturedMessage.contains("test-job-id"));
        assertTrue(capturedMessage.contains("Test body content"));
    }

    @Test
    public void testFromJson() throws IOException, JMSException {
        String jsonMessage = "{\"depositId\":\"test-deposit-id\"," +
                "\"action\":\"PAUSE\"," +
                "\"username\":\"test-user\"," +
                "\"jobId\":\"test-job-id\"," +
                "\"body\":\"Test body content\"}";
        when(jmsMessage.getBody(String.class)).thenReturn(jsonMessage);

        DepositOperationMessage result = service.fromJson(jmsMessage);

        assertEquals(DepositOperation.PAUSE, result.getAction());
        assertEquals("test-deposit-id", result.getDepositId());
        assertEquals("test-user", result.getUsername());
        assertEquals("test-job-id", result.getJobId());
        assertEquals("Test body content", result.getBody());
    }
}