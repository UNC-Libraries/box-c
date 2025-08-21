package edu.unc.lib.boxc.deposit.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import edu.unc.lib.boxc.deposit.api.DepositOperation;
import edu.unc.lib.boxc.deposit.work.DepositEmailHandler;
import edu.unc.lib.boxc.deposit.work.JobFailedException;
import edu.unc.lib.boxc.deposit.work.JobInterruptedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;

@ExtendWith(MockitoExtension.class)
public class JobFailureHandlerTest {

    private JobFailureHandler handler;

    @Mock
    private DepositStatusFactory depositStatusFactory;
    @Mock
    private ActiveDepositsService activeDeposits;
    @Mock
    private DepositEmailHandler depositEmailHandler;

    private DepositOperationMessage operationMessage;
    private final static String DEPOSIT_ID = "deposit123";
    private final static String JOB_ID = "job456";
    private final static String USERNAME = "testuser";
    private final static String EXCEPTION_MESSAGE = "Something went wrong";
    private final static String STACK_TRACE = "Stack trace here";

    @BeforeEach
    public void setup() {
        handler = new JobFailureHandler();
        handler.setDepositStatusFactory(depositStatusFactory);
        handler.setActiveDeposits(activeDeposits);
        handler.setDepositEmailHandler(depositEmailHandler);

        operationMessage = new DepositOperationMessage();
        operationMessage.setDepositId(DEPOSIT_ID);
        operationMessage.setJobId(JOB_ID);
        operationMessage.setAction(DepositOperation.JOB_FAILURE);
        operationMessage.setUsername(USERNAME);
        operationMessage.setExceptionMessage(EXCEPTION_MESSAGE);
        operationMessage.setExceptionStackTrace(STACK_TRACE);
    }

    @Test
    public void testJobFailedExceptionWithDuration() {
        operationMessage.setExceptionClassName(JobFailedException.class.getName());
        when(depositStatusFactory.addSupervisorLock(DEPOSIT_ID, USERNAME)).thenReturn(true);

        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.startTime.name(), "1234567890");
        when(depositStatusFactory.get(DEPOSIT_ID)).thenReturn(depositStatus);

        handler.handleMessage(operationMessage);

        verify(depositStatusFactory).addSupervisorLock(DEPOSIT_ID, USERNAME);
        verify(depositStatusFactory).fail(DEPOSIT_ID, EXCEPTION_MESSAGE);
        verify(depositStatusFactory).set(eq(DEPOSIT_ID), eq(DepositField.endTime), any());
        verify(depositEmailHandler).sendDepositResults(DEPOSIT_ID);
        verify(activeDeposits).markInactive(DEPOSIT_ID);
        verify(depositStatusFactory).removeSupervisorLock(DEPOSIT_ID);
    }

    @Test
    public void testJobFailedExceptionWithoutStartTime() {
        operationMessage.setExceptionClassName(JobFailedException.class.getName());
        when(depositStatusFactory.addSupervisorLock(DEPOSIT_ID, USERNAME)).thenReturn(true);

        Map<String, String> depositStatus = new HashMap<>();
        // No start time in status
        when(depositStatusFactory.get(DEPOSIT_ID)).thenReturn(depositStatus);

        handler.handleMessage(operationMessage);

        verify(depositStatusFactory).addSupervisorLock(DEPOSIT_ID, USERNAME);
        verify(depositStatusFactory).fail(DEPOSIT_ID, EXCEPTION_MESSAGE);
        verify(depositStatusFactory, never()).set(eq(DEPOSIT_ID), eq(DepositField.endTime), any());
        verify(depositEmailHandler).sendDepositResults(DEPOSIT_ID);
        verify(activeDeposits).markInactive(DEPOSIT_ID);
        verify(depositStatusFactory).removeSupervisorLock(DEPOSIT_ID);
    }

    @Test
    public void testOtherExceptionWithFullyQualifiedClassName() {
        operationMessage.setExceptionClassName("java.lang.IllegalArgumentException");
        when(depositStatusFactory.addSupervisorLock(DEPOSIT_ID, USERNAME)).thenReturn(true);

        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.startTime.name(), "1234567890");
        when(depositStatusFactory.get(DEPOSIT_ID)).thenReturn(depositStatus);

        handler.handleMessage(operationMessage);

        verify(depositStatusFactory).addSupervisorLock(DEPOSIT_ID, USERNAME);
        verify(depositStatusFactory).fail(DEPOSIT_ID, "Failed while performing service IllegalArgumentException");
        verify(depositStatusFactory).set(eq(DEPOSIT_ID), eq(DepositField.endTime), any());
        verify(depositEmailHandler).sendDepositResults(DEPOSIT_ID);
        verify(activeDeposits).markInactive(DEPOSIT_ID);
        verify(depositStatusFactory).removeSupervisorLock(DEPOSIT_ID);
    }

    @Test
    public void testFailsToAcquireLock() {
        operationMessage.setExceptionClassName(JobFailedException.class.getName());
        when(depositStatusFactory.addSupervisorLock(DEPOSIT_ID, USERNAME)).thenReturn(false);

        handler.handleMessage(operationMessage);

        verify(depositStatusFactory).addSupervisorLock(DEPOSIT_ID, USERNAME);
        verify(depositStatusFactory, never()).fail(eq(DEPOSIT_ID), any());
        verify(depositEmailHandler, never()).sendDepositResults(DEPOSIT_ID);
        verify(activeDeposits, never()).markInactive(DEPOSIT_ID);
        verify(depositStatusFactory, never()).removeSupervisorLock(DEPOSIT_ID);
    }

    @Test
    public void testGetDepositStatusFactory() {
        DepositStatusFactory result = handler.getDepositStatusFactory();

        assertEquals(depositStatusFactory, result);
    }
}