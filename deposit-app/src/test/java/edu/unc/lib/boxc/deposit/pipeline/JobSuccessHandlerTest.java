package edu.unc.lib.boxc.deposit.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unc.lib.boxc.deposit.CleanupDepositJob;
import edu.unc.lib.boxc.deposit.api.DepositOperation;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessageService;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;
import edu.unc.lib.boxc.deposit.jms.DepositCompleteService;
import edu.unc.lib.boxc.deposit.jms.DepositJobMessageFactory;
import edu.unc.lib.boxc.deposit.work.DepositEmailHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;

@MockitoSettings(strictness = Strictness.LENIENT)
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
        handler.setCleanupDelaySeconds(CLEANUP_DELAY_SECONDS);

        operationMessage = new DepositOperationMessage();
        operationMessage.setDepositId(DEPOSIT_ID);
        operationMessage.setJobId(JOB_ID);
        operationMessage.setAction(DepositOperation.JOB_SUCCESS);

        nextJobMessage = new DepositJobMessage();
        nextJobMessage.setJobClassName(REGULAR_JOB_CLASS);

        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(DepositState.running);
    }

    @Test
    public void testRegularJobSuccess() throws Exception {
        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.startTime.name(), "1234567890");

        when(depositStatusFactory.get(DEPOSIT_ID)).thenReturn(depositStatus);
        when(depositJobMessageFactory.createNextJobMessage(DEPOSIT_ID, depositStatus))
                .thenReturn(nextJobMessage);

        handler.handleMessage(operationMessage);

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

        verify(depositStatusFactory).setState(DEPOSIT_ID, DepositState.finished);
        verify(depositStatusFactory, never()).set(eq(DEPOSIT_ID), eq(DepositField.endTime), any());
        verify(depositEmailHandler).sendDepositResults(DEPOSIT_ID);
        verify(depositCompleteService).sendDepositCompleteEvent(DEPOSIT_ID);
        verify(depositJobMessageService).sendDepositJobMessage(nextJobMessage, CLEANUP_DELAY_SECONDS);
    }

    @Test
    public void testDepositNotRunning() throws Exception {
        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(DepositState.failed);

        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.startTime.name(), "1234567890");

        when(depositStatusFactory.get(DEPOSIT_ID)).thenReturn(depositStatus);
        when(depositJobMessageFactory.createNextJobMessage(DEPOSIT_ID, depositStatus))
                .thenReturn(nextJobMessage);

        handler.handleMessage(operationMessage);

        // No further jobs queued
        verify(depositJobMessageService, never()).sendDepositJobMessage(any(DepositJobMessage.class));
        verify(depositJobMessageService, never()).sendDepositJobMessage(any(DepositJobMessage.class), anyInt());
    }

    @Test
    public void testGetDepositStatusFactory() {
        DepositStatusFactory result = handler.getDepositStatusFactory();

        assertEquals(depositStatusFactory, result);
    }
}