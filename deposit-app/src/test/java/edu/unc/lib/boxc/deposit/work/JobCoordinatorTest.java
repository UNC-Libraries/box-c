package edu.unc.lib.boxc.deposit.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.unc.lib.boxc.deposit.api.DepositOperation;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessageService;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessageService;
import edu.unc.lib.boxc.deposit.impl.model.DepositPipelineStatusFactory;

import edu.unc.lib.boxc.deposit.normalize.Simple2N3BagJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;

import jakarta.jms.Message;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class JobCoordinatorTest {
    private JobCoordinator coordinator;

    @Mock
    private DepositJobMessageService depositJobMessageService;
    @Mock
    private DepositOperationMessageService depositOperationMessageService;
    @Mock
    private DepositPipelineStatusFactory depositPipelineStatusFactory;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private ActiveDepositsService activeDeposits;
    @Mock
    private Message message;
    @Mock
    private Runnable jobRunnable;

    private DepositJobMessage jobMessage;
    private final String JOB_ID = "job123";
    private final String DEPOSIT_ID = "deposit456";
    private final String JOB_CLASS_NAME = Simple2N3BagJob.class.getName();

    @BeforeEach
    public void setup() throws Exception {
        coordinator = new JobCoordinator();
        coordinator.setDepositJobMessageService(depositJobMessageService);
        coordinator.setDepositOperationMessageService(depositOperationMessageService);
        coordinator.setDepositPipelineStatusFactory(depositPipelineStatusFactory);
        coordinator.setApplicationContext(applicationContext);
        coordinator.setActiveDeposits(activeDeposits);

        // Setup job message
        jobMessage = new DepositJobMessage();
        jobMessage.setJobId(JOB_ID);
        jobMessage.setDepositId(DEPOSIT_ID);
        jobMessage.setJobClassName(JOB_CLASS_NAME);

        // Setup mocks
        when(depositPipelineStatusFactory.getPipelineState()).thenReturn(DepositPipelineState.active);
        when(depositJobMessageService.fromJson(message)).thenReturn(jobMessage);
        when(activeDeposits.isDepositActive(DEPOSIT_ID)).thenReturn(true);
        when(applicationContext.getBean(any(Class.class))).thenReturn(jobRunnable);
    }

    @Test
    public void testSuccessfulJobExecution() throws Exception {
        // When the job runs successfully
        coordinator.onMessage(message);

        // Then message should be acknowledged
        verify(message).acknowledge();

        // And job should be executed
        verify(jobRunnable).run();

        // And success message should be sent
        ArgumentCaptor<DepositOperationMessage> messageCaptor = ArgumentCaptor.forClass(DepositOperationMessage.class);
        verify(depositOperationMessageService).sendDepositOperationMessage(messageCaptor.capture());
        DepositOperationMessage successMessage = messageCaptor.getValue();
        assertEquals(DepositOperation.JOB_SUCCESS, successMessage.getAction());
        assertEquals(JOB_ID, successMessage.getJobId());
        assertEquals(DEPOSIT_ID, successMessage.getDepositId());
    }

    @Test
    public void testJobExecutionFailure() throws Exception {
        // Given a job that throws an exception
        RuntimeException jobException = new RuntimeException("Job failed");
        doThrow(jobException).when(jobRunnable).run();

        // When the job is executed
        coordinator.onMessage(message);

        // Then message should still be acknowledged
        verify(message).acknowledge();

        // And failure message should be sent
        ArgumentCaptor<DepositOperationMessage> messageCaptor = ArgumentCaptor.forClass(DepositOperationMessage.class);
        verify(depositOperationMessageService).sendDepositOperationMessage(messageCaptor.capture());
        DepositOperationMessage failureMessage = messageCaptor.getValue();
        assertEquals(DepositOperation.JOB_FAILURE, failureMessage.getAction());
        assertEquals(JOB_ID, failureMessage.getJobId());
        assertEquals(DEPOSIT_ID, failureMessage.getDepositId());
        assertEquals(RuntimeException.class.getName(), failureMessage.getExceptionClassName());
        assertEquals("Job failed", failureMessage.getExceptionMessage());
    }

    @Test
    public void testPipelineNotActive() throws Exception {
        // Given an inactive pipeline
        when(depositPipelineStatusFactory.getPipelineState()).thenReturn(DepositPipelineState.quieted);

        // When a message is received
        coordinator.onMessage(message);

        // Then no processing should occur
        verify(depositJobMessageService, never()).fromJson(any());
        verify(message, never()).acknowledge();
        verify(jobRunnable, never()).run();
    }

    @Test
    public void testDepositNotActive() throws Exception {
        // Given an inactive deposit
        when(activeDeposits.isDepositActive(DEPOSIT_ID)).thenReturn(false);

        // When a message is received
        coordinator.onMessage(message);

        // Then message should be acknowledged but job shouldn't run
        verify(message).acknowledge();
        verify(jobRunnable, never()).run();
    }

    @Test
    public void testMessageDeserializationFailure() throws Exception {
        // Given a message that can't be deserialized
        when(depositJobMessageService.fromJson(message)).thenThrow(new IOException("Parse error"));

        // When a message is received
        assertThrows(RuntimeException.class, () -> coordinator.onMessage(message));

        verify(message).acknowledge();
        // Then no job should run
        verify(jobRunnable, never()).run();
    }

    @Test
    public void testSendOperationMessageFailure() throws Exception {
        // Given a failure sending the operation message
        doThrow(new JsonProcessingException("Serialization error") {}).when(depositOperationMessageService)
                .sendDepositOperationMessage(any());

        // When a message is received
        assertThrows(RuntimeException.class, () -> coordinator.onMessage(message));

        // The job should still run
        verify(jobRunnable).run();

        // But the message shouldn't be acknowledged
        verify(message, never()).acknowledge();
    }
}