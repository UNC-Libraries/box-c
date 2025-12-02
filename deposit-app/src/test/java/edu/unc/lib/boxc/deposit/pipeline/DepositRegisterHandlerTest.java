package edu.unc.lib.boxc.deposit.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unc.lib.boxc.deposit.api.DepositOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;

@ExtendWith(MockitoExtension.class)
public class DepositRegisterHandlerTest {

    private DepositRegisterHandler handler;

    @Mock
    private DepositStatusFactory depositStatusFactory;

    private DepositOperationMessage operationMessage;
    private final String DEPOSIT_ID = "deposit123";
    private final String USERNAME = "testuser";

    @BeforeEach
    public void setup() {
        handler = new DepositRegisterHandler();
        handler.setDepositStatusFactory(depositStatusFactory);

        operationMessage = new DepositOperationMessage();
        operationMessage.setDepositId(DEPOSIT_ID);
        operationMessage.setUsername(USERNAME);
        operationMessage.setAction(DepositOperation.REGISTER);
    }

    @Test
    public void testSuccessfulRegistration() {
        handler.handleMessage(operationMessage);

        verify(depositStatusFactory).queueDeposit(DEPOSIT_ID);
        verify(depositStatusFactory).removeSupervisorLock(DEPOSIT_ID);
    }

    @Test
    public void testGetDepositStatusFactory() {
        DepositStatusFactory result = handler.getDepositStatusFactory();

        assertEquals(depositStatusFactory, result);
    }
}