package edu.unc.lib.boxc.deposit.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import edu.unc.lib.boxc.deposit.api.DepositOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.unc.lib.boxc.deposit.CleanupDepositJob;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessageService;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;
import edu.unc.lib.boxc.deposit.jms.DepositCompleteService;
import edu.unc.lib.boxc.deposit.jms.DepositJobMessageFactory;

@ExtendWith(MockitoExtension.class)
public class JobSuccessHandlerTest {

    private JobSuccessHandler handler;

    @Mock
    private DepositJobMessageService depositJobMessageService;
    @Mock
    private DepositJobMessageFactory depositJobMessageFactory;
    @Mock
    private DepositStatusFactory depositStatusFactory;
    @Mock
    private DepositEmailHandler depositEmailHandler;
    @Mock
    private DepositCompleteService depositCompleteService;
    @Mock
    private JobStatusFactory jobStatusFactory;

    private DepositOperationMessage operationMessage;
    private DepositJobMessage nextJobMessage;
    private final String DEPOSIT_ID = "deposit123";
    private final String JOB_ID = "job456";
    private final String REGULAR_JOB_CLASS = "edu.unc.lib.boxc.deposit.SomeJob";
    private final int CLEANUP_DELAY_SECONDS = 30;

    @BeforeEach
    public void setup() {
        handler = new JobSuccessHandler();
        handler.setDepositJobMessageService(depositJobMessageService);
        handler.setDepositJobMessageFactory(depositJobMessageFactory);
        handler.setDepositStatusFactory(depositStatusFactory);
        handler.setDepositEmailHandler(depositEmailHandler);
        handler.setDepositCompleteService(depositCompleteService);
        handler.setJobStatusFactory(jobStatusFactory);
        handler.setCleanupDelaySeconds(CLEANUP_DELAY_SECONDS);

        operationMessage = new DepositOperationMessage();
        operationMessage.setDepositId(DEPOSIT_ID);
        operationMessage.setJobId(JOB_ID);
        operationMessage.setAction(DepositOperation.JOB_SUCCESS);

        nextJobMessage = new DepositJobMessage();
        nextJobMessage.setJobClassName(REGULAR_JOB_CLASS);
    }

    @Test
    public void testRegularJobSuccess() throws Exception {
        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.startTime.name(), "1234567890");

        when(depositStatusFactory.get(DEPOSIT_ID)).thenReturn(depositStatus);
        when(depositJobMessageFactory.createNextJobMessage(DEPOSIT_ID, depositStatus))
                .thenReturn(nextJobMessage);

        handler.handleMessage(operationMessage);

        verify(jobStatusFactory).completed(JOB_ID);
        verify(depositJobMessageService).sendDepositJobMessage(nextJobMessage);
        verify(depositStatusFactory, never()).setState(DEPOSIT_ID, DepositState.finished);
        verify(depositEmailHandler, never()).sendDepositResults(DEPOSIT_ID);
        verify(depositCompleteService, never()).sendDepositCompleteEvent(DEPOSIT_ID);
        verify(depositJobMessageService, never()).sendDepositJobMessage(any(), anyInt());
    }

    @Test
    public void testCleanupJobSuccess() throws Exception {
        nextJobMessage.setJobClassName(CleanupDepositJob.class.getName());

        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.startTime.name(), "1234567890");

        when(depositStatusFactory.get(DEPOSIT_ID)).thenReturn(depositStatus);
        when(depositJobMessageFactory.createNextJobMessage(DEPOSIT_ID, depositStatus))
                .thenReturn(nextJobMessage);

        handler.handleMessage(operationMessage);

        verify(jobStatusFactory).completed(JOB_ID);
        verify(depositStatusFactory).setState(DEPOSIT_ID, DepositState.finished);
        verify(depositStatusFactory).set(eq(DEPOSIT_ID), eq(DepositField.endTime), any());
        verify(depositEmailHandler).sendDepositResults(DEPOSIT_ID);
        verify(depositCompleteService).sendDepositCompleteEvent(DEPOSIT_ID);
        verify(depositJobMessageService).sendDepositJobMessage(nextJobMessage, CLEANUP_DELAY_SECONDS);
    }

    @Test
    public void testCleanupJobSuccessNoStartTime() throws Exception {
        nextJobMessage.setJobClassName(CleanupDepositJob.class.getName());

        // No start time in status
        Map<String, String> depositStatus = new HashMap<>();

        when(depositStatusFactory.get(DEPOSIT_ID)).thenReturn(depositStatus);
        when(depositJobMessageFactory.createNextJobMessage(DEPOSIT_ID, depositStatus))
                .thenReturn(nextJobMessage);

        handler.handleMessage(operationMessage);

        verify(jobStatusFactory).completed(JOB_ID);
        verify(depositStatusFactory).setState(DEPOSIT_ID, DepositState.finished);
        verify(depositStatusFactory, never()).set(eq(DEPOSIT_ID), eq(DepositField.endTime), any());
        verify(depositEmailHandler).sendDepositResults(DEPOSIT_ID);
        verify(depositCompleteService).sendDepositCompleteEvent(DEPOSIT_ID);
        verify(depositJobMessageService).sendDepositJobMessage(nextJobMessage, CLEANUP_DELAY_SECONDS);
    }

    @Test
    public void testGetDepositStatusFactory() {
        DepositStatusFactory result = handler.getDepositStatusFactory();

        assertEquals(depositStatusFactory, result);
    }
}