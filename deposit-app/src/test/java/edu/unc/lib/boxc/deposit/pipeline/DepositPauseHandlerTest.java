package edu.unc.lib.boxc.deposit.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unc.lib.boxc.deposit.api.DepositOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;

@ExtendWith(MockitoExtension.class)
public class DepositPauseHandlerTest {

    private DepositPauseHandler handler;

    @Mock
    private DepositStatusFactory depositStatusFactory;
    @Mock
    private ActiveDepositsService activeDeposits;

    private DepositOperationMessage operationMessage;
    private final static String DEPOSIT_ID = "deposit123";
    private final static String USERNAME = "testuser";

    @BeforeEach
    public void setup() {
        handler = new DepositPauseHandler();
        handler.setDepositStatusFactory(depositStatusFactory);
        handler.setActiveDeposits(activeDeposits);

        operationMessage = new DepositOperationMessage();
        operationMessage.setDepositId(DEPOSIT_ID);
        operationMessage.setUsername(USERNAME);
        operationMessage.setAction(DepositOperation.PAUSE);
    }

    @Test
    public void testSuccessfulPause() {
        when(depositStatusFactory.addSupervisorLock(DEPOSIT_ID, USERNAME)).thenReturn(true);

        handler.handleMessage(operationMessage);

        verify(depositStatusFactory).addSupervisorLock(DEPOSIT_ID, USERNAME);
        verify(depositStatusFactory).setState(DEPOSIT_ID, DepositState.paused);
        verify(activeDeposits).markInactive(DEPOSIT_ID);
        verify(depositStatusFactory).removeSupervisorLock(DEPOSIT_ID);
    }

    @Test
    public void testPauseFailsToAcquireLock() {
        when(depositStatusFactory.addSupervisorLock(DEPOSIT_ID, USERNAME)).thenReturn(false);

        handler.handleMessage(operationMessage);

        verify(depositStatusFactory).addSupervisorLock(DEPOSIT_ID, USERNAME);
        verify(depositStatusFactory, never()).setState(DEPOSIT_ID, DepositState.paused);
        verify(activeDeposits, never()).markInactive(DEPOSIT_ID);
        verify(depositStatusFactory, never()).removeSupervisorLock(DEPOSIT_ID);
    }

    @Test
    public void testGetDepositStatusFactory() {
        DepositStatusFactory result = handler.getDepositStatusFactory();

        assertEquals(depositStatusFactory, result);
    }
}