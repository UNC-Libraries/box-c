package edu.unc.lib.boxc.deposit.impl.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
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
public class DepositJobMessageServiceTest {

    private DepositJobMessageService service;

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private Message jmsMessage;
    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @BeforeEach
    public void setup() {
        service = new DepositJobMessageService();
        service.setJmsTemplate(jmsTemplate);
        service.setDestinationName("depositJobQueue");
    }

    @Test
    public void testSendDepositJobMessage() throws JsonProcessingException {
        DepositJobMessage message = new DepositJobMessage();
        message.setJobId("test-job-id");
        message.setJobClassName("path.to.test.JobClass");
        message.setDepositId("test-deposit-id");

        service.sendDepositJobMessage(message);

        // Verify the JmsTemplate was called with the right parameters
        verify(jmsTemplate).convertAndSend(eq("depositJobQueue"), messageCaptor.capture());
        String capturedMessage = messageCaptor.getValue();
        assertTrue(capturedMessage.contains("test-job-id"));
        assertTrue(capturedMessage.contains("test-deposit-id"));
        assertTrue(capturedMessage.contains("path.to.test.JobClass"));
    }

    @Test
    public void testFromJson() throws IOException, JMSException {
        String jsonMessage = "{\"jobId\":\"test-job-id\"," +
                "\"depositId\":\"test-deposit-id\"," +
                "\"jobClassName\":\"path.to.test.JobClass\"}";
        when(jmsMessage.getBody(String.class)).thenReturn(jsonMessage);

        DepositJobMessage result = service.fromJson(jmsMessage);

        assertEquals("test-job-id", result.getJobId());
        assertEquals("test-deposit-id", result.getDepositId());
        assertEquals("path.to.test.JobClass", result.getJobClassName());
    }
}