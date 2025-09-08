package edu.unc.lib.boxc.deposit.pipeline;

import edu.unc.lib.boxc.deposit.api.DepositOperation;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DepositQuietHandlerTest {
    private DepositQuietHandler handler;

    @Mock
    private DepositStatusFactory depositStatusFactory;

    private DepositOperationMessage operationMessage;
    private final static String DEPOSIT_ID = "deposit123";
    private final static String USERNAME = "testuser";

    @BeforeEach
    public void setup() {
        handler = new DepositQuietHandler();
        handler.setDepositStatusFactory(depositStatusFactory);

        operationMessage = new DepositOperationMessage();
        operationMessage.setDepositId(DEPOSIT_ID);
        operationMessage.setUsername(USERNAME);
        operationMessage.setAction(DepositOperation.QUIET);
    }

    @Test
    public void testSuccessfulPause() {
        when(depositStatusFactory.addSupervisorLock(DEPOSIT_ID, USERNAME)).thenReturn(true);

        handler.handleMessage(operationMessage);

        verify(depositStatusFactory).addSupervisorLock(DEPOSIT_ID, USERNAME);
        verify(depositStatusFactory).setState(DEPOSIT_ID, DepositState.quieted);
        verify(depositStatusFactory).removeSupervisorLock(DEPOSIT_ID);
    }

    @Test
    public void testPauseFailsToAcquireLock() {
        when(depositStatusFactory.addSupervisorLock(DEPOSIT_ID, USERNAME)).thenReturn(false);

        handler.handleMessage(operationMessage);

        verify(depositStatusFactory).addSupervisorLock(DEPOSIT_ID, USERNAME);
        verify(depositStatusFactory, never()).setState(DEPOSIT_ID, DepositState.quieted);
        verify(depositStatusFactory, never()).removeSupervisorLock(DEPOSIT_ID);
    }

    @Test
    public void testGetDepositStatusFactory() {
        DepositStatusFactory result = handler.getDepositStatusFactory();

        assertEquals(depositStatusFactory, result);
    }
}
