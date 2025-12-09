package edu.unc.lib.boxc.deposit.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;

@ExtendWith(MockitoExtension.class)
public class DepositResumeHandlerTest {

    private DepositResumeHandler handler;

    @Mock
    private DepositStatusFactory depositStatusFactory;
    @Mock
    private JobStatusFactory jobStatusFactory;

    private DepositOperationMessage operationMessage;
    private final String DEPOSIT_ID = "deposit123";
    private final String USERNAME = "testuser";

    @BeforeEach
    public void setup() {
        handler = new DepositResumeHandler();
        handler.setDepositStatusFactory(depositStatusFactory);
        handler.setJobStatusFactory(jobStatusFactory);

        operationMessage = new DepositOperationMessage();
        operationMessage.setDepositId(DEPOSIT_ID);
        operationMessage.setUsername(USERNAME);
        operationMessage.setAction(DepositOperation.RESUME);
    }

    @Test
    public void testSuccessfulResumeFromPausedState() {
        testSuccessfulResumeFromAcceptableState(DepositState.paused);
    }

    @Test
    public void testSuccessfulResumeFromQuietedState() {
        testSuccessfulResumeFromAcceptableState(DepositState.quieted);
    }

    @Test
    public void testSuccessfulResumeFromFailedState() {
        testSuccessfulResumeFromAcceptableState(DepositState.failed);
    }

    @Test
    public void testSuccessfulResumeFromUnregisteredState() {
        testSuccessfulResumeFromAcceptableState(DepositState.unregistered);
    }

    public void testSuccessfulResumeFromAcceptableState(DepositState state) {
        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.state.name(), state.name());
        when(depositStatusFactory.get(DEPOSIT_ID)).thenReturn(depositStatus);

        handler.handleMessage(operationMessage);

        verify(jobStatusFactory).clearStale(DEPOSIT_ID);
        verify(depositStatusFactory).deleteField(DEPOSIT_ID, DepositField.errorMessage);
        verify(depositStatusFactory).queueDeposit(DEPOSIT_ID);
    }

    @Test
    public void testResumeFromInvalidState() {
        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.state.name(), DepositState.running.name());
        when(depositStatusFactory.get(DEPOSIT_ID)).thenReturn(depositStatus);

        handler.handleMessage(operationMessage);

        verify(jobStatusFactory, never()).clearStale(DEPOSIT_ID);
        verify(depositStatusFactory, never()).deleteField(DEPOSIT_ID, DepositField.errorMessage);
        verify(depositStatusFactory, never()).queueDeposit(DEPOSIT_ID);
    }

    @Test
    public void testGetDepositStatusFactory() {
        DepositStatusFactory result = handler.getDepositStatusFactory();

        assertEquals(depositStatusFactory, result);
    }
}