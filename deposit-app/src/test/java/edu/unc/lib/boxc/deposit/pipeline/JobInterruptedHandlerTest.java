package edu.unc.lib.boxc.deposit.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.unc.lib.boxc.deposit.api.DepositOperation;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.work.JobInterruptedException;

@ExtendWith(MockitoExtension.class)
public class JobInterruptedHandlerTest {

    private JobInterruptedHandler handler;

    @Mock
    private DepositStatusFactory depositStatusFactory;

    private DepositOperationMessage operationMessage;
    private final static String DEPOSIT_ID = "deposit123";
    private final static String JOB_ID = "job456";
    private final static String USERNAME = "testuser";

    @BeforeEach
    public void setup() {
        handler = new JobInterruptedHandler();
        handler.setDepositStatusFactory(depositStatusFactory);

        operationMessage = new DepositOperationMessage();
        operationMessage.setDepositId(DEPOSIT_ID);
        operationMessage.setJobId(JOB_ID);
        operationMessage.setAction(DepositOperation.JOB_INTERRUPTED);
        operationMessage.setUsername(USERNAME);
        operationMessage.setExceptionClassName(JobInterruptedException.class.getName());
    }

    @Test
    public void testJobInterruptedRunningDeposit() {
        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(DepositState.running);

        handler.handleMessage(operationMessage);

        verify(depositStatusFactory).setState(DEPOSIT_ID, DepositState.quieted);
        verify(depositStatusFactory).removeSupervisorLock(DEPOSIT_ID);
    }

    @Test
    public void testJobInterruptedNonRunningDeposit() {
        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(DepositState.paused);

        handler.handleMessage(operationMessage);

        verify(depositStatusFactory, never()).setState(DEPOSIT_ID, DepositState.quieted);
        verify(depositStatusFactory).removeSupervisorLock(DEPOSIT_ID);
    }

    @Test
    public void testGetDepositStatusFactory() {
        DepositStatusFactory result = handler.getDepositStatusFactory();

        assertEquals(depositStatusFactory, result);
    }
}